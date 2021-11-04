/**
 * Copyright 2021 Deere & Company
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

import java.util.Map;
import java.util.concurrent.Callable;

public interface TaskDecorator {
    String TASK_TIME_INTERVAL = Work.TIME_INTERVAL;
    String TASK_ELAPSED_MS = Work.ELAPSED_MS;
    String TASK_ID = "task_id";
    String TASK_CLASS_NAME = "task_class_name";

    Runnable decorate(Map<String, String> parentMdc, Runnable runnable);

    <T> Callable<T> decorate(Map<String, String> parentMdc, Callable<T> task);

    void setLogger(Logger logger);
}
