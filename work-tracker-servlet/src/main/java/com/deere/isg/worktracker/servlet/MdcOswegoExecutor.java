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

import EDU.oswego.cs.dl.util.concurrent.Executor;
import com.deere.isg.worktracker.ContextualOswegoExecutor;
import com.deere.isg.worktracker.OutstandingTaskDecorator;
import com.deere.isg.worktracker.OutstandingWorkTracker;
import com.deere.isg.worktracker.TaskDecorator;
import com.deere.isg.worktracker.TaskWork;
import com.deere.isg.worktracker.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class MdcOswegoExecutor extends ContextualOswegoExecutor {
    public static final String PARENT_ENDPOINT = "parent_endpoint";
    public static final String PARENT_PATH = "parent_path";

    private Logger logger = LoggerFactory.getLogger(MdcExecutor.class);

    public MdcOswegoExecutor(Executor executor) {
        super(executor);
        setLogger(logger);
    }

    public MdcOswegoExecutor(Executor executor, TaskDecorator taskDecorator) {
        super(executor, taskDecorator);
        setLogger(logger);
    }

    public MdcOswegoExecutor(Executor executor, OutstandingWorkTracker<Work> outstandingWork, BiFunction<Object, Work, TaskWork> workFactory) {
        this(executor, new OutstandingTaskDecorator<>(outstandingWork, workFactory));
    }

    public MdcOswegoExecutor(Executor executor, OutstandingWorkTracker<Work> outstandingWork) {
        this(executor, outstandingWork, (t,p)->new TaskWork(t.getClass().getName(), p));
    }

    @Override
    protected Map<String, String> transformKeys(Map<String, String> parentMdc) {
        Map<String, String> copy = new HashMap<>(parentMdc);
        copy.put(PARENT_ENDPOINT, copy.remove("endpoint"));
        copy.put(PARENT_PATH, copy.remove(HttpWork.PATH));
        return copy;
    }
}
