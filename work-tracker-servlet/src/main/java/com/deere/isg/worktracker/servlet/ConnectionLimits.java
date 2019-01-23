/**
 * Copyright 2019 Deere & Company
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


package com.deere.isg.worktracker.servlet;

import com.deere.isg.worktracker.FloodSensor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.deere.isg.worktracker.StringUtils.isNotBlank;
import static java.util.stream.Collectors.toList;

/**
 * ConnectionLimits contains the threshold limits for your resources available.
 * FloodSensor uses this to determine if there are too many requests and provides DoS protection
 *
 * @param <W> The type of the work object
 */
public class ConnectionLimits<W extends HttpWork> {
    public static final String TOTAL = "total";
    public static final String SESSION = "session";
    public static final String USER = "user";
    public static final String SERVICE = "service";

    private static final String MESSAGE = "Request rejected to protect JVM from too many requests";
    private static final String MESSAGE_FROM = MESSAGE + " from same ";

    private static final int DEFAULT_MAX_RESOURCES = 60;

    private ConcurrentMap<String, Limit> connections;

    public ConnectionLimits() {
        this(DEFAULT_MAX_RESOURCES, true);
    }

    public ConnectionLimits(boolean defaultLimits) {
        this(DEFAULT_MAX_RESOURCES, defaultLimits);
    }

    /**
     * @param maxResourcesLimit maximum resources available
     * @param defaultLimits     adds total, session, user and service limits if true
     */
    public ConnectionLimits(int maxResourcesLimit, boolean defaultLimits) {
        connections = new ConcurrentHashMap<>();
        if (defaultLimits) {
            addConnectionLimit((int) (maxResourcesLimit * .9), TOTAL).test(x -> true);
            addConnectionLimit((int) (maxResourcesLimit * .4), SESSION).method(W::getSessionId);
            addConnectionLimit((int) (maxResourcesLimit * .5), USER).method(W::getRemoteUser);
            addConnectionLimit((int) (maxResourcesLimit * .6), SERVICE).method(W::getService);
        }
    }

    /**
     * Adds a connection limit to the map
     *
     * @param limit    limit in integer
     * @param typeName the type of the limit
     * @return {@link LimitBuilder} to add a function or a predicate for {@link FloodSensor} to check against
     */
    public LimitBuilder addConnectionLimit(int limit, String typeName) {
        return new LimitBuilder(limit, typeName);
    }

    public Limit getConnectionLimit(String key) {
        return connections.get(key);
    }

    /**
     * @return list of available limits with the lowest limits first
     */
    public List<Limit> getConnectionLimits() {
        return connections.entrySet().stream()
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparing(Limit::getLimit))
                .collect(toList());
    }

    public void updateLimit(int limit, String typeName) {
        if (connections.containsKey(typeName)) {
            Limit current = connections.get(typeName);
            current.setLimit(limit);
            connections.replace(typeName, current);
        }
    }

    public int getLimit(String typeName) {
        return connections.get(typeName).getLimit();
    }

    private void addConnectionLimit(Limit limit) {
        if (connections.containsKey(limit.getTypeName())) {
            connections.replace(limit.getTypeName(), limit);
        } else {
            this.connections.putIfAbsent(limit.getTypeName(), limit);
        }
    }

    public class Limit {
        private Function<W, String> function;
        private Predicate<W> predicate;
        private int limit;
        private String typeName;

        private Limit(int limit, String typeName, Function<W, String> function) {
            this.limit = limit;
            this.typeName = typeName;
            this.function = function;
            this.predicate = x -> true;
        }

        private Limit(int limit, String typeName, Predicate<W> predicate) {
            this.limit = limit;
            this.typeName = typeName;
            this.predicate = predicate;
        }

        public Function<W, String> getFunction() {
            return function;
        }

        public Predicate<W> getPredicate() {
            return predicate;
        }

        public int getLimit() {
            return limit;
        }

        private void setLimit(int limit) {
            this.limit = limit;
        }

        public String getTypeName() {
            return typeName;
        }

        public String getMessage() {
            return typeName.equalsIgnoreCase(TOTAL)
                    ? MESSAGE + " " + TOTAL
                    : MESSAGE_FROM + typeName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Limit that = (Limit) o;
            return limit == that.limit &&
                    Objects.equals(typeName, that.typeName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(limit, typeName);
        }

        @Override
        public String toString() {
            return "Limit{ limit=" + limit + ", typeName=" + typeName + "}";
        }
    }

    public class LimitBuilder {
        private final int limit;
        private final String typeName;

        private LimitBuilder(int limit, String typeName) {
            assert isNotBlank(typeName);
            this.limit = limit;
            this.typeName = typeName;
        }

        public void method(Function<W, String> function) {
            addConnectionLimit(new Limit(limit, typeName, function));
        }

        public void test(Predicate<W> predicate) {
            addConnectionLimit(new Limit(limit, typeName, predicate));
        }
    }
}
