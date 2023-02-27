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

package com.deere.isg.worktracker;

import com.deere.clock.Clock;
import net.logstash.logback.argument.StructuredArgument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import java.util.List;
import java.util.regex.Pattern;

import static com.deere.isg.worktracker.MockTimeUtils.*;
import static com.deere.isg.worktracker.Work.*;
import static net.logstash.logback.argument.StructuredArguments.keyValue;
import static org.assertj.core.api.Assertions.assertThat;

public class WorkTest {
    private static final String MAIN_THREAD = "main";
    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[a-zA-Z0-9]{8}-([a-zA-Z0-9]{4}-){3}[a-zA-Z0-9]{12}$");

    private static final String SOME_KEY = "some_key";
    private static final String NON_SNAKE_CASE_KEY = "JohnDeere";
    private static final String UPPERCASE_KEY = "Upper_Case";
    private static final String RANDOM_CHAR_KEY = "$happy#";

    private static final String SOME_VALUE = "some_value";
    private static final String NON_TRIM_VALUE = "    This is a non trimmed text    ";
    private static final String TRIM_VALUE = "This is a non trimmed text";
    private static final String USER = "user";
    private static final String SERVICE = "service";

    private Work work;

    @Before
    public void setUp() {
        Clock.freeze();
        work = new MockWork();
    }

    @After
    public void tearDown() {
        Clock.clear();
        MDC.clear();
    }

    @Test
    public void elapsedMillisIs5minAfterFreeze() {
        freezeClock();

        assertThat(work.getElapsedMillis()).isEqualTo(MINUTE_5);
    }

    @Test
    public void workHasThreadNameInMdc() {
        assertThat(work.getThreadName()).isEqualTo(MAIN_THREAD);
        assertThat(MDC.get(THREAD_NAME)).isEqualTo(MAIN_THREAD);
    }

    @Test
    public void requestIdHasRandomUUID() {
        boolean match = UUID_PATTERN.matcher(work.getRequestId()).matches();

        assertThat(match).isTrue();
    }

    @Test
    public void currentThreadIsNamedMain() {
        assertThat(work.getThreadName()).isEqualTo(MAIN_THREAD);
    }

    @Test
    public void multipleWorkOnMultipleThreadsHaveDifferentThreadNames() throws InterruptedException {
        assertThat(work.getThreadName()).isEqualTo(MAIN_THREAD);

        HelperThread testThread = new HelperThread("testThread");
        testThread.start();

        HelperThread anotherThread = new HelperThread("anotherThread");
        anotherThread.start();

        testThread.join(1000);
        assertThat(testThread.isThreadNameEqual()).isTrue();
        anotherThread.join(1000);
        assertThat(anotherThread.isThreadNameEqual()).isTrue();

        assertThat(work.getThreadName()).isEqualTo(MAIN_THREAD);

    }

    @Test
    public void mdcContainsRequestId() {
        String actual = MDC.get(REQUEST_ID);
        boolean match = UUID_PATTERN.matcher(actual).matches();

        assertThat(match).isTrue();
    }

    @Test
    public void maxTimeIsOverriddenOnSetter() {
        assertThat(work.getMaxTime()).isEqualTo(MINUTE_5);

        work.setMaxTime(SECOND_10);
        assertThat(work.getMaxTime()).isEqualTo(SECOND_10);
    }

    @Test
    public void workIsNotZombieIfLessThanMaxTime() {
        freezeClockOffset(-1L);

        assertThat(work.isZombie()).isEqualTo(false);
    }

    @Test
    public void workIsNotZombieIfEqualToMaxTime() {
        freezeClock();

        assertThat(work.isZombie()).isEqualTo(false);
    }

    @Test
    public void workIsZombieIfMoreThanMaxTime() {
        freezeClockOffset(1L);

        assertThat(work.isZombie()).isTrue();
    }

    @Test
    public void killThemAll() throws InterruptedException {
        KillerThread thread = new KillerThread("killerThread");

        thread.start();
        thread.run();
        thread.join(1000);

        assertThat(thread.isFirstInterrupt()).isEqualTo(false);
        assertThat(thread.isSecondInterrupt()).isTrue();
    }

    @Test
    public void getMetadataReturnsArrayOfSArgs() {
        List<StructuredArgument> metadata = work.getMetadata();

        assertThat(metadata).hasSize(4);

        assertThat(metadata).contains(keyValue(REQUEST_ID, work.getRequestId()));
        assertThat(metadata).contains(keyValue(ELAPSED_MS, work.getElapsedMillis()));
        assertThat(metadata).contains(keyValue(THREAD_NAME, work.getThreadName()));
        assertThat(metadata).contains(keyValue(ZOMBIE, work.isZombie()));
    }

    @Test
    public void addInfoToMDC() {
        work.addToMDC(SOME_KEY, SOME_VALUE);

        assertThat(MDC.get(SOME_KEY)).isEqualTo(SOME_VALUE);
    }

