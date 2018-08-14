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
import net.logstash.logback.marker.SingleFieldAppendingMarker;
import org.hamcrest.CustomMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ContextualExecutorTest {
    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[a-zA-Z0-9]{8}-([a-zA-Z0-9]{4}-){3}[a-zA-Z0-9]{12}$");
    private static final String PARENT = "parent";
    private static final String TEST = "test";

    private ExecutorService executorService;
    private ContextualExecutor contextualExecutor;
    private MockRunnable runnable;
    private Logger logger;

    @Before
    public void setUp() {
        executorService = Executors.newSingleThreadExecutor();
        contextualExecutor = new ContextualExecutor(executorService);

        Clock.freeze();

        runnable = new MockRunnable();
        logger = mock(Logger.class);
    }

    @After
    public void tearDown() {
        runnable.reset();
        MDC.clear();
        Clock.clear();
    }

    @Test
    public void addsMetadataToMDC() {
        contextualExecutor.execute(runnable);

        awaitTermination();

        boolean match = UUID_PATTERN.matcher(runnable.getValue("task_id")).matches();

        assertThat(match, is(true));
        assertThat(runnable.getValue("task_class_name"), containsString("ContextualExecutorTest"));
    }

    @Test
    public void verifyStartAndEndLogs() {
        ArgumentCaptor<SingleFieldAppendingMarker> timeCapture = ArgumentCaptor
                .forClass(SingleFieldAppendingMarker.class);

        contextualExecutor.setLogger(logger);
        contextualExecutor.execute(runnable);

        awaitTermination();

        verify(logger).info("Task started", kv("time_interval", "start"));

        verify(logger).info(eq("Task ended"), eq(kv("time_interval", "end")), timeCapture.capture());
        assertThat(timeCapture.getValue().getFieldName(), is("elapsed_ms"));
        assertThat(timeCapture.getValue().toString(), matches(".*(\\d{4}).*"));
    }

    @Test
    public void addsParentMdcContextToRunnable() {
        MDC.put(PARENT, TEST);
        MDC.put("endpoint", "test_endpoint");
        MDC.put("exception_name", "test_exception_name");
        contextualExecutor.execute(runnable);

        awaitTermination();

        assertThat(runnable.getValue(PARENT), is(TEST));
        assertThat(runnable.getValue("exception_name"), nullValue());
    }

    @Test
    public void removesExceptionNamesFromParentMDC() {
        Map<String, String> keys = new HashMap<>();
        keys.put("exception_root_name", "test_root_exception_name");
        keys.put("exception_name", "test_exception_name");
        Map<String, String> cleanKeys = contextualExecutor.cleanseParentMdc(keys);

        assertThat(cleanKeys.get("exception_root_name"), nullValue());
        assertThat(cleanKeys.get("exception_name"), nullValue());
    }

    @Test
    public void returnsSameParentMdcForTransform() {
        Map<String, String> parentMdc = new HashMap<>();
        parentMdc.put("key1", "value1");
        parentMdc.put("key2", "value2");

        Map<String, String> map = contextualExecutor.transformKeys(parentMdc);
        assertThat(map.entrySet(), hasSize(2));
        assertThat(map, hasEntry("key1", "value1"));
        assertThat(map, hasEntry("key2", "value2"));
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
