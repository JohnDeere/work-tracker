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

import com.deere.isg.outstanding.Outstanding;
import net.logstash.logback.argument.StructuredArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;

import static java.util.Collections.emptyMap;

public class OutstandingTaskDecorator<W extends TaskWork> implements TaskDecorator {
    private Logger logger = LoggerFactory.getLogger(ContextualTaskDecorator.class);
    private final OutstandingWorkTracker<? super W> outstandingWork;
    private BiFunction<Object, Work, W> workFactory;

    public OutstandingTaskDecorator(OutstandingWorkTracker<? super W> outstandingWork,
                                    BiFunction<Object, Work, W> workFactory) {
        this.outstandingWork = outstandingWork;
        this.workFactory = workFactory;
    }

    @Override
    public <T> Callable<T> decorate(Map<String, String> parentMdc, Callable<T> task) {
        return ()->{
            T call;
            W payload = createWork(parentMdc, task, outstandingWork);
            try (Outstanding<?>.Ticket ignored = outstandingWork.create(payload)) {
                doStartLog(payload);
                call = task.call();
                payload.setSuccess(true);
            } finally {
                doFinally(payload);
            }
            return call;
        };
    }

    private void doFinally(W payload) {
        try {
            postProcess(payload);
        } finally {
            if (outstandingWork != null) {
                doEndLog(payload);
            }
            MDC.clear();
        }
    }

    private void doStartLog(W current) {
        List<StructuredArgument> startInfo = current != null ? current.getStartInfo() : new ArrayList<>();
        logger.info("Task started", startInfo.toArray());
    }

    private void doEndLog(W payload) {
        List<StructuredArgument> endInfo = payload != null ? payload.getEndInfo() : new ArrayList<>();
        String successMessage = (payload != null && payload.isSuccess()) ? "Successful" : "Failure";
        logger.info("Task ended: {} "+ successMessage, endInfo.toArray());
    }

    /**
     * An extension point to collect metrics or whatever you want to do with it.
     * @param payload The Work object to process.
     */
    protected void postProcess(W payload) {}

    private W createWork(Map<String, String> parentMdc, Object task,
                         OutstandingWorkTracker<? super W> outstandingWork)
    {
        W taskWork = workFactory.apply(task, outstandingWork.current().orElse(null));
        Optional.ofNullable(parentMdc).orElse(emptyMap()).forEach(taskWork::addToMDC);
        return taskWork;
    }

    @Override
    public Runnable decorate(Map<String, String> parentMdc, final Runnable runnable) {
        return ()->{
            W payload = createWork(parentMdc, runnable, outstandingWork);
            try  (Outstanding<?>.Ticket ignored = outstandingWork.create(payload)) {
                doStartLog(payload);
                runnable.run();
            } finally {
                doFinally(payload);
            }
        };
    }

    @Override
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
}
