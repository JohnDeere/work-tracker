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

package com.deere.isg.worktracker;

import com.deere.clock.Clock;
import com.deere.isg.worktracker.ExecutorTestUtils.MockRunnable;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.mock;

class ExecutorTestHelper {
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
}
