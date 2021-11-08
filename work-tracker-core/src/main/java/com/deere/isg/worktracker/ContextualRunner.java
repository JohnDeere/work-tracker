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

package com.deere.isg.worktracker;

import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;

class ContextualRunner {
    protected final TaskDecorator taskDecorator;

    ContextualRunner(TaskDecorator taskDecorator) {
        this.taskDecorator = taskDecorator;
    }

    protected Map<String, String> cleanseParentMdc() {
        return cleanseParentMdc(MDC.getCopyOfContextMap());
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
        taskDecorator.setLogger(logger);
    }
}
