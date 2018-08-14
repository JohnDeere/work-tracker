/**
 * Copyright 2018 Deere & Company
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

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static java.lang.String.valueOf;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public final class MockWorkUtils {

    public static List<MockWork> createMockWorkList(int count) {
        return IntStream.iterate(count, i -> i - 1)
                .mapToObj(i -> new MockWork(generateString(8)))
                .limit(count)
                .collect(toList());
    }

    public static List<MockWork> createSameUserMockWork(int count, String user) {
        List<MockWork> works = createMockWorkList(count);
        works.forEach(w -> w.setUser(user));
        return works;
    }

    public static String generateString(int count) {
        return IntStream.range(0, count)
                .map(i -> ThreadLocalRandom.current().nextInt(123 - 97) + 97)
                .mapToObj(randCharInt -> valueOf((char) randCharInt))
                .collect(joining());
    }
}
