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

import com.deere.isg.worktracker.ExecutorTestUtils.MockCallable;
import com.deere.isg.worktracker.ExecutorTestUtils.MockRunnable;
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.slf4j.Logger;
import org.slf4j.MDC;

import static com.deere.isg.worktracker.ExecutorTestUtils.UUID_PATTERN;
import static java.util.Collections.emptyMap;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ContextualTaskDecoratorTest {
    private static final String TASK_CLASS_NAME = "task_class_name";
    private static final String TASK_ID = "task_id";
    private static final String TEST_KEY = "testKey";
    private static final String TEST_VALUE = "testValue";
    private MockRunnable runnable;
    private TaskDecorator taskDecorator;
    private MockCallable callable;
    private Logger logger;

    @Before
    public void setUp() {
        MDC.clear();
        taskDecorator = new ContextualTaskDecorator();
        callable = new MockCallable();
        runnable = new MockRunnable();
        logger = mock(Logger.class);
    }

    @After
    public void tearDown() {
        runnable.reset();
        callable.reset();
        MDC.clear();
    }

    @Test
    public void verifyStartAndEndLogsForRunnable() {
        ArgumentCaptor<SingleFieldAppendingMarker> timeCapture = ArgumentCaptor
                .forClass(SingleFieldAppendingMarker.class);

        taskDecorator.setLogger(logger);
        taskDecorator.decorate(emptyMap(), runnable).run();
        boolean uuidMatch = UUID_PATTERN.matcher(runnable.getValue(TASK_ID)).matches();

        verify(logger).info("Task started", kv("time_interval", "start"));

        verify(logger).info(ArgumentMatchers.startsWith("Task ended after {} ms"), timeCapture.capture(), eq(kv("time_interval", "end")));
        assertThat(timeCapture.getValue().getFieldName()).isEqualTo("elapsed_ms");
        assertThat(timeCapture.getValue().toString()).matches(".*(\\d{4}).*");
        assertThat(uuidMatch).isTrue();
        assertThat(runnable.getValue(TASK_CLASS_NAME)).contains("MockRunnable");
    }

    @Test
    public void verifyStartAndEndLogsForCallable() {
        ArgumentCaptor<SingleFieldAppendingMarker> timeCapture = ArgumentCaptor
                .forClass(SingleFieldAppendingMarker.class);

        taskDecorator.setLogger(logger);
        taskDecorator.decorate(emptyMap(), runnable).run();
        boolean uuidMatch = UUID_PATTERN.matcher(runnable.getValue(TASK_ID)).matches();

        verify(logger).info("Task started", kv("time_interval", "start"));

        verify(logger).info(ArgumentMatchers.startsWith("Task ended after {} ms"), timeCapture.capture(), eq(kv("time_interval", "end")));
        assertThat(timeCapture.getValue().getFieldName()).isEqualTo("elapsed_ms");
        assertThat(timeCapture.getValue().toString()).matches(".*(\\d{4}).*");
        assertThat(uuidMatch).isTrue();
        assertThat(runnable.getValue(TASK_CLASS_NAME)).contains("MockRunnable");
    }

    @Test
    public void hasParentMdcRunnable() {
        MDC.put(TEST_KEY, TEST_VALUE);
        taskDecorator.decorate(emptyMap(), runnable).run();


        assertThat(runnable.getValue(TEST_KEY)).isEqualTo(TEST_VALUE);
    }

    @Test
    public void hasParentMdcCallable() throws Exception {
        MDC.put(TEST_KEY, TEST_VALUE);
        taskDecorator.decorate(emptyMap(), callable).call();

        assertThat(callable.getValue(TEST_KEY)).isEqualTo(TEST_VALUE);
    }
}