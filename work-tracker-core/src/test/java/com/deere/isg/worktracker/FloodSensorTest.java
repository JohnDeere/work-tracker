/**
 * Copyright 2018 Deere & Company
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.deere.isg.worktracker.MockWorkUtils.createMockWorkList;
import static com.deere.isg.worktracker.MockWorkUtils.createSameUserMockWork;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FloodSensorTest {
    private static final int LIMIT_EQUAL = 10;
    private static final int LIMIT_OVER = 20;
    private static final int LIMIT_UNDER = 5;
    private static final String TEST_USER = "test_user";
    private static final String USER = "user";
    private static final String MESSAGE = "some message";
    @Mock
    private OutstandingWork<MockWork> outstanding;
    @Mock
    private Logger logger;
    private MockFloodSensor floodSensor;

    @Before
    public void setUp() {
        floodSensor = new MockFloodSensor(outstanding);
        floodSensor.setLogger(logger);

        setStream(createMockWorkList(LIMIT_EQUAL));
    }

    @Test
    public void shouldRetryLaterReturnEmptyOpIfCheckIsFalse() {
        MockWork work = new MockWork(null);
        Optional<Integer> retryAfter = floodSensor.shouldRetryLater(work, predicate(false), LIMIT_EQUAL, USER, MESSAGE);

        assertNoRetry(MESSAGE, retryAfter);
    }

    @Test
    public void shouldRetryLaterReturnEmptyOpIfEqualsLimit() {
        MockWork work = new MockWork(null);
        Optional<Integer> retryAfter = floodSensor.shouldRetryLater(work, predicate(true), LIMIT_EQUAL, USER, MESSAGE);

        assertNoRetry(MESSAGE, retryAfter);
    }

    @Test
    public void shouldRetryLaterReturnIntegerOpIfExceedsLimit() {
        MockWork work = new MockWork(null);
        floodSensor.shouldRetryLater(work, predicate(true), LIMIT_UNDER, USER, MESSAGE);

        verify(logger).warn(eq(MESSAGE), (Object[]) any());
    }

    @Test
    public void shouldRetryLaterReturnEmptyOpIfNotExceedsLimit() {
        MockWork work = new MockWork(null);
        Optional<Integer> retryAfter = floodSensor.shouldRetryLater(work, predicate(true), LIMIT_OVER, USER, MESSAGE);

        assertNoRetry(MESSAGE, retryAfter);
    }

    @Test
    public void workRetryLaterReturnsEmptyOpIfEqualsConditionalLimit() {
        setSameUserStream();

        Optional<Integer> retryAfter = floodSensor
                .shouldRetryLater(new MockWork(TEST_USER), MockWork::getUser, LIMIT_EQUAL, USER, MESSAGE);

        assertNoRetry(MESSAGE, retryAfter);
    }

    @Test
    public void workRetryLaterReturnsIntegerOpIfExceedsConditionalLimit() {
        setSameUserStream();

        floodSensor.shouldRetryLater(new MockWork(TEST_USER), MockWork::getUser, LIMIT_UNDER, USER, MESSAGE);

        verify(logger).warn(eq(MESSAGE), (Object[]) any());
    }

    @Test
    public void workRetryLaterReturnsEmptyOpIfNotExceedsConditionalLimit() {
        setSameUserStream();

        Optional<Integer> retryAfter = floodSensor
                .shouldRetryLater(new MockWork(TEST_USER), MockWork::getUser, LIMIT_OVER, USER, MESSAGE);

        assertNoRetry(MESSAGE, retryAfter);
    }

    @Test
    public void shouldRetryReturnsEmptyIfLimitAlreadyInSet() {
        setSameUserStream();

        MockWork work = new MockWork(TEST_USER);
        boolean addToSetIfNotFound = work.checkLimit(USER);
        assertThat(addToSetIfNotFound, is(false));

        Optional<Integer> retryAfter = floodSensor
                .shouldRetryLater(work, MockWork::getUser, LIMIT_UNDER, USER, MESSAGE);

        boolean isInSet = work.checkLimit(USER);
        assertThat(isInSet, is(true));
        assertNoRetry(MESSAGE, retryAfter);
    }

    @Test
    public void shouldRetryReturnsEmptyIfAttributeIsNull() {
        setStream(createSameUserMockWork(LIMIT_EQUAL, null));

        MockWork work = new MockWork(null);
        Optional<Integer> retryAfter = floodSensor
                .shouldRetryLater(work, MockWork::getUser, LIMIT_UNDER, USER, MESSAGE);

        assertThat(work.checkLimit(USER), is(false));
        assertNoRetry(MESSAGE, retryAfter);
    }

    @Test
    public void shouldRetryReturnsEmptyIfAttributeIsEmpty() {
        setStream(createSameUserMockWork(LIMIT_EQUAL, ""));

        MockWork work = new MockWork("");
        Optional<Integer> retryAfter = floodSensor
                .shouldRetryLater(work, MockWork::getUser, LIMIT_UNDER, USER, MESSAGE);

        assertThat(work.checkLimit(USER), is(false));
        assertNoRetry(MESSAGE, retryAfter);
    }

    private void assertNoRetry(String message, Optional<Integer> retryAfter) {
        assertThat(retryAfter, is(Optional.empty()));
        verify(logger, never()).warn(eq(message), (Object[]) any());
    }

    private void setStream(List<MockWork> workList) {
        when(outstanding.stream()).thenAnswer(invocation -> workList.stream());
    }

    private void setSameUserStream() {
        setStream(createSameUserMockWork(LIMIT_EQUAL, TEST_USER));
    }

    private Predicate<MockWork> predicate(boolean value) {
        return x -> value;
    }

    private class MockFloodSensor extends FloodSensor<MockWork> {
        MockFloodSensor(OutstandingWork<MockWork> outstanding) {
            super(outstanding);
        }

        @Override
        protected Stream<Function<MockWork, Optional<Integer>>> checkLimits() {
            return Stream.of(i -> Optional.of(5));
        }
    }
}
