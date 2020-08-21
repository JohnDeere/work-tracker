/**
 * Copyright 2020 Deere & Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.deere.isg.worktracker;

import net.logstash.logback.argument.StructuredArgument;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.deere.isg.worktracker.StringUtils.isNotBlank;

/**
 * {@link FloodSensor} helps the JVM stay healthy by detecting requests that exceeds
 * a certain limit. In other words, it protects from Denial of Service attacks.
 * Limits can be anything that can be checked against the Work object.
 *
 * @param <W> The type passed should extend {@link Work}
 */
public abstract class FloodSensor<W extends Work> {
    private final OutstandingWorkTracker<W> outstanding;
    private static Logger logger = LoggerFactory.getLogger(FloodSensor.class);

    public FloodSensor(OutstandingWorkTracker<W> outstanding) {
        this.outstanding = outstanding;
    }

    private <T, R> Predicate<T> compose(Function<T, R> getter, Predicate<R> test) {
        return b -> test.test(getter.apply(b));
    }

    protected final OutstandingWorkTracker<W> getOutstanding() {
        return outstanding;
    }

    protected Optional<Integer> shouldRetryLater(W incoming, Predicate<W> predicate, int limit, String typeName, String message) {
        boolean notCheckedYet = !incoming.checkLimit(typeName);
        if (exceeds(limit, predicate) && notCheckedYet) {
            return likeThingsStream(predicate)
                    .findFirst()
                    .map(oldestSimilar -> getRetryAfter(oldestSimilar, incoming.getMaxTime(), typeName, message));
        }
        return Optional.empty();
    }

    protected Optional<Integer> shouldRetryLater(W incoming, Function<W, String> getter, int limit, String typeName, String message) {
        return Optional.ofNullable(incoming)
                .map(getter)
                .flatMap(attribute -> isNotBlank(attribute)
                        ? shouldRetryLater(incoming, compose(getter, attribute::equals), limit, typeName, message)
                        : Optional.empty());
    }

    /**
     * An abstract method to provide the implementation for how to check the limits.
     * Provide a stream of limit checks that returns {@code Optional<Integer>}
     *
     * @return a stream of {@code Optional<Integer>} limit checks
     */
    protected abstract Stream<Function<W, Optional<Integer>>> checkLimits();

    private Stream<W> likeThingsStream(Predicate<W> predicate) {
        return outstanding.stream().filter(predicate);
    }

    private boolean exceeds(int limit, Predicate<W> predicate) {
        return likeThingsStream(predicate).limit(limit + 1).count() > limit;
    }

    private int getRetryAfter(W oldestSimilar, long maxTime, String typeName, String message) {
        long timeInMillis = oldestSimilar.getElapsedMillis();
        int retryAfterSeconds = calculateRetryAfterFromMillis(maxTime, timeInMillis);
        logFloodDetected(typeName, message, retryAfterSeconds);
        return retryAfterSeconds;
    }

    private int calculateRetryAfterFromMillis(long maxTime, long timeInMillis) {
        long minTime = Math.min(maxTime, timeInMillis);
        return (int) Math.ceil(minTime / 1000.0);
    }

    protected void logFloodDetected(String typeName, String message, int retryAfterSeconds) {
        outstanding.putInContext("limit_type", typeName);
        List<StructuredArgument> metadata = outstanding.getCurrentMetadata();
        metadata.add(StructuredArguments.keyValue("retry_after_seconds", retryAfterSeconds));
        logger.warn(message, metadata.toArray());
    }

    protected void setLogger(Logger logger) {
        this.logger = logger;
    }
}
