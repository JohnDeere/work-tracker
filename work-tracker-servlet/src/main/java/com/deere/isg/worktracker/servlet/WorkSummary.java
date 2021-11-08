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


package com.deere.isg.worktracker.servlet;

import com.deere.isg.worktracker.Work;

import java.util.Date;
import java.util.Objects;

public class WorkSummary<W extends Work> {
    private Date startTime;
    private String service;
    private String requestId;
    private String threadName;
    private String acceptHeader;
    private String elapsedMillis;
    private boolean zombie;

    public WorkSummary(W work) {
        if (work != null) {
            setStartTime(new Date(work.getStartTime()));
            setElapsedMillis(String.valueOf(work.getElapsedMillis()));
            String service = work.getService();
            setService(service == null ? "" : service);
            setRequestId(work.getRequestId());
            setThreadName(work.getThreadName());
            String extraInfo = work.getExtraInfo();
            setAcceptHeader(extraInfo == null ? "" : extraInfo);
            setZombie(work.isZombie());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorkSummary summary = (WorkSummary) o;
        return Objects.equals(requestId, summary.requestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }

    public boolean isZombie() {
        return zombie;
    }

    public void setZombie(boolean zombie) {
        this.zombie = zombie;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = new Date(startTime);
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getAcceptHeader() {
        return acceptHeader;
    }

    public void setAcceptHeader(String acceptHeader) {
        if (acceptHeader != null){
            this.acceptHeader = acceptHeader.replaceAll(";\\s*", ";&#8203;");
        }
    }

    public String getElapsedMillis() {
        return elapsedMillis;
    }

    public void setElapsedMillis(String elapsedMillis) {
        this.elapsedMillis = elapsedMillis + " ms";
    }

    @Override
    public String toString() {
        return "startTime=" + startTime +
                "service=" + service +
                "requestId=" + requestId +
                "threadName=" + threadName +
                "acceptHeader=" + acceptHeader +
                "elapsed_ms=" + elapsedMillis;
    }
}
