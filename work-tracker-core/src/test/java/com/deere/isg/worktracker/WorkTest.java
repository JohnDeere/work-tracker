/**
 * Copyright 2021 Deere & Company
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
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

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

        assertThat(work.getElapsedMillis(), is(MINUTE_5));
    }

    @Test
    public void workHasThreadNameInMdc() {
        assertThat(work.getThreadName(), is(MAIN_THREAD));
        assertThat(MDC.get(THREAD_NAME), is(MAIN_THREAD));
    }

    @Test
    public void requestIdHasRandomUUID() {
        boolean match = UUID_PATTERN.matcher(work.getRequestId()).matches();

        assertThat(match, is(true));
    }

    @Test
    public void currentThreadIsNamedMain() {
        assertThat(work.getThreadName(), is(MAIN_THREAD));
    }

    @Test
    public void multipleWorkOnMultipleThreadsHaveDifferentThreadNames() throws InterruptedException {
        assertThat(work.getThreadName(), is(MAIN_THREAD));

        HelperThread testThread = new HelperThread("testThread");
        testThread.start();

        HelperThread anotherThread = new HelperThread("anotherThread");
        anotherThread.start();

        testThread.join(1000);
        assertThat(testThread.isThreadNameEqual(), is(true));
        anotherThread.join(1000);
        assertThat(anotherThread.isThreadNameEqual(), is(true));

        assertThat(work.getThreadName(), is(MAIN_THREAD));

    }

    @Test
    public void mdcContainsRequestId() {
        String actual = MDC.get(REQUEST_ID);
        boolean match = UUID_PATTERN.matcher(actual).matches();

        assertThat(match, is(true));
    }

    @Test
    public void maxTimeIsOverriddenOnSetter() {
        assertThat(work.getMaxTime(), is(MINUTE_5));

        work.setMaxTime(SECOND_10);
        assertThat(work.getMaxTime(), is(SECOND_10));
    }

    @Test
    public void workIsNotZombieIfLessThanMaxTime() {
        freezeClockOffset(-1L);

        assertThat(work.isZombie(), is(false));
    }

    @Test
    public void workIsNotZombieIfEqualToMaxTime() {
        freezeClock();

        assertThat(work.isZombie(), is(false));
    }

    @Test
    public void workIsZombieIfMoreThanMaxTime() {
        freezeClockOffset(1L);

        assertThat(work.isZombie(), is(true));
    }

    @Test
    public void killThemAll() throws InterruptedException {
        KillerThread thread = new KillerThread("killerThread");

        thread.start();
        thread.run();
        thread.join(1000);

        assertThat(thread.isFirstInterrupt(), is(false));
        assertThat(thread.isSecondInterrupt(), is(true));
    }

    @Test
    public void getMetadataReturnsArrayOfSArgs() {
        List<StructuredArgument> metadata = work.getMetadata();

        assertThat(metadata, hasSize(equalTo(4)));

        assertThat(metadata, hasItem(keyValue(REQUEST_ID, work.getRequestId())));
        assertThat(metadata, hasItem(keyValue(ELAPSED_MS, work.getElapsedMillis())));
        assertThat(metadata, hasItem(keyValue(THREAD_NAME, work.getThreadName())));
        assertThat(metadata, hasItem(keyValue(ZOMBIE, work.isZombie())));
    }

    @Test
    public void addInfoToMDC() {
        work.addToMDC(SOME_KEY, SOME_VALUE);

        assertThat(MDC.get(SOME_KEY), is(SOME_VALUE));
    }

    @Test
    public void mayOverrideValidation() {
        work = new MockWork() {
            @Override
            protected void validateKey(String key) {
                if(!(REQUEST_ID.equals(key) || THREAD_NAME.equals(key))) {
                    assertThat(key, is("bad.key"));
                }
            }
        };

        work.addToMDC("bad.key", SOME_VALUE);
        assertThat(MDC.get("bad.key"), is(SOME_VALUE));
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

        assertThat(MDC.get("a_bs"), is(SOME_VALUE));
    }

    @Test
    public void trimsValueWhenPutInMDC() {
        work.addToMDC(SOME_KEY, NON_TRIM_VALUE);

        assertThat(MDC.get(SOME_KEY), is(TRIM_VALUE));
    }

    @Test
    public void doesNotPutEmptyValuesInMDC() {
        work.addToMDC(SOME_KEY, "    ");

        assertThat(MDC.get(SOME_KEY), nullValue());
    }

    @Test
    public void putInMDCReturnsValue() {
        String value = work.addToMDC(SOME_KEY, SOME_VALUE);

        assertThat(value, is(SOME_VALUE));
    }

    @Test
    public void getThreadInfoReturnsListOfSArgs() {
        final List<StructuredArgument> threadInfo = work.getThreadInfo();

        assertThat(threadInfo, hasItem(keyValue(ZOMBIE, false)));
        assertThat(threadInfo, hasItem(keyValue(ELAPSED_MS, work.getElapsedMillis())));
        assertThat(threadInfo, hasSize(2));
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
        assertThat(work.checkLimit(null), is(false));
        assertThat(work.checkLimit(""), is(false));
        assertThat(work.checkLimit("            "), is(false));
    }

    @Test
    public void limitIsAddedIfNotThere() {
        assertThat(work.checkLimit(USER), is(false));

        assertThat(work.checkLimit(USER), is(true));
    }

    @Test
    public void moreThanOneLimitCheck() {
        assertThat(work.checkLimit(USER), is(false));
        assertThat(work.checkLimit(SERVICE), is(false));

        assertThat(work.checkLimit(USER), is(true));
        assertThat(work.checkLimit(SERVICE), is(true));
    }

    @Test
    public void removesALimit() {
        assertThat(work.checkLimit(USER), is(false));
        assertThat(work.checkLimit(USER), is(true));
        assertThat(work.removeLimit(USER), is(true));
    }

    @Test
    public void hasExtensionPoints() {
        assertThat(work.getService(), nullValue(String.class));
        assertThat(work.getExtraInfo(), nullValue(String.class));
    }

    private void assertIntervalInfo(List<StructuredArgument> endInfo, String end) {
        assertThat(endInfo, hasItem(keyValue(ZOMBIE, false)));
        assertThat(endInfo, hasItem(keyValue(ELAPSED_MS, work.getElapsedMillis())));
        assertThat(endInfo, hasItem(keyValue("time_interval", end)));
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
