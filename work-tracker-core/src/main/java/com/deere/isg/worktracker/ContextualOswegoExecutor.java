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

import EDU.oswego.cs.dl.util.concurrent.Executor;

import java.util.Map;

public class ContextualOswegoExecutor extends ContextualRunner implements Executor {
    private Executor executor;

    public ContextualOswegoExecutor(Executor executor) {
        this(executor, new ContextualTaskDecorator());
    }

    public ContextualOswegoExecutor(Executor executor, TaskDecorator taskDecorator) {
        super(taskDecorator);
        this.executor = executor;
    }

    @Override
    public void execute(Runnable runnable) throws InterruptedException {
        Map<String, String> parentMdc = cleanseParentMdc();
        executor.execute(taskDecorator.decorate(parentMdc, runnable));
    }
 }