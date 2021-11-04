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

import com.deere.clock.Clock;
import net.logstash.logback.argument.StructuredArgument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.Optional;

import static net.logstash.logback.argument.StructuredArguments.keyValue;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

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

        assertThat(work.getMetadata(), is(payload.getMetadata()));
    }

    @Test
    public void nullDoesNotCreateTicket() {
        outstanding.createTicket(null);

        assertThat(outstanding.current(), is(Optional.empty()));
    }

    @Test
    public void getCurrentMetadataReturnsCurrentTicketsMetadata() {
        outstanding.createTicket(payload);

        assertThat(outstanding.getCurrentMetadata(), is(payload.getMetadata()));
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
                () -> assertThat(outstanding.current().orElseThrow(AssertionError::new), is(work)));

        assertThat(outstanding.current().isPresent(), is(false));
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

        assertThat(outstanding.getCurrentMetadata(), not(hasItem(argument)));

        String result = outstanding.putInContext(SOME_KEY, SOME_VALUE);
        assertThat(outstanding.getCurrentMetadata(), hasItem(argument));
        assertThat(result, is(SOME_VALUE));
    }

    private void assertCurrentThread(OutstandingWork<MockWork> outstanding, MockWork work) {
        assertThat(outstanding.getCurrentMetadata(), is(work.getMetadata()));
    }

    private class MockWork extends Work {

    }
}
