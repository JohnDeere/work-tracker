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


package com.deere.example;

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.Work;

public final class MDC {
    private static OutstandingWork<?> outstandingWork;

    private MDC() {

    }

    static <W extends Work> OutstandingWork<W> init(OutstandingWork<W> ws) {
        outstandingWork = ws;
        return ws;
    }

    public static void put(String key, String value) {
        assert outstandingWork != null : "Call init to initialize outstandingWork";
        outstandingWork.putInContext(key, value);
    }
}
