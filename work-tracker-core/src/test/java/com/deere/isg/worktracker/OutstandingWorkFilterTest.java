package com.deere.isg.worktracker;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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
}