package com.deere.isg.worktracker;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class DefaultMetricEngine<W extends Work> implements MetricEngine<W> {
    private Duration duration;
    private final Consumer<Bucket> output;
    private final BiConsumer<MetricSet, W> collector;

    private volatile MyBucket bucket;

    private DefaultMetricEngine(Duration duration, Consumer<Bucket> output, BiConsumer<MetricSet, W> collector) {
        this.duration = duration;

        this.output = output;
        this.collector = collector;
        bucket = new MyBucket(this.duration);
    }

    public static class Builder<W extends Work> {
        private Consumer<Bucket> output;
        private BiConsumer<MetricSet, W> collector;
        private Duration duration;

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

        public MetricEngine<W> build() {
            return new DefaultMetricEngine<>(duration, output, collector);
        }
    }

    @Override
    public void postProcess(W work) {

        if(bucket.exceedsDuration()) {
            Bucket toWrite = bucket;
            bucket = new MyBucket(duration);
            output.accept(toWrite);
        }

        collector.accept(bucket, work);

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
            set.getMetric("count", CountMetric.class).increment();
            return set;
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
            this.startTime = new Date().toInstant();
        }

        boolean exceedsDuration() {
            return getEndTime().isAfter(getStartTime());
        }

        public Instant getEndTime() {
            return getStartTime().plus(duration);
        }

        public Instant getStartTime() {
            return startTime;
        }
    }
}
