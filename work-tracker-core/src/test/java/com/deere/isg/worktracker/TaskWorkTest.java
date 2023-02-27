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

import org.junit.After;
import org.junit.Test;
import org.slf4j.MDC;

import static com.deere.isg.worktracker.TaskDecorator.TASK_CLASS_NAME;
import static com.deere.isg.worktracker.TaskDecorator.TASK_ID;
import static com.deere.isg.worktracker.Work.REQUEST_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class TaskWorkTest {

    public static final String ANY_CLASS_NAME = "any class name";

    @After
    public void tearDown() {
        MDC.clear();
    }

    @Test
    public void emptyConstructor() {
        TaskWork work = new TaskWork();
        assertThat(work).isInstanceOf(Work.class);
        assertIdConsistency(work);
        assertThat(work.getService()).isNull();
    }

    @Test
    public void nullClassName() {
        TaskWork work = new TaskWork(null);
        assertIdConsistency(work);
        assertThat(work.getService()).isNull();
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
        assertThat(work.getService()).isEqualTo(ANY_CLASS_NAME);
        assertThat(MDC.get(TASK_CLASS_NAME)).isEqualTo(ANY_CLASS_NAME);
    }

    @Test
    public void allTogetherConstructor() {
        TaskWork work = new TaskWork();
        String parentRequestId = work.getRequestId();
        work = new TaskWork(ANY_CLASS_NAME, work);
        assertIdConsistency(work);
        assertThat(work.getService()).isEqualTo(ANY_CLASS_NAME);
        assertThat(MDC.get(TASK_CLASS_NAME)).isEqualTo(ANY_CLASS_NAME);
        assertInheritsRequestId(work, parentRequestId);
    }

    @Test
    public void extraInfo() {
        TaskWork work = new TaskWork();
        assertThat(work.getExtraInfo()).isNull();
        work.setExtraInfo("foo");
        assertThat(work.getExtraInfo()).isEqualTo("foo");
    }

    @Test
    public void successDoesNotChangeOnceSetFalse() {
        TaskWork work = new TaskWork();
        assertThat(work.isSuccess()).isFalse();
        work.setSuccess(false);
        assertThat(work.isSuccess()).isFalse();
        work.setSuccess(true);
        assertThat(work.isSuccess()).isFalse();
    }

    // I can see this rule changing to only not changing out of false
    @Test
    public void successDoesNotChangeOnceSetTrue() {
        TaskWork work = new TaskWork();
        assertThat(work.isSuccess()).isFalse();
        work.setSuccess(true);
        assertThat(work.isSuccess()).isTrue();
        work.setSuccess(false);
        assertThat(work.isSuccess()).isTrue();
    }

    private void assertInheritsRequestId(TaskWork work, String parentRequestId) {
        assertThat(work.getRequestId()).isEqualTo(parentRequestId);
        assertThat(MDC.get(REQUEST_ID)).isEqualTo(work.getRequestId());
    }

    private void assertIdConsistency(TaskWork work) {
        assertThat(work.getRequestId()).isNotNull();
        assertThat(MDC.get(REQUEST_ID)).isEqualTo(work.getRequestId());
        assertThat(work.getTaskId()).isNotNull();
        assertThat(MDC.get(TASK_ID)).isEqualTo(work.getTaskId());
    }
}
