package com.deere.isg.worktracker;

import net.logstash.logback.argument.StructuredArgument;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface OutstandingWorkTracker<W extends Work> {
    Stream<W> stream();

    String putInContext(String key, String value);

    List<StructuredArgument> getCurrentMetadata();

    Optional<W> current();
}
