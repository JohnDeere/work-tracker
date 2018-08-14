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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import static java.util.Collections.emptyMap;
import static net.logstash.logback.argument.StructuredArguments.kv;

public class ContextualExecutor implements Executor {
    public static final String TASK_TIME_INTERVAL = Work.TIME_INTERVAL;
    public static final String TASK_ELAPSED_MS = Work.ELAPSED_MS;
    public static final String TASK_ID = "task_id";
    public static final String TASK_CLASS_NAME = "task_class_name";

    private Logger logger = LoggerFactory.getLogger(ContextualExecutor.class);
    private Executor executor;

    public ContextualExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void execute(Runnable runnable) {
        Map<String, String> parentMdc = cleanseParentMdc(MDC.getCopyOfContextMap());
        executor.execute(() -> wrap(parentMdc, runnable));
    }

    protected Map<String, String> cleanseParentMdc(Map<String, String> parentMdc) {
        if (parentMdc != null) {
            Map<String, String> copy = transformKeys(parentMdc);
            removeKeys().forEach(copy::remove);
            return copy;
        }

        return emptyMap();
    }

    protected Map<String, String> transformKeys(Map<String, String> parentMdc) {
        return parentMdc;
    }

    protected List<String> removeKeys() {
        return Arrays.asList(
                RootCauseTurboFilter.FIELD_ROOT_CAUSE_NAME,
                RootCauseTurboFilter.FIELD_CAUSE_NAME
        );
    }

    protected void setLogger(Logger logger) {
        this.logger = logger;
    }

    private void wrap(Map<String, String> parentMdc, Runnable runnable) {
        long startTime = Clock.now().getMillis();
        try {
            Optional.ofNullable(parentMdc).orElse(emptyMap()).forEach(MDC::put);
            addMetadataToMDC(getClassName(runnable));
            taskStart();
            runnable.run();
        } finally {
            taskEnd(Clock.now().getMillis() - startTime);
            MDC.clear();
        }
    }

    private void taskStart() {
        logger.info("Task started", kv(TASK_TIME_INTERVAL, "start"));
    }

    private void taskEnd(long elapsedMillis) {
        logger.info("Task ended", kv(TASK_TIME_INTERVAL, "end"),
                kv(TASK_ELAPSED_MS, elapsedMillis));
    }

    private void addMetadataToMDC(String className) {
        MDC.put(TASK_ID, UUID.randomUUID().toString());

        if (className != null) {
            MDC.put(TASK_CLASS_NAME, className);
        }

    }

    private String getClassName(Runnable runnable) {
        return runnable != null ? runnable.getClass().getName() : null;
    }
}
