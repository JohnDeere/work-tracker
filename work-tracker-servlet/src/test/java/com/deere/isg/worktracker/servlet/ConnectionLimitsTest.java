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

import com.deere.isg.worktracker.FloodSensor;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class ConnectionLimitsTest {
    private static final String TOTAL_MESSAGE = "Request rejected to protect JVM from too many requests total";
    private static final String USER_MESSAGE = "Request rejected to protect JVM from too many requests from same user";
    private static final String TYPE_NAME = "typeName";
    public static final String USER_TYPE = "user";
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
                hasProperty(TYPE_NAME, is(USER_TYPE)),
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
        Function<HttpWork, Predicate<HttpWork>> builtTest = mock(Function.class);
        Predicate<HttpWork> expectedTest = w -> true;
        when(builtTest.apply(incoming)).thenReturn(expectedTest);
        HttpFloodSensor<HttpWork> sensor = mock(HttpFloodSensor.class);

        connectionLimits.addConnectionLimit(2, USER_TYPE).buildTest(builtTest);
        ConnectionLimits<HttpWork>.Limit limit = connectionLimits.getConnectionLimit(USER_TYPE);

        Optional<Integer> expectedrResult = Optional.of(1);
        when(sensor.shouldRetryLater(eq(incoming), eq(expectedTest), eq(2), eq(USER_TYPE), eq(USER_MESSAGE))).thenReturn(expectedrResult);

        Optional<Integer> result = limit.shouldRetryLater(sensor, incoming);

        verify(sensor).shouldRetryLater(eq(incoming), eq(expectedTest), eq(2), eq(USER_TYPE), eq(USER_MESSAGE));
        assertThat(result, is(expectedrResult));
    }

    @Test
    public void canUseSimpleTestPredicate() {
        HttpFloodSensor<HttpWork> sensor = mock(HttpFloodSensor.class);
        HttpWork incoming = new HttpWork(null);
        Predicate<HttpWork> expectedTest = x -> true;

        connectionLimits.addConnectionLimit(2, USER_TYPE).test(expectedTest);
        ConnectionLimits<HttpWork>.Limit limit = connectionLimits.getConnectionLimit(USER_TYPE);

        assertRunsPredicate(sensor, incoming, expectedTest, limit);
        assertRunsPredicate(sensor, null, expectedTest, limit);
        assertRunsPredicate(sensor, new HttpWork(null), expectedTest, limit);
    }

    private void assertRunsPredicate(HttpFloodSensor<HttpWork> sensor, HttpWork incoming, Predicate<HttpWork> expectedTest, ConnectionLimits<HttpWork>.Limit limit) {
        Optional<Integer> expectedrResult = Optional.of(1);
        when(sensor.shouldRetryLater(eq(incoming), eq(expectedTest), eq(2), eq(USER_TYPE), eq(USER_MESSAGE))).thenReturn(expectedrResult);
        Optional<Integer> result = limit.shouldRetryLater(sensor, incoming);
        verify(sensor).shouldRetryLater(eq(incoming), eq(expectedTest), eq(2), eq(USER_TYPE), eq(USER_MESSAGE));

        assertThat(result, is(expectedrResult));
    }

    @Test
    public void canUseSimpleTestMethod() {
        HttpFloodSensor<HttpWork> sensor = mock(HttpFloodSensor.class);
        HttpWork incoming = new HttpWork(null);
        Function<HttpWork, String> expectedMethod = HttpWork::getRemoteUser;

        connectionLimits.addConnectionLimit(2, USER_TYPE).method(expectedMethod);
        ConnectionLimits<HttpWork>.Limit limit = connectionLimits.getConnectionLimit(USER_TYPE);

        assertRunsMethod(sensor, incoming, expectedMethod, limit);
        assertRunsMethod(sensor, null, expectedMethod, limit);
        assertRunsMethod(sensor, new HttpWork(null), expectedMethod, limit);
    }

    private void assertRunsMethod(HttpFloodSensor<HttpWork> sensor, HttpWork incoming, Function<HttpWork, String> expectedMethod, ConnectionLimits<HttpWork>.Limit limit) {
        Optional<Integer> expectedrResult = Optional.of(1);
        when(sensor.shouldRetryLater(eq(incoming), eq(expectedMethod), eq(2), eq(USER_TYPE), eq(USER_MESSAGE))).thenReturn(expectedrResult);
        Optional<Integer> result = limit.shouldRetryLater(sensor, incoming);
        verify(sensor).shouldRetryLater(eq(incoming), eq(expectedMethod), eq(2), eq(USER_TYPE), eq(USER_MESSAGE));

        assertThat(result, is(expectedrResult));
    }

    @Test
    public void canSupplyAdvancedFunction() {
        Function<HttpWork, Optional<Integer>> advancedTest = mock(Function.class);
        connectionLimits.addConnectionLimit(2, USER_TYPE).advanced(advancedTest);
        HttpWork incoming = new HttpWork(null);
        Optional<Integer> expectedResult = Optional.of(1);
        HttpFloodSensor<HttpWork> sensor = mock(HttpFloodSensor.class);
        when(advancedTest.apply(eq(incoming))).thenReturn(expectedResult);

        assertAdvanced(sensor, incoming, expectedResult);

        verify(advancedTest).apply(incoming);
    }

    @Test
    public void canSupplyAdvancedBiFunction() {
        BiFunction<FloodSensor<HttpWork>, HttpWork, Optional<Integer>> advancedTest = mock(BiFunction.class);
        connectionLimits.addConnectionLimit(2, USER_TYPE).advanced(advancedTest);

        HttpWork incoming = new HttpWork(null);
        Optional<Integer> expectedResult = Optional.of(1);
        HttpFloodSensor<HttpWork> sensor = mock(HttpFloodSensor.class);
        when(advancedTest.apply(eq(sensor), eq(incoming))).thenReturn(expectedResult);

        assertAdvanced(sensor, incoming, expectedResult);
        verify(advancedTest).apply(sensor, incoming);
    }


    private void assertAdvanced(HttpFloodSensor<HttpWork> sensor, HttpWork incoming, Optional<Integer> expectedResult) {
        when(sensor.logFloodDetected(any(), any())).thenAnswer(a->a.getArgument(1));

        ConnectionLimits<HttpWork>.Limit limit = connectionLimits.getConnectionLimit(USER_TYPE);

        Optional<Integer> result = limit.shouldRetryLater(sensor, incoming);

        assertThat(result, is(expectedResult));
        verify(sensor).logFloodDetected(eq(limit), eq(result));
    }
}