    @Test
    public void mayOverrideValidation() {
        work = new MockWork() {
            @Override
            protected void validateKey(String key) {
                if(!(REQUEST_ID.equals(key) || THREAD_NAME.equals(key))) {
                    assertThat(key).isEqualTo("bad.key");
                }
            }
        };

        work.addToMDC("bad.key", SOME_VALUE);
        assertThat(MDC.get("bad.key")).isEqualTo(SOME_VALUE);
    }

    @Test(expected = TestableIllegalArgumentException.class)
    public void mayOverrideValidationAndValidateDifferently() {
        work = new MockWork() {
            @Override
            protected void validateKey(String key) {
                throw new TestableIllegalArgumentException(key);
            }
        };

        work.addToMDC("bad.key", SOME_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsErrorForNonSnakeCase() {
        work.addToMDC(NON_SNAKE_CASE_KEY, SOME_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsErrorForUpperCase() {
        work.addToMDC(UPPERCASE_KEY, SOME_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsErrorForRandomCharCase() {
        work.addToMDC(RANDOM_CHAR_KEY, SOME_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwErrorForBlankKey() {
        work.addToMDC("", SOME_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwErrorForNullKey() {
        work.addToMDC(null, SOME_VALUE);
    }

    @Test
    public void trimKeyWhenPutInMDC() {
        work.addToMDC("    a_bs   ", SOME_VALUE);

        assertThat(MDC.get("a_bs")).isEqualTo(SOME_VALUE);
    }

    @Test
    public void trimsValueWhenPutInMDC() {
        work.addToMDC(SOME_KEY, NON_TRIM_VALUE);

        assertThat(MDC.get(SOME_KEY)).isEqualTo(TRIM_VALUE);
    }

    @Test
    public void doesNotPutEmptyValuesInMDC() {
        work.addToMDC(SOME_KEY, "    ");

        assertThat(MDC.get(SOME_KEY)).isNull();
    }

    @Test
    public void putInMDCReturnsValue() {
        String value = work.addToMDC(SOME_KEY, SOME_VALUE);

        assertThat(value).isEqualTo(SOME_VALUE);
    }

    @Test
    public void getThreadInfoReturnsListOfSArgs() {
        final List<StructuredArgument> threadInfo = work.getThreadInfo();

        assertThat(threadInfo).contains(keyValue(ZOMBIE, false));
        assertThat(threadInfo).contains(keyValue(ELAPSED_MS, work.getElapsedMillis()));
        assertThat(threadInfo).hasSize(2);
    }

    @Test
    public void getStartInfoContainsStartSArg() {
        assertIntervalInfo(work.getStartInfo(), "start");
    }

    @Test
    public void getEndInfoContainsEndSArg() {
        assertIntervalInfo(work.getEndInfo(), "end");
    }

    @Test
    public void blankLimitReturnsFalse() {
        assertThat(work.checkLimit(null)).isEqualTo(false);
        assertThat(work.checkLimit("")).isEqualTo(false);
        assertThat(work.checkLimit("            ")).isEqualTo(false);
    }

    @Test
    public void limitIsAddedIfNotThere() {
        assertThat(work.checkLimit(USER)).isEqualTo(false);

        assertThat(work.checkLimit(USER)).isTrue();
    }

    @Test
    public void moreThanOneLimitCheck() {
        assertThat(work.checkLimit(USER)).isEqualTo(false);
        assertThat(work.checkLimit(SERVICE)).isEqualTo(false);

        assertThat(work.checkLimit(USER)).isTrue();
        assertThat(work.checkLimit(SERVICE)).isTrue();
    }

    @Test
    public void removesALimit() {
        assertThat(work.checkLimit(USER)).isEqualTo(false);
        assertThat(work.checkLimit(USER)).isTrue();
        assertThat(work.removeLimit(USER)).isTrue();
    }

    @Test
    public void hasExtensionPoints() {
        assertThat(work.getService()).isNull();
        assertThat(work.getExtraInfo()).isNull();
    }

    private void assertIntervalInfo(List<StructuredArgument> endInfo, String end) {
        assertThat(endInfo).contains(keyValue(ZOMBIE, false));
        assertThat(endInfo).contains(keyValue(ELAPSED_MS, work.getElapsedMillis()));
        assertThat(endInfo).contains(keyValue("time_interval", end));
    }

    private static class TestableIllegalArgumentException extends IllegalArgumentException {
        public TestableIllegalArgumentException(String key) {
            super(key);
        }
    }

    private class HelperThread extends Thread {
        private Work helperWork;

        HelperThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            this.helperWork = new MockWork();
        }

        boolean isThreadNameEqual() {
            return helperWork.getThreadName().equals(getName());
        }
    }

    private class KillerThread extends Thread {
        private boolean firstInterrupt = true, secondInterrupt = false;

        KillerThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            Work testWork = new MockWork();
            firstInterrupt = Thread.interrupted();

            testWork.kill();

            secondInterrupt = Thread.interrupted();
        }

        boolean isFirstInterrupt() {
            return firstInterrupt;
        }

        boolean isSecondInterrupt() {
            return secondInterrupt;
        }
    }
}
