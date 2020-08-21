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


package com.deere.isg.worktracker.servlet;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static java.lang.String.valueOf;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public final class TestWorkUtils {
    public static List<HttpWork> createSameConditionWorkList(int count, String condition, String value) {
        List<HttpWork> workList = createWorkList(count);

        if (condition != null) {
            switch (condition) {
                case "user":
                    workList.forEach(work -> work.setRemoteUser(value));
                    break;
                case "session":
                    workList.forEach(work -> work.setSessionId(value));
                    break;
                case "service":
                    workList.forEach(work -> work.setService(value));
                    break;
            }
        }

        return workList;
    }

    public static List<HttpWork> createAllConditionsList(int count, String service, String session, String user) {
        return createWorkList(count).stream()
                .peek(createWork(service, session, user))
                .collect(toList());
    }

    public static List<HttpWork> createWorkList(int count) {
        return IntStream.iterate(count, i -> i - 1)
                .mapToObj(i -> createWork())
                .limit(count)
                .collect(toList());
    }

    public static HttpWork createWork() {
        HttpWork work = new HttpWork(null);
        work.setRemoteUser(generateString(8));
        work.setService(generateString(4) + "/" + generateString(4));
        work.setRemoteAddress("127.0.0.1");
        return work;
    }

    private static String generateString(int count) {
        return IntStream.range(0, count)
                .map(i -> ThreadLocalRandom.current().nextInt(123 - 97) + 97)
                .mapToObj(randCharInt -> valueOf((char) randCharInt))
                .collect(joining());
    }

    private static Consumer<HttpWork> createWork(String service, String session, String user) {
        return work -> {
            work.setRemoteUser(generateString(user));
            work.setService(generateString(service));
            work.setSessionId(generateString(session));
        };
    }

    private static String generateString(String defaultValue) {
        return defaultValue == null ? generateString(8) : defaultValue;
    }
}
