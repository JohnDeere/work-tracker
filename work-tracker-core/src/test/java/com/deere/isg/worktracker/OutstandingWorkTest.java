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

import com.deere.clock.Clock;
import com.deere.isg.outstanding.Outstanding;
import net.logstash.logback.argument.StructuredArgument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.logstash.logback.argument.StructuredArguments.keyValue;
import static org.assertj.core.api.Assertions.assertThat;

public class OutstandingWorkTest {
    private static final String SOME_KEY = "some_key";
    private static final String SOME_VALUE = "some_value";

    private OutstandingWork<MockWork> outstanding;
    private MockWork payload;

    @Before
    public void setUp() {
        Clock.freeze();
        outstanding = new OutstandingWork<>();
        payload = new MockWork();
    }

    @After
    public void tearDown() {
        Clock.clear();
        MDC.clear();
    }

    @Test
    public void outstandingCreatesTicket() {
        outstanding.createTicket(payload);
        MockWork work = outstanding.current().get();

        assertThat(work.getMetadata()).isEqualTo(payload.getMetadata());
    }

    @Test
    public void nullDoesNotCreateTicket() {
        outstanding.createTicket(null);

        assertThat(outstanding.current()).isEqualTo(Optional.empty());
    }

    @Test
    public void getCurrentMetadataReturnsCurrentTicketsMetadata() {
        outstanding.createTicket(payload);

        assertThat(outstanding.getCurrentMetadata()).isEqualTo(payload.getMetadata());
    }

    @Test
    public void doInTransactionCheckedExecuteRunnable() {
        MockWork work = new MockWork();

        outstanding.doInTransactionChecked(work,
                () -> assertCurrentThread(outstanding, work));
    }

    @Test
    public void currentIsSetWhileInTransactionAndClearedWhenDone() {
        MockWork work = new MockWork();

        outstanding.doInTransactionChecked(work,
                () -> assertThat(outstanding.current().orElseThrow(AssertionError::new)).isEqualTo(work));

        assertThat(outstanding.current().isPresent()).isEqualTo(false);
    }

    @Test
    public void ticketsAreNotLeakedByThreadLocal() throws InterruptedException, ExecutionException {
        final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        try {
            final ReferenceQueue<Object> queue = new ReferenceQueue<>();
            // First, we need a ticket created on some thread that will reference tickets made later
            outstanding.create(new MockWork()).close();
            // Now get a ticket on another thread
            final WeakReference<Outstanding<?>.Ticket> ref = executorService.submit(() -> {
                System.out.println("I'm creating the weak reference");
                outstanding.createTicket(new MockWork()).close();
                final WeakReference<Outstanding<?>.Ticket> ticketToReferTo = new WeakReference<>(
                        outstanding.create(new MockWork()), queue);
                // we need some other ticket created that may be referenced as current on that other thread
                // to remove the ticket we want to test from being referenced there.
                outstanding.createTicket(new MockWork()).close();
                return ticketToReferTo;
            }).get();
            // Now close the ticket so that it isn't referenced from the head of OutstandingWork.
            ref.get().close();
            // Iterating through the list cleans out any stray references to the ticket we want to make sure is gc'ed
            outstanding.stream().forEach(System.out::println);

            // now let's create a bunch of garbage so the gc will actually run
            executorService.scheduleAtFixedRate(() -> {
                System.out.println("I'm running the garbage");
                outstanding.doInTransaction(new MockWork(), ()->outstanding.stream().forEach(System.out::println));
                Runtime.getRuntime().gc();
            }, 0, 100, TimeUnit.MILLISECONDS);

            assertThat(outstanding.current().isPresent()).isEqualTo(false);
            assertThat(queue.remove(20000)).isEqualTo(ref);
        } finally {
            executorService.shutdown();
        }
    }

    @Test(expected = IOException.class)
    public void doInTransactionCheckThrowsException() throws IOException {
        outstanding.doInTransactionChecked(null, () -> {
            throw new IOException();
        });
    }

    @Test
    public void putContextToCurrent() {
        outstanding.createTicket(payload);
        StructuredArgument argument = keyValue(SOME_KEY, SOME_VALUE);

        assertThat(outstanding.getCurrentMetadata()).doesNotContain(argument);

        String result = outstanding.putInContext(SOME_KEY, SOME_VALUE);
        assertThat(outstanding.getCurrentMetadata()).contains(argument);
        assertThat(result).isEqualTo(SOME_VALUE);
    }

    private void assertCurrentThread(OutstandingWork<MockWork> outstanding, MockWork work) {
        assertThat(outstanding.getCurrentMetadata()).isEqualTo(work.getMetadata());
    }

    private class MockWork extends Work {

    }
}
