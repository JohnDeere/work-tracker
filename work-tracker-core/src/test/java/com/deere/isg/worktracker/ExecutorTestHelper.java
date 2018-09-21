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

import com.deere.clock.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static com.deere.isg.worktracker.ContextualExecutor.TASK_ID;
import static org.mockito.Mockito.mock;

class ExecutorTestHelper {
    protected static final Pattern UUID_PATTERN =
            Pattern.compile("^[a-zA-Z0-9]{8}-([a-zA-Z0-9]{4}-){3}[a-zA-Z0-9]{12}$");
    protected ExecutorService executorService;
    protected MockRunnable runnable;
    protected Logger logger;

    @SuppressWarnings("Duplicates")
    void awaitTermination() {
        assert executorService != null;
        executorService.shutdown();

        synchronized (executorService) {
            while (!executorService.isTerminated() && executorService.isShutdown()) {
                try {
                    Thread.sleep(1); //simulate awaitTermination
                } catch (InterruptedException ignored) {
                    Thread.interrupted();
                }

                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    void createExecutor() {
        MDC.clear();
        executorService = Executors.newSingleThreadExecutor();

        Clock.freeze();

        runnable = new MockRunnable();
        logger = mock(Logger.class);
    }

    void resetExecutor() {
        if (runnable != null) {
            runnable.reset();
        }
        MDC.clear();
        Clock.clear();

    }

    class MockTask {
        protected Logger actualLogger = LoggerFactory.getLogger(MockTask.class);
        protected Map<String, String> mdcCopy;

        public String getValue(String key) {
            return mdcCopy.get(key);
        }

        public void reset() {
            mdcCopy = null;
        }
    }

    class MockRunnable extends MockTask implements Runnable {
        @Override
        public void run() {
            mdcCopy = MDC.getCopyOfContextMap();
            actualLogger.debug(MDC.get(TASK_ID));
            Clock.freeze(Clock.now().plus(1000));
        }
    }

    class MockCallable extends MockTask implements Callable<String> {
        private final String output;

        MockCallable() {
            this("test");
        }

        MockCallable(String output) {
            this.output = output;
        }

        @Override
        public String call() throws Exception {
            mdcCopy = MDC.getCopyOfContextMap();
            actualLogger.debug(MDC.get(TASK_ID));
            Clock.freeze(Clock.now().plus(1000));
            return output;
        }

    }
}
