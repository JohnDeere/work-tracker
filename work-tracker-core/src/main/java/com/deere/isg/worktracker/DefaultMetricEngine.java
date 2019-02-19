package com.deere.isg.worktracker;

import com.deere.clock.Clock;
import com.deere.isg.outstanding.Outstanding;
import org.joda.time.Duration;
import org.joda.time.Instant;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class DefaultMetricEngine<W extends Work> implements MetricEngine<W> {
    private Duration duration;
    private final Consumer<Bucket> output;
    private final BiConsumer<MetricSet, W> collector;
    private List<TriConsumer<ScheduledExecutorService, Supplier<MetricSet>, Outstanding<W>>> outstandings;

    private volatile MyBucket bucket;

    private DefaultMetricEngine(Duration duration, Consumer<Bucket> output,
            BiConsumer<MetricSet, W> collector,
            List<TriConsumer<ScheduledExecutorService, Supplier<MetricSet>, Outstanding<W>>> outstandings)
    {
        this.duration = duration;
        this.output = output;
        this.collector = collector;
        this.outstandings = outstandings;
        bucket = new MyBucket(this.duration);
    }

    public static class Builder<W extends Work> {
        // TODO: outputs, collectors, and such should be in lists
        private Consumer<Bucket> output;
        private BiConsumer<MetricSet, W> collector;
        private Duration duration;
        private BiConsumer<MetricSet, Outstanding<W>> outstandingConsumer;
        private List<TriConsumer<ScheduledExecutorService, Supplier<MetricSet>, Outstanding<W>>> outstandings
                = new ArrayList<>();

        public Builder(Duration duration) {
            this.duration = duration;
        }

        public Builder<W> output(Consumer<Bucket> output) {
            this.output = output;
            return this;
        }

        public Builder<W> collector(BiConsumer<MetricSet, W> collector) {
            this.collector = collector;
            return this;
        }

        public Builder<W> outstanding(BiConsumer<MetricSet, Outstanding<W>> sampler, Duration rate) {
            outstandings.add((executor, b, outstanding)->{
                executor.scheduleAtFixedRate(()->{
                    sampler.accept(b.get(), outstanding);
                }, 0, rate.getMillis(), TimeUnit.MILLISECONDS);
            });

            return this;
        }
        public MetricEngine<W> build() {
            return new DefaultMetricEngine<>(duration, output, collector, outstandings);
        }
    }

    interface TriConsumer<A,B,C> {
        void accept(A a, B b, C c);
    }

    @Override
    public void init(Outstanding<W> outstanding) {
        if(!outstandings.isEmpty()) {
            ScheduledExecutorService service = Executors.newScheduledThreadPool(outstandings.size());
            outstandings.forEach(c->c.accept(service, this::getBucket, outstanding));
        }
    }

    @Override
    public void postProcess(W work) {
        Bucket bucket = getBucket();

        collector.accept(bucket, work);
    }

    private Bucket getBucket() {
        // TODO: It should be a background thread that does bucket replacement
        if(bucket.exceedsDuration()) {
            Bucket toWrite = bucket;
            bucket = new MyBucket(duration);
            output.accept(toWrite);
        }

        return this.bucket;
    }

    private static class MyMetricSet implements MetricSet {
        private Map<String, Metric> metrics = new ConcurrentHashMap<>();
        private Tag[] tags;

        public MyMetricSet(Tag... tags) {
            this.tags = tags;
            if(tags != null) {
                for (Tag tag : tags) {
                    metrics.put(tag.getKey(), tag);
                }
            }
        }

        @Override
        public <M extends Metric> M getMetric(String key, Class<M> clazz) {
            return (M)metrics.computeIfAbsent(key, k -> createMetric(key, clazz));
        }

        @Override
        public <M extends Metric> M getMetric(String key, Class<M> clazz, Consumer<M> setup) {
            return (M)metrics.computeIfAbsent(key, k -> {
                M metric = createMetric(key, clazz);
                if(setup != null) {
                    setup.accept(metric);
                }
                return metric;
            });
        }

        private <M extends Metric> M createMetric(String key, Class<M> clazz) {
            try {
                return clazz.getConstructor(String.class).newInstance(key);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public MetricSet getMetricSet(String key, Object value) {
            MetricSet set = ((MyMetricList) metrics.computeIfAbsent(key, MyMetricList::new)).add(value);
            CountMetric countMetric = incrementCount(set);
            set.getMetric("percent", PercentageMetric.class,
                    (m)->m.with(countMetric, this.getMetric("count", CountMetric.class)));
            return set;
        }

        private CountMetric incrementCount(MetricSet set) {
            CountMetric countMetric = set.getMetric("count", CountMetric.class);
            countMetric.increment();
            return countMetric;
        }

        @Override
        public Collection<Metric> getMetrics() {
            return metrics.values();
        }
    }

    private static class MyMetricList implements MetricList {
        private String key;
        private Map<Object, MetricSet> metrics = new ConcurrentHashMap<>();

        MyMetricList(String key) {
            this.key = key;
        }

        public MetricSet add(Object value) {
            return metrics.computeIfAbsent(value, v -> new MyMetricSet(new Tag(key, v)));
        }

        @Override
        public Stream<MetricSet> stream() {
            return metrics.values().stream();
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Object getValue() {
            return metrics.values();
        }
    }

    private static class MyBucket extends MyMetricSet implements Bucket {

        private final Instant startTime;
        private Duration duration;

        MyBucket(Duration duration) {
            super();
            this.duration = duration;
            // actually the start times should be on minute and half minute boundaries
            this.startTime = Clock.now().toInstant();
        }

        boolean exceedsDuration() {
            return getEndTime().isBefore(Clock.now().toInstant());
        }

        public Instant getEndTime() {
            return getStartTime().plus(duration);
        }

        public Instant getStartTime() {
            return startTime;
        }
    }
}