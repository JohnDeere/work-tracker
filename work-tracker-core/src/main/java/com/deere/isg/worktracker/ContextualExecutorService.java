/**
 * Copyright 2019 Deere & Company
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class ContextualExecutorService extends ContextualExecutor implements ExecutorService {
    private final ExecutorService executorService;

    public ContextualExecutorService(ExecutorService executorService) {
        this(executorService, new ContextualTaskDecorator());
    }

    public ContextualExecutorService(ExecutorService executorService, TaskDecorator decorator) {
        super(executorService, decorator);
        this.executorService = executorService;
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        Map<String, String> parentMdc = cleanseParentMdc();
        return executorService.submit(taskDecorator.decorate(parentMdc, task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        Map<String, String> parentMdc = cleanseParentMdc();
        return executorService.submit(taskDecorator.decorate(parentMdc, task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        Map<String, String> parentMdc = cleanseParentMdc();
        return executorService.submit(taskDecorator.decorate(parentMdc, task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        Map<String, String> parentMdc = cleanseParentMdc();
        return executorService.invokeAll(wrapTasks(parentMdc, tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        Map<String, String> parentMdc = cleanseParentMdc();
        return executorService.invokeAll(wrapTasks(parentMdc, tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        Map<String, String> parentMdc = cleanseParentMdc();
        return executorService.invokeAny(wrapTasks(parentMdc, tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        Map<String, String> parentMdc = cleanseParentMdc();
        return executorService.invokeAny(wrapTasks(parentMdc, tasks), timeout, unit);
    }

    private <T> List<Callable<T>> wrapTasks(Map<String, String> parentMdc, Collection<? extends Callable<T>> tasks) {
        return tasks.stream()
                .map(t -> taskDecorator.decorate(parentMdc, t))
                .collect(Collectors.toList());
    }
}
