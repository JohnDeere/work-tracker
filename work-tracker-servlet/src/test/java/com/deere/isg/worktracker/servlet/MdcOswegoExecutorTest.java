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

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import com.deere.clock.Clock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class MdcOswegoExecutorTest {
    private static final String PARENT = "parent";
    private static final String TEST = "test";
    private static final String TASK_CLASS_NAME = "task_class_name";

    private PooledExecutor executorService;
    private MdcOswegoExecutor mdcExecutor;
    private MockRunnable runnable;

    @Before
    public void setUp() {
        executorService = new PooledExecutor(1);
        mdcExecutor = new MdcOswegoExecutor(executorService);

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
    public void addsMetadataToMDC() throws InterruptedException {
        mdcExecutor.execute(runnable);

        awaitTermination();

        assertThat(runnable.getValue(TASK_CLASS_NAME), containsString("MdcOswegoExecutorTest"));
    }

    @Test
    public void addsParentMdcContextToRunnable() throws InterruptedException {
        MDC.put(PARENT, TEST);
        MDC.put("endpoint", "test_endpoint");
        MDC.put("exception_name", "test_exception_name");
        mdcExecutor.execute(runnable);

        awaitTermination();

        assertThat(runnable.getValue(PARENT), is(TEST));
        assertThat(runnable.getValue("parent_endpoint"), is("test_endpoint"));
        assertThat(runnable.getValue("exception_name"), nullValue());
    }

    private void awaitTermination() throws InterruptedException {
        executorService.shutdownAfterProcessingCurrentlyQueuedTasks();
        executorService.awaitTerminationAfterShutdown();
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