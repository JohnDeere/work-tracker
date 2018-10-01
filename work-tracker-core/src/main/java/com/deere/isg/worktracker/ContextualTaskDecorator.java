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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import static java.util.Collections.emptyMap;
import static net.logstash.logback.argument.StructuredArguments.kv;

public class ContextualTaskDecorator {
    public static final String TASK_TIME_INTERVAL = Work.TIME_INTERVAL;
    public static final String TASK_ELAPSED_MS = Work.ELAPSED_MS;
    public static final String TASK_ID = "task_id";
    public static final String TASK_CLASS_NAME = "task_class_name";
    private Logger logger = LoggerFactory.getLogger(ContextualTaskDecorator.class);

    public Runnable decorate(Map<String, String> parentMdc, Runnable runnable) {
        return () -> {
            long startTime = nowInMillis();
            try {
                beforeExecute(parentMdc, getClassName(runnable));
                runnable.run();
            } finally {
                afterExecute(startTime);
            }
        };
    }

    public <T> Callable<T> decorate(Map<String, String> parentMdc, Callable<T> task) {
        return () -> {
            long startTime = nowInMillis();
            try {
                beforeExecute(parentMdc, getClassName(task));
                return task.call();
            } finally {
                afterExecute(startTime);
            }
        };

    }

    protected void setLogger(Logger logger) {
        this.logger = logger;
    }

    protected final void beforeExecute(Map<String, String> parentMdc, String className) {
        Optional.ofNullable(parentMdc).orElse(emptyMap()).forEach(MDC::put);
        addMetadataToMDC(className);
        taskStart();
    }

    protected final void afterExecute(long startTime) {
        taskEnd(nowInMillis() - startTime);
        MDC.clear();
    }

    protected String getClassName(Object object) {
        return object != null ? object.getClass().getName() : null;
    }

    private void taskStart() {
        logger.info("Task started", kv(TASK_TIME_INTERVAL, "start"));
    }

    private void taskEnd(long elapsedMillis) {
        logger.info("Task ended after {} ms", kv(TASK_ELAPSED_MS, elapsedMillis), kv(TASK_TIME_INTERVAL, "end"));
    }

    private void addMetadataToMDC(String className) {
        MDC.put(TASK_ID, UUID.randomUUID().toString());

        if (className != null) {
            MDC.put(TASK_CLASS_NAME, className);
        }

    }

    private long nowInMillis() {
        return Clock.milliseconds();
    }
}
