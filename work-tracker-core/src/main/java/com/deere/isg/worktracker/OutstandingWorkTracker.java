package com.deere.isg.worktracker;

import net.logstash.logback.argument.StructuredArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface OutstandingWorkTracker<W extends Work> {
    Stream<W> stream();
    Optional<W> current();

    default List<StructuredArgument> getCurrentMetadata() {
        return current().map(Work::getMetadata).orElse(new ArrayList<>());
    }

    /**
     * Adds metadata to every subsequent log message created on the current thread (i.e. stored in the {@link org.slf4j.MDC}),
     * and also makes sure that metadata is available to loggers that are running
     * outside the current thread, such as the {@code ZombieDetector}.
     * <p>
     * This method does nothing if there is no current work in progress
     *
     * @param key   must be in snake_case because that is most efficiently managed in Elasticsearch.
     * @param value which must be a String because of historical limitations of SLF4J.
     *              If the value is {@code null} or empty it will not be stored as metadata.
     * @return The same value passed in for your coding convenience.
     * @throws IllegalArgumentException if the key is {@code null} or not in snake_case.
     */
    default String putInContext(String key, String value) {
        current().ifPresent(work -> work.addToMDC(key, value));
        return value;
    }
}
