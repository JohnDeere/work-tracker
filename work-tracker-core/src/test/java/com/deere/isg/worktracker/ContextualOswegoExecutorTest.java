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

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import com.deere.clock.Clock;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static com.deere.isg.worktracker.ExecutorTestUtils.UUID_PATTERN;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ContextualOswegoExecutorTest {
    private static final String PARENT = "parent";
    private static final String TEST = "test";

    private ContextualOswegoExecutor contextualExecutor;
    private PooledExecutor executorService;
    private ExecutorTestUtils.MockRunnable runnable;
    private Logger logger;

    void createExecutor() {
        MDC.clear();
        executorService = new PooledExecutor(1);

        Clock.freeze();

        runnable = new ExecutorTestUtils.MockRunnable();
        logger = mock(Logger.class);
    }

    @Before
    public void setUp() {
        createExecutor();
        contextualExecutor = new ContextualOswegoExecutor(executorService);
    }

    @Test
    public void addsMetadataToMDC() throws InterruptedException {
        contextualExecutor.execute(runnable);
        awaitTermination();

        boolean match = UUID_PATTERN.matcher(runnable.getValue("task_id")).matches();

        assertThat(match).isTrue();
        assertThat(runnable.getValue("task_class_name")).contains("MockRunnable");
    }

    private void awaitTermination() throws InterruptedException {
        executorService.shutdownAfterProcessingCurrentlyQueuedTasks();
        executorService.awaitTerminationAfterShutdown();
    }

    @Test
    public void verifyStartAndEndLogs() throws InterruptedException {
        ArgumentCaptor<SingleFieldAppendingMarker> timeCapture = ArgumentCaptor
                .forClass(SingleFieldAppendingMarker.class);

        contextualExecutor.setLogger(logger);
        contextualExecutor.execute(runnable);

        awaitTermination();

        verify(logger).info("Task started", kv("time_interval", "start"));

        verify(logger).info(ArgumentMatchers.startsWith("Task ended after {} ms"), timeCapture.capture(), eq(kv("time_interval", "end")));
        assertThat(timeCapture.getValue().getFieldName()).isEqualTo("elapsed_ms");
        assertThat(timeCapture.getValue().toString()).matches(".*(\\d{4}).*");
    }

    @Test
    public void addsParentMdcContextToRunnable() throws InterruptedException {
        MDC.put(PARENT, TEST);
        MDC.put("endpoint", "test_endpoint");
        MDC.put("exception_name", "test_exception_name");
        contextualExecutor.execute(runnable);
        awaitTermination();

        assertThat(runnable.getValue(PARENT)).isEqualTo(TEST);
        assertThat(runnable.getValue("exception_name")).isNull();
    }

    @Test
    public void removesExceptionNamesFromParentMDC() {
        Map<String, String> keys = new HashMap<>();
        keys.put("exception_root_name", "test_root_exception_name");
        keys.put("exception_name", "test_exception_name");
        Map<String, String> cleanKeys = contextualExecutor.cleanseParentMdc(keys);

        assertThat(cleanKeys.get("exception_root_name")).isNull();
        assertThat(cleanKeys.get("exception_name")).isNull();
    }

    @Test
    public void returnsSameParentMdcForTransform() {
        Map<String, String> parentMdc = new HashMap<>();
        parentMdc.put("key1", "value1");
        parentMdc.put("key2", "value2");

        Map<String, String> map = contextualExecutor.transformKeys(parentMdc);
        assertThat(map.entrySet()).hasSize(2);
        assertThat(map).containsEntry("key1", "value1");
        assertThat(map).containsEntry("key2", "value2");
    }
}
