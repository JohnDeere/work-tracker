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


package com.deere.isg.worktracker.servlet;

import com.deere.clock.Clock;

import java.util.concurrent.TimeUnit;

final class TestTimeUtils {

    public static final long MINUTE_5 = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

    public static void freezeClockOffset(long milliseconds) {
        freezeClock(MINUTE_5 + milliseconds);
    }

    public static void freezeClock() {
        freezeClock(MINUTE_5);
    }

    public static void freezeClock(long milliseconds) {
        Clock.freeze(Clock.milliseconds() + milliseconds);
    }
}
