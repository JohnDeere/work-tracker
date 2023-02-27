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

import net.logstash.logback.argument.StructuredArgument;
import org.slf4j.MDC;

import java.util.List;
import java.util.UUID;

import static com.deere.isg.worktracker.ContextualTaskDecorator.*;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

public class TaskWork extends Work {
    private String service;
    private String taskId = addToMDC(TASK_ID, UUID.randomUUID().toString());
    private Boolean success = null;
    private String extraInfo;

    protected TaskWork() {}

    public TaskWork(String className) {
        this(className, MDC.get(REQUEST_ID));
    }

    private TaskWork(String className, String requestId) {
        super(requestId);
        setService(addToMDC(TASK_CLASS_NAME, className));
    }

    public TaskWork(String className, Work parent) {
        this(className, parent != null ? parent.getRequestId() : MDC.get(REQUEST_ID));
    }

    @Override
    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = addToMDC("service", service);
    }

    @Override
    public List<StructuredArgument> getStartInfo() {
        return getIntervalInfo("start");
    }

    @Override
    public List<StructuredArgument> getEndInfo() {
        return getIntervalInfo("end");
    }

    private List<StructuredArgument> getIntervalInfo(String interval) {
        List<StructuredArgument> intervalInfo = getThreadInfo();
        intervalInfo.add(keyValue(TASK_TIME_INTERVAL, interval));
        if(success != null) {
            intervalInfo.add(keyValue("successful", success));
        }
        return intervalInfo;
    }

    public String getTaskId() {
        return taskId;
    }

    public boolean setSuccess(boolean successful) {
        if(success == null ) {
            success = successful;
        }
        return success;
    }

    public boolean isSuccess() {
        return success == null ? false : success;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }

    @Override
    public String getExtraInfo() {
        return extraInfo;
    }
}
