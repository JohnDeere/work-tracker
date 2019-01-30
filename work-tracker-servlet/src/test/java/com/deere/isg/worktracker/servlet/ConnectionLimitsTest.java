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


package com.deere.isg.worktracker.servlet;

import com.deere.isg.worktracker.OutstandingWork;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.mockito.Mockito.*;

public class ConnectionLimitsTest {
    private static final String TOTAL_MESSAGE = "Request rejected to protect JVM from too many requests total";
    private static final String USER_MESSAGE = "Request rejected to protect JVM from too many requests from same user";
    private static final String TYPE_NAME = "typeName";
    private ConnectionLimits<HttpWork> connectionLimits;

    @Before
    public void setUp() {
        connectionLimits = new ConnectionLimits<>();
    }

    @Test
    public void createsDefaultOnlyIfTrue() {
        ConnectionLimits<HttpWork> connectionLimitsFalse = new ConnectionLimits<>(false);
        assertThat(connectionLimitsFalse.getConnectionLimits(), empty());

        ConnectionLimits<HttpWork> connectionLimitsTrue = new ConnectionLimits<>(true);
        assertThat(connectionLimitsTrue.getConnectionLimits(), hasSize(4));
    }

    @Test
    public void createsDefaultLimitsAndAssertOrder() {
        List<ConnectionLimits<HttpWork>.Limit> connections = connectionLimits.getConnectionLimits();

        assertThat(connections, hasSize(4));
        assertThat(connections, contains(
                hasProperty(TYPE_NAME, is("session")),
                hasProperty(TYPE_NAME, is("user")),
                hasProperty(TYPE_NAME, is("service")),
                hasProperty(TYPE_NAME, is("total"))
        ));
    }

    @Test
    public void correctDefaultLimits() {
        assertThat(connectionLimits.getConnectionLimit(ConnectionLimits.TOTAL).getLimit(), is((int) (60 * .9)));
        assertThat(connectionLimits.getConnectionLimit(ConnectionLimits.SERVICE).getLimit(), is((int) (60 * .6)));
        assertThat(connectionLimits.getConnectionLimit(ConnectionLimits.USER).getLimit(), is((int) (60 * .5)));
        assertThat(connectionLimits.getConnectionLimit(ConnectionLimits.SESSION).getLimit(), is((int) (60 * .4)));
    }

    @Test
    public void replacesLimit() {
        connectionLimits.addConnectionLimit(10, ConnectionLimits.TOTAL).test(x -> true);

        assertThat(connectionLimits.getConnectionLimit(ConnectionLimits.TOTAL).getLimit(), is(10));
    }

    @Test
    public void addsIfAbsent() {
        ConnectionLimits<HttpWork>.Limit test = connectionLimits.getConnectionLimit("test");
        assertThat(test, nullValue());

        connectionLimits.addConnectionLimit(10, "test").test(x -> true);

        ConnectionLimits<HttpWork>.Limit finalTest = connectionLimits.getConnectionLimit("test");
        assertThat(finalTest, notNullValue());
        assertThat(finalTest.getLimit(), is(10));
    }

    @Test
    public void returnsMessage() {
        String messageTotal = connectionLimits.getConnectionLimit(ConnectionLimits.TOTAL).getMessage();
        assertThat(messageTotal, is(TOTAL_MESSAGE));

        String messageType = connectionLimits.getConnectionLimit(ConnectionLimits.USER).getMessage();
        assertThat(messageType, is(USER_MESSAGE));
    }

    @Test(expected = AssertionError.class)
    public void cannotAddEmptyType() {
        connectionLimits.addConnectionLimit(10, "  ");
    }

    @Test(expected = AssertionError.class)
    public void cannotAddNullType() {
        connectionLimits.addConnectionLimit(10, null);
    }

    @Test
    public void updatesLimit() {
        ConnectionLimits<HttpWork>.Limit limit = connectionLimits.getConnectionLimit(ConnectionLimits.TOTAL);
        int initialLimit = limit.getLimit();
        assertThat(initialLimit, is((int) (60 * .9)));

        connectionLimits.updateLimit(10, ConnectionLimits.TOTAL);
        int finalLimit = limit.getLimit();
        assertThat(finalLimit, is(10));
    }

    @Test
    public void limitUpdatedOnlyIfPresent() {
        connectionLimits.updateLimit(10, "test");
        ConnectionLimits<HttpWork>.Limit test = connectionLimits.getConnectionLimit("test");

        assertThat(test, nullValue());
    }

    @Test
    public void helperReturnsLimit() {
        int limit = connectionLimits.getLimit(ConnectionLimits.TOTAL);
        assertThat(limit, is((int) (60 * .9)));
    }

    @Test
    public void canUseIncomingWorkToDefineTestFunction() {
        HttpWork incoming = new HttpWork(null);
        connectionLimits.addConnectionLimit(2, "test").buildTest(w->(x->(w==incoming)));
        ConnectionLimits<HttpWork>.Limit limit = connectionLimits.getConnectionLimit("test");

        HttpWork existing = new HttpWork(null);
        assertThat(limit.getPredicate(incoming).test(existing), is(true));
        assertThat(limit.getPredicate(null).test(existing), is(false));
        assertThat(limit.getPredicate(new HttpWork(null)).test(existing), is(false));

    }

    @Test
    public void canUseSimpleTestPredicate() {
        HttpWork incoming = new HttpWork(null);
        Predicate<HttpWork> expected = x -> true;
        connectionLimits.addConnectionLimit(2, "test").test(expected);
        ConnectionLimits<HttpWork>.Limit limit = connectionLimits.getConnectionLimit("test");

        assertThat(limit.getPredicate(incoming), is(expected));
        assertThat(limit.getPredicate(null), is(expected));
        assertThat(limit.getPredicate(new HttpWork(null)), is(expected));
    }

    @Test
    public void floodSensorPassesIncomingWorkToGetLimitTest() {

        HttpWork incoming = new HttpWork(null);
        ConnectionLimits<HttpWork>.Limit limit = mock(ConnectionLimits.Limit.class);
        when(limit.getPredicate(any())).thenReturn(x->true);

        OutstandingWork<HttpWork> outstandingWork = new OutstandingWork<>();
        HttpFloodSensor<HttpWork> sensor = new HttpFloodSensor<>(outstandingWork, connectionLimits);

        sensor.shouldRetryLater(incoming, limit);

        verify(limit).getPredicate(eq(incoming));
    }
}
