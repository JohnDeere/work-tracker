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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.MDC;
import org.slf4j.Marker;

public class RootCauseTurboFilter extends TurboFilter {
    public static final String FIELD_ROOT_CAUSE_NAME = "exception_root_name";
    public static final String FIELD_CAUSE_NAME = "exception_name";

    private String rootCauseFieldName = FIELD_ROOT_CAUSE_NAME;
    private String causeFieldName = FIELD_CAUSE_NAME;

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level,
            String format, Object[] params, Throwable throwable) {
        addThrowableToMDC(throwable);
        return FilterReply.NEUTRAL;
    }

    public String getRootCauseFieldName() {
        return rootCauseFieldName;
    }

    public void setRootCauseFieldName(String rootCauseFieldName) {
        this.rootCauseFieldName = rootCauseFieldName;
    }

    public String getCauseFieldName() {
        return causeFieldName;
    }

    public void setCauseFieldName(String causeFieldName) {
        this.causeFieldName = causeFieldName;
    }

    protected String getCauseClassName(Throwable throwable) {
        return throwable != null ? throwable.getClass().getName() : null;
    }

    protected Throwable findRootCause(Throwable throwable) {
        Throwable child = throwable;
        while (child != null) {
            Throwable parent = child.getCause();
            if (parent == null || child == parent || throwable == parent) {
                return child;
            }
            child = parent;
        }
        return null;
    }

    protected void addThrowableToMDC(Throwable cause) {
        if (cause != null) {
            String causeName = getCauseClassName(cause);

            Throwable rootCause = findRootCause(cause);
            String rootCauseName = getCauseClassName(rootCause);

            if (causeName != null) {
                MDC.put(causeFieldName, causeName);
            }

            if (rootCauseName != null) {
                MDC.put(rootCauseFieldName, rootCauseName);
            }
        }
    }
}
