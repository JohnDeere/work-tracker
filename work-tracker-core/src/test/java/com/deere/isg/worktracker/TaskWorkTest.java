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

import org.junit.After;
import org.junit.Test;
import org.slf4j.MDC;

import static com.deere.isg.worktracker.TaskDecorator.TASK_CLASS_NAME;
import static com.deere.isg.worktracker.TaskDecorator.TASK_ID;
import static com.deere.isg.worktracker.Work.REQUEST_ID;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class TaskWorkTest {

    public static final String ANY_CLASS_NAME = "any class name";

    @After
    public void tearDown() {
        MDC.clear();
    }

    @Test
    public void emptyConstructor() {
        TaskWork work = new TaskWork();
        assertThat(work, instanceOf(Work.class));
        assertIdConsistency(work);
        assertThat(work.getService(), nullValue());
    }

    @Test
    public void nullClassName() {
        TaskWork work = new TaskWork(null);
        assertIdConsistency(work);
        assertThat(work.getService(), nullValue());
    }

    @Test
    public void nullParent() {
        TaskWork work = new TaskWork(null, null);
        assertIdConsistency(work);
    }

    @Test
    public void inheritsRequestIdFromParent() {
        TaskWork work = new TaskWork();
        String parentRequestId = work.getRequestId();
        work = new TaskWork(null, work);
        assertIdConsistency(work);
        assertInheritsRequestId(work, parentRequestId);
    }

    @Test
    public void addsClassNameToService() {
        TaskWork work = new TaskWork(ANY_CLASS_NAME);
        assertIdConsistency(work);
        assertThat(work.getService(), is(ANY_CLASS_NAME));
        assertThat(MDC.get(TASK_CLASS_NAME), is(ANY_CLASS_NAME));
    }

    @Test
    public void allTogetherConstructor() {
        TaskWork work = new TaskWork();
        String parentRequestId = work.getRequestId();
        work = new TaskWork(ANY_CLASS_NAME, work);
        assertIdConsistency(work);
        assertThat(work.getService(), is(ANY_CLASS_NAME));
        assertThat(MDC.get(TASK_CLASS_NAME), is(ANY_CLASS_NAME));
        assertInheritsRequestId(work, parentRequestId);
    }

    @Test
    public void extraInfo() {
        TaskWork work = new TaskWork();
        assertThat(work.getExtraInfo(), nullValue());
        work.setExtraInfo("foo");
        assertThat(work.getExtraInfo(), is("foo"));
    }

    @Test
    public void successDoesNotChangeOnceSetFalse() {
        TaskWork work = new TaskWork();
        assertThat(work.isSuccess(), is(false));
        work.setSuccess(false);
        assertThat(work.isSuccess(), is(false));
        work.setSuccess(true);
        assertThat(work.isSuccess(), is(false));
    }

    // I can see this rule changing to only not changing out of false
    @Test
    public void successDoesNotChangeOnceSetTrue() {
        TaskWork work = new TaskWork();
        assertThat(work.isSuccess(), is(false));
        work.setSuccess(true);
        assertThat(work.isSuccess(), is(true));
        work.setSuccess(false);
        assertThat(work.isSuccess(), is(true));
    }

    private void assertInheritsRequestId(TaskWork work, String parentRequestId) {
        assertThat(work.getRequestId(), is(parentRequestId));
        assertThat(MDC.get(REQUEST_ID), is(work.getRequestId()));
    }

    private void assertIdConsistency(TaskWork work) {
        assertThat(work.getRequestId(), notNullValue());
        assertThat(MDC.get(REQUEST_ID), is(work.getRequestId()));
        assertThat(work.getTaskId(), notNullValue());
        assertThat(MDC.get(TASK_ID), is(work.getTaskId()));
    }
}
