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

import com.deere.isg.outstanding.Outstanding;
import net.logstash.logback.argument.StructuredArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface OutstandingWorkTracker<W extends Work> extends Iterable<W> {
    Stream<W> stream();
    Optional<W> current();
    Outstanding<W>.Ticket create(W payload);
    void doInTransaction(W payload, Runnable transaction);

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

    default <E extends Throwable, E2 extends Throwable> void doInTransactionChecked(W payload, CheckedRunnable<E, E2> runnable) throws E, E2 {
        try (Outstanding<W>.Ticket ignored = this.create(payload)) {
            runnable.run();
        }
    }

    default <T extends W> OutstandingWorkTracker<T> filterTo(Class<T> clazz) {
        return new OutstandingWorkFilter<>(this, clazz);
    }
}
