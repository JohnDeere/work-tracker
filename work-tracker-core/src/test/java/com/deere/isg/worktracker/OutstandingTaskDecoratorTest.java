/**
 * Copyright 2019 Deere & Company
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

import net.logstash.logback.marker.SingleFieldAppendingMarker;
import org.hamcrest.CustomMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.regex.Pattern;

import static com.deere.isg.worktracker.ExecutorTestUtils.UUID_PATTERN;
import static java.util.Collections.emptyMap;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OutstandingTaskDecoratorTest {
    private static final String TASK_CLASS_NAME = "task_class_name";
    private static final String TASK_ID = "task_id";
    private static final String TEST_KEY = "testKey";
    private static final String TEST_VALUE = "testValue";
    private ExecutorTestUtils.MockRunnable runnable;
    private TaskDecorator taskDecorator;
    private ExecutorTestUtils.MockCallable callable;
    private Logger logger;
    private OutstandingWork<Work> outstandingWork = new OutstandingWork<>();

    @Before
    public void setUp() {
        MDC.clear();
        callable = new ExecutorTestUtils.MockCallable();
        runnable = new ExecutorTestUtils.MockRunnable();
        taskDecorator = new OutstandingTaskDecorator<>(outstandingWork,
                (t,p)->new TaskWork(runnable.getClass().getName()));
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

        verify(logger).info(eq("Task started"), anyObject(),
                eq(kv("zombie", false)), eq(kv("time_interval", "start")));

        verify(logger).info(ArgumentMatchers.startsWith("Task ended: {1} {3}"),
                timeCapture.capture(), eq(kv("zombie", false)),
                eq(kv("time_interval", "end")));

        assertThat(timeCapture.getValue().getFieldName(), is("elapsed_ms"));
        assertThat(timeCapture.getValue().toString(), matches(".*(\\d{4}).*"));
        assertThat(uuidMatch, is(true));
        assertThat(runnable.getValue(TASK_CLASS_NAME), containsString("MockRunnable"));
    }

    @Test
    public void verifyStartAndEndLogsForCallable() {
        ArgumentCaptor<SingleFieldAppendingMarker> timeCapture = ArgumentCaptor
                .forClass(SingleFieldAppendingMarker.class);

        taskDecorator.setLogger(logger);
        taskDecorator.decorate(emptyMap(), runnable).run();
        boolean uuidMatch = UUID_PATTERN.matcher(runnable.getValue(TASK_ID)).matches();

        verify(logger).info(eq("Task started"), anyObject(),
                eq(kv("zombie", false)), eq(kv("time_interval", "start")));

        verify(logger).info(ArgumentMatchers.startsWith("Task ended: {1} {3}"),
                timeCapture.capture(), eq(kv("zombie", false)),
                eq(kv("time_interval", "end")));
        assertThat(timeCapture.getValue().getFieldName(), is("elapsed_ms"));
        assertThat(timeCapture.getValue().toString(), matches(".*(\\d{4}).*"));
        assertThat(uuidMatch, is(true));
        assertThat(runnable.getValue(TASK_CLASS_NAME), containsString("MockRunnable"));
    }

    @Test
    public void hasParentMdcRunnable() {
        MDC.put(TEST_KEY, TEST_VALUE);
        taskDecorator.decorate(emptyMap(), runnable).run();


        assertThat(runnable.getValue(TEST_KEY), is(TEST_VALUE));
    }

    @Test
    public void hasParentMdcCallable() throws Exception {
        MDC.put(TEST_KEY, TEST_VALUE);
        taskDecorator.decorate(emptyMap(), callable).call();

        assertThat(callable.getValue(TEST_KEY), is(TEST_VALUE));
    }

    private CustomMatcher<String> matches(String regex) {
        return new CustomMatcher<String>(regex) {
            @Override
            public boolean matches(Object item) {
                return Pattern.compile(regex)
                        .matcher((String) item)
                        .matches();
            }
        };
    }
}