/**
 * Copyright 2018-2021 Deere & Company
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

import com.deere.isg.worktracker.ExecutorTestUtils.MockCallable;
import com.deere.isg.worktracker.ExecutorTestUtils.MockTask;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.deere.isg.worktracker.ExecutorTestUtils.UUID_PATTERN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ContextualExecutorServiceTest extends ExecutorTestHelper {
    private ExecutorService contextualExecutor;
    private MockCallable callable;

    @Before
    public void setUp() {
        createExecutor();
        contextualExecutor = new ContextualExecutorService(executorService);
        callable = new MockCallable();
    }

    @After
    public void tearDown() {
        resetExecutor();
    }

    @Test
    public void hasSameFunctionalityAsExecutorService() throws Exception {
        ExecutorService executor = mock(ExecutorService.class);
        ContextualExecutorService contextualService = new ContextualExecutorService(executor);

        contextualService.shutdown();
        verify(executor).shutdown();

        contextualService.shutdownNow();
        verify(executor).shutdownNow();

        contextualService.isShutdown();
        verify(executor).isShutdown();

        contextualService.isTerminated();
        verify(executor).isTerminated();

        contextualService.awaitTermination(10, TimeUnit.SECONDS);
        verify(executor).awaitTermination(10, TimeUnit.SECONDS);
    }

    @Test
    public void addsMetadataToMDCSubmit() throws Exception {
        resetInterruptOrIgnoreTest();
        Future<String> future = contextualExecutor.submit(callable);
        String output = future.get();

        awaitTermination();

        assertTaskHasMetadata(output, "MockCallable", callable);
    }

    @Test
    public void addsMetadataToMDCSubmitRunnableWithResult() throws Exception {
        resetInterruptOrIgnoreTest();
        Future<String> future = contextualExecutor.submit(runnable, "test");
        String output = future.get();

        awaitTermination();

        assertTaskHasMetadata(output, "MockRunnable", runnable);
    }

    @Test
    public void addsMetadataToMDCSubmitRunnable() throws Exception {
        resetInterruptOrIgnoreTest();
        Future<?> future = contextualExecutor.submit(runnable);
        String output = (String) future.get();

        awaitTermination();

        boolean match = UUID_PATTERN.matcher(runnable.getValue("task_id")).matches();

        assertThat(match, is(true));
        assertThat(output, is(nullValue()));
        assertThat(runnable.getValue("task_class_name"), containsString("MockRunnable"));
    }

    @Test
    public void addsMetadataToMDCInvokeAll() throws Exception {
        resetInterruptOrIgnoreTest();
        String output = contextualExecutor.invokeAll(Collections.singleton(callable)).get(0).get();

        awaitTermination();

        assertTaskHasMetadata(output, "MockCallable", callable);
    }

    @Test
    public void addsMetadataToMDCInvokeAllWithTimeout() throws Exception {
        resetInterruptOrIgnoreTest();
        String output = contextualExecutor.invokeAll(Collections.singleton(callable), 10, TimeUnit.SECONDS).get(0).get();

        awaitTermination();

        assertTaskHasMetadata(output, "MockCallable", callable);
    }

    @Test
    public void addsMetadataToMDCInvokeAny() throws Exception {
        resetInterruptOrIgnoreTest();
        String output = contextualExecutor.invokeAny(Collections.singleton(callable));

        awaitTermination();

        assertTaskHasMetadata(output, "MockCallable", callable);
    }

    @Test
    public void addsMetadataToMDCInvokeAnyWithTimeout() throws Exception {
        resetInterruptOrIgnoreTest();
        String output = contextualExecutor.invokeAny(Collections.singleton(callable), 10, TimeUnit.SECONDS);

        awaitTermination();

        assertTaskHasMetadata(output, "MockCallable", callable);
    }

    private void assertTaskHasMetadata(String output, String taskClassName, MockTask task) {
        boolean match = UUID_PATTERN.matcher(task.getValue("task_id")).matches();

        assertThat(match, is(true));
        assertThat(output, is("test"));
        assertThat(task.getValue("task_class_name"), containsString(taskClassName));
    }

    private void resetInterruptOrIgnoreTest() {
        if (Thread.currentThread().isInterrupted() && !Thread.interrupted()) {
            Assume.assumeFalse(Thread.interrupted());
        }
    }

}