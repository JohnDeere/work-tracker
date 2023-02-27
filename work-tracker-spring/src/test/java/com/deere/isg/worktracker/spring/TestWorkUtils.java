/**
 * Copyright 2018-2023 Deere & Company
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

package com.deere.isg.worktracker.spring;

import com.deere.isg.worktracker.Work;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

final class TestWorkUtils {
    static final int SIZE = 10;

    private static final String TEST_USER = "some_user";
    private static final String TEST_SERVICE = "GET /hello";
    private static final String TEST_SESSION = "test_session";

    static List<Work> getWorkList(int count) {
        return IntStream.iterate(count, i -> i - 1)
                .mapToObj(i -> createWork())
                .limit(count)
                .collect(toList());
    }

    static SpringWork createWork() {
        SpringWork work = new SpringWork(null);
        work.setRemoteUser(TEST_USER);
        work.setService(TEST_SERVICE);
        work.setSessionId(TEST_SESSION);
        return work;
    }
}
