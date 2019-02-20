package com.deere.isg.worktracker;

import org.joda.time.Instant;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface MetricEngine<W extends Work> extends PostProcessor<W> {

    interface Metric {
        String getKey();
        Object getValue();
    }

    class Tag implements Metric {
        private final String key;
        private final Object value;

        public Tag(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }
        public Object getValue() {
            return value;
        }
    }

    interface MetricCollection {
        Collection<Metric> getMetrics();
    }

    interface MetricSet extends MetricCollection {
        <M extends Metric> M getMetric(String key, Class<M> clazz);
        <M extends Metric> M getMetric(String key, Class<M> clazz, Consumer<M> setup);

        <M extends Metric> Optional<M> findMetric(String key, Class<M> clazz);
        MetricSet getMetricSet(Tag... tags);

        default MetricSet getMetricSet(List<Tag> tags) {
            return getMetricSet(tags.toArray(new Tag[0]));
        }
    }

    interface NumberMetric extends Metric {
        Number getValue();
    }

    interface Bucket extends MetricSet {
        Instant getStartTime();
        org.joda.time.Instant getEndTime();
    }

    abstract class BaseMetric implements Metric {
        private String key;

        BaseMetric(String key) {
            this.key = key;
        }

        @Override
        public String getKey() {
            return key;
        }
    }

    class CountMetric extends BaseMetric implements NumberMetric {
        private AtomicInteger count = new AtomicInteger();

        public CountMetric(String key) {
            super(key);
        }

        public void increment() {
            count.incrementAndGet();
        }

        public Integer getValue() {
            return count.get();
        }
    }

    class PercentageMetric extends BaseMetric implements NumberMetric {
        private CountMetric count;
        private CountMetric parentCount;

        public PercentageMetric(String key) {
            super(key);
        }

        public Double getValue() {
            return (double)count.getValue() / parentCount.getValue() * 100;
        }

        public void with(CountMetric count, CountMetric parentCount) {
            this.count = count;
            this.parentCount = parentCount;
        }
    }

    class LongMetric extends BaseMetric implements MetricCollection {
        LongExtendedStatistics longSummaryStatistics = new LongExtendedStatistics();

        public LongMetric(String key) {
            super(key);
        }

        void init(long missed) {
            for (long i = 0; i < missed; i++) {
                add(0);
            }
        }

        public void add(long value) {
            longSummaryStatistics.accept(value);
        }

        @Override
        public Object getValue() {
            return longSummaryStatistics;
        }

        @Override
        public Collection<Metric> getMetrics() {
            return Stream.of(
                    new NumberMetricReport("sum", longSummaryStatistics.getSum()),
                    new NumberMetricReport("average", longSummaryStatistics.getAverage()),
                    new NumberMetricReport("max", longSummaryStatistics.getMax()),
                    new NumberMetricReport("min", longSummaryStatistics.getMin()),
                    new NumberMetricReport("std_dev", longSummaryStatistics.getStandardDeviation())
            ).collect(Collectors.toList());
        }

        long getHits() {
            return longSummaryStatistics.getCount();
        }
    }

    class NumberMetricReport extends BaseMetric implements NumberMetric {
        private final Number value;

        public NumberMetricReport(String key, Number value) {
            super(key);
            this.value = value;
        }

        @Override
        public Number getValue() {
            return value;
        }
    }

    class UniqueMetric extends BaseMetric implements NumberMetric {
        private Set hash = ConcurrentHashMap.newKeySet();

        public UniqueMetric(String key) {
            super(key);
        }

        public void add(Object value) {
            hash.add(value);
        }

        public Number getValue() {
            return hash.size();
        }
    }

    interface MetricList extends Metric {
        Stream<MetricSet> stream();
    }
}
