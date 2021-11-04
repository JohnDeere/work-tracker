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


package com.deere.isg.worktracker.servlet;

import com.deere.clock.Clock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class MdcExecutorTest {
    private static final String PARENT = "parent";
    private static final String TEST = "test";
    private static final String TASK_CLASS_NAME = "task_class_name";

    private ExecutorService executorService;
    private MdcExecutor mdcExecutor;
    private MockRunnable runnable;

    @Before
    public void setUp() {
        executorService = Executors.newSingleThreadExecutor();
        mdcExecutor = new MdcExecutor(executorService);

        Clock.freeze();

        runnable = new MockRunnable();
    }

    @After
    public void tearDown() {
        runnable.reset();
        MDC.clear();
        Clock.clear();
    }

    @Test
    public void addsMetadataToMDC() {
        mdcExecutor.execute(runnable);

        awaitTermination();

        assertThat(runnable.getValue(TASK_CLASS_NAME), containsString("MdcExecutorTest"));
    }

    @Test
    public void addsParentMdcContextToRunnable() {
        MDC.put(PARENT, TEST);
        MDC.put("endpoint", "test_endpoint");
        MDC.put("exception_name", "test_exception_name");
        mdcExecutor.execute(runnable);

        awaitTermination();

        assertThat(runnable.getValue(PARENT), is(TEST));
        assertThat(runnable.getValue("parent_endpoint"), is("test_endpoint"));
        assertThat(runnable.getValue("exception_name"), nullValue());
    }

    @SuppressWarnings("Duplicates")
    private void awaitTermination() {
        executorService.shutdown();

        synchronized (executorService) {
            while (!executorService.isTerminated() && executorService.isShutdown()) {
                try {
                    Thread.sleep(1); //simulate awaitTermination
                } catch (InterruptedException ignored) {
                    Thread.interrupted();
                }
            }
        }
    }

    private class MockRunnable implements Runnable {
        private Map<String, String> mdcCopy;

        @Override
        public void run() {
            mdcCopy = MDC.getCopyOfContextMap();
            Clock.freeze(Clock.now().plus(1000));
        }

        public String getValue(String key) {
            return mdcCopy.get(key);
        }

        public void reset() {
            mdcCopy = null;
        }
    }
}
