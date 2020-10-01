/**
 * Copyright 2020 Deere & Company
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

import org.junit.Test;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OutstandingWorkFilterTest {
    private static class SuperWork extends Work {}
    private static class TestWork extends SuperWork {}
    private static class AnotherWork extends SuperWork {}
    private OutstandingWork<Work> base = new OutstandingWork<>();
    private OutstandingWorkTracker<TestWork> filtered = new OutstandingWorkFilter<>(base, TestWork.class);

    @Test
    public void stream() {
        base.doInTransaction(new SuperWork(), ()->{
            assertThat(filtered.stream().findFirst().isPresent(), is(false));
        });
        base.doInTransaction(new AnotherWork(), ()->{
            assertThat(filtered.stream().findFirst().isPresent(), is(false));
        });
        TestWork work = new TestWork();
        base.doInTransaction(work, ()->{
            assertThat(filtered.stream().findFirst().orElse(null), is(work));
        });
    }

    @Test
    public void current() {
        base.doInTransaction(new SuperWork(), ()->{
            assertThat(filtered.current().isPresent(), is(false));
        });
        base.doInTransaction(new AnotherWork(), ()->{
            assertThat(filtered.current().isPresent(), is(false));
        });
        TestWork work = new TestWork();
        base.doInTransaction(work, ()->{
            assertThat(filtered.current().orElse(null), is(work));
        });
    }

    @Test
    public void create() {
        TestWork payload = new TestWork();
        assertThat(filtered.create(payload).getPayload().orElse(null), is(payload));
    }

    @Test
    public void doInTransaction() {
        TestWork payload = new TestWork();
        Runnable runnable = mock(Runnable.class);
        filtered.doInTransaction(payload, runnable);
        verify(runnable).run();
        filtered.doInTransaction(payload, ()->{
            assertThat(filtered.current().orElseGet(null), is(payload));
        });
    }

    @Test
    public void iterable() {
        base.doInTransaction(new SuperWork(), ()->{
            final Iterator<TestWork> iterator = filtered.iterator();
            assertThat(iterator.hasNext(), is(false));
            assertNoSuchElement(iterator);
        });
        base.doInTransaction(new AnotherWork(), ()->{
            final Iterator<TestWork> iterator = filtered.iterator();
            assertThat(iterator.hasNext(), is(false));
            assertNoSuchElement(iterator);
        });
        TestWork work = new TestWork();
        base.doInTransaction(work, ()->{
            final Iterator<TestWork> iterator = filtered.iterator();
            assertThat(iterator.hasNext(), is(true));
            assertThat(iterator.next(), is(work));
        });
    }

    private void assertNoSuchElement(Iterator<?> iterator) {
        try {
            iterator.next();
            fail("Should have thrown exception");
        } catch(NoSuchElementException e) {
            // expected
        }
    }
}