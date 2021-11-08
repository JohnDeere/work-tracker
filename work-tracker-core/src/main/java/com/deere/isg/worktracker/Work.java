/**
 * Copyright 2018-2021 Deere & Company
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

import com.deere.clock.Clock;
import net.logstash.logback.argument.StructuredArgument;
import net.logstash.logback.composite.loggingevent.ThreadNameJsonProvider;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.deere.isg.worktracker.StringUtils.*;
import static java.util.stream.Collectors.toList;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

public abstract class Work {
    public static final String THREAD_NAME = ThreadNameJsonProvider.FIELD_THREAD_NAME;
    public static final String ELAPSED_MS = "elapsed_ms";
    public static final String REQUEST_ID = "request_id";
    public static final String TIME_INTERVAL = "time_interval";
    public static final String ZOMBIE = "zombie";
    public static final String REQUEST_URL = "request_url";
    private static final long DEFAULT_MAX_TIME = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

    private long maxTime = DEFAULT_MAX_TIME;
    private long startTime = Clock.milliseconds();
    private Map<String, String> mdc;
    private final String requestId;
    private Thread thread = Thread.currentThread();
    private String threadName = addToMDC(THREAD_NAME, thread.getName());
    private Set<String> checkedLimits = new HashSet<>();

    protected Work() {
        this(null);
    }

    protected Work(String requestId) {
        this.requestId = addToMDC(REQUEST_ID, requestId != null ? requestId : generatedRequestId());
    }

    public long getElapsedMillis() {
        return Clock.milliseconds() - startTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getThreadName() {
        return threadName;
    }

    public long getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(long maxTime) {
        this.maxTime = maxTime;
    }

    /**
     * Extension point for subclasses to show what kind of work this is.
     * @return null unless overridden
     */
    public String getService() {
        return null;
    };

    /**
     * Extension point for subclasses to show additional information in /outstanding
     * @return null unless overridden
     */
    public String getExtraInfo() {
        return null;
    }

    public boolean isZombie() {
        return getElapsedMillis() > maxTime;
    }

    public void kill() {
        thread.interrupt();
    }

    /**
     * Adds metadata to every subsequent log message created on this thread (i.e. stored in the {@link MDC}),
     * and also makes sure that metadata is available to loggers that are running
     * outside the current thread and are logging about this work, see {@code ZombieDetector}.
     *
     * @param key   must be in snake_case because that is most efficiently managed in Elasticsearch.
     * @param value which must be a String because of historical limitations of SLF4J.
     *              If the value is null or empty it will not be stored as metadata.
     * @return The same value passed in for your coding convenience.
     * @throws IllegalArgumentException if the key is null or not in snake_case.
     */
    public String addToMDC(String key, String value) {
        validateKey(key);

        if (value != null) {
            String trimKey = key.trim();
            String trimValue = value.trim();
            if (isNotEmpty(trimValue)) {
                MDC.put(trimKey, trimValue);

                if (mdc != null) {
                    mdc.put(trimKey, trimValue);
                } else {
                    mdc = MDC.getCopyOfContextMap();
                }
            }
        }
        return value;
    }

    protected void validateKey(String key) throws IllegalArgumentException {
        if (isNotSnakeCase(key)) {
            throw new IllegalArgumentException("Key "+key+" should be in snake_case and cannot be null");
        }
    }

    /**
     * Metadata for start log. Required when you log the start of a request.
     * Usually used when the lifecycle of a Work has just started
     *
     * @return Metadata for start log
     */
    public List<StructuredArgument> getStartInfo() {
        return getIntervalInfo("start");
    }

    /**
     * Metadata for end log. Required when you log the end of a request.
     * Usually used when the lifecycle of a Work has ended
     *
     * @return Metadata for end log
     */
    public List<StructuredArgument> getEndInfo() {
        return getIntervalInfo("end");
    }

    public boolean checkLimit(String limit) {
        if (isBlank(limit)) {
            return false;
        }

        boolean hasLimit = checkedLimits.contains(limit);
        if (!hasLimit) {
            checkedLimits.add(limit);
        }
        return hasLimit;
    }

    public Set<String> getLimits() {
        return new HashSet<>(checkedLimits);
    }

    public boolean removeLimit(String limit) {
        return checkedLimits.remove(limit);
    }

    public final List<StructuredArgument> getMetadata() {
        List<StructuredArgument> metadata = mdc.entrySet().stream()
                .map(entry -> keyValue(entry.getKey(), entry.getValue()))
                .collect(toList());
        metadata.addAll(getThreadInfo());

        return new ArrayList<>(metadata);
    }

    public final List<StructuredArgument> getThreadInfo() {
        List<StructuredArgument> args = Arrays.asList(
                keyValue(ELAPSED_MS, getElapsedMillis()),
                keyValue(ZOMBIE, isZombie())
        );
        return new ArrayList<>(args);
    }

    protected String generatedRequestId() {
        return UUID.randomUUID().toString();
    }

    private List<StructuredArgument> getIntervalInfo(String interval) {
        List<StructuredArgument> intervalInfo = getThreadInfo();
        intervalInfo.add(keyValue(TIME_INTERVAL, interval));
        return intervalInfo;
    }
}
