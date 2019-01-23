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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.deere.isg.worktracker.servlet.TestWorkUtils.createAllConditionsList;
import static com.deere.isg.worktracker.servlet.TestWorkUtils.createWorkList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HttpFloodSensorTest {
    private static final int SC_TOO_MANY_REQUESTS = 429;
    private static final String TEST_USER = "test_user";
    private static final String TEST_SESSION = "test_session";
    private static final String TEST_SERVICE = "test/this/path";
    private static final String SERVICE = "service";
    private static final String USER = "user";
    private static final String SESSION = "session";
    private static final String TOTAL = "total";
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    @Mock
    private OutstandingWork<HttpWork> outstanding;
    @Mock
    private Logger logger;

    private HttpFloodSensor<HttpWork> floodSensor;
    private HttpServletResponse response;
    private ConnectionLimits<HttpWork> limit;

    @Before
    public void setUp() {
        limit = new ConnectionLimits<>();
        floodSensor = new HttpFloodSensor<>(outstanding, limit);
        floodSensor.setLogger(logger);
        response = mock(HttpServletResponse.class);
    }

    @Test
    public void servletResponseMayProceed() {
        ServletResponse servletResp = mock(ServletResponse.class);

        assertThat(floodSensor.mayProceedOrRedirectTooManyRequest(servletResp), is(true));
    }

    @Test
    public void canProceedIfExactTooManyTotalLimit() {
        setCurrentStream(new HttpWork(null), createWorkList(limit.getConnectionLimit(ConnectionLimits.TOTAL).getLimit()));

        assertThat(floodSensor.mayProceedOrRedirectTooManyRequest(response), is(true));
    }

    @Test
    public void cannotProceedIfExceedTooManyTotalLimit() {
        setCurrentStream(new HttpWork(null), createWorkList(limit.getConnectionLimit(ConnectionLimits.TOTAL).getLimit() + 1));

        assertThat(floodSensor.mayProceedOrRedirectTooManyRequest(response), is(false));
    }

    @Test
    public void canProceedIfUnderTooManyTotalLimit() {
        setCurrentStream(new HttpWork(null), createWorkList(limit.getConnectionLimit(ConnectionLimits.TOTAL).getLimit() - 1));

        assertThat(floodSensor.mayProceedOrRedirectTooManyRequest(response), is(true));
    }

    @Test
    public void canProceedIfExactTooManyUserLimit() {
        HttpWork work = setSameUserStream(limit.getConnectionLimit(ConnectionLimits.USER).getLimit());

        assertLimits(work, true, TOTAL, USER);
    }

    @Test
    public void cannotProceedIfExceedTooManyUserLimit() {
        HttpWork work = setSameUserStream(limit.getConnectionLimit(ConnectionLimits.USER).getLimit() + 1);

        assertLimits(work, false, USER);
    }

    @Test
    public void canProceedIfUnderTooManyUserLimit() {
        HttpWork work = setSameUserStream(limit.getConnectionLimit(ConnectionLimits.USER).getLimit() - 1);

        assertLimits(work, true, TOTAL, USER);
    }

    @Test
    public void canProceedIfExactTooManySessionLimit() {
        HttpWork work = setSameSessionStream(limit.getConnectionLimit(ConnectionLimits.SESSION).getLimit());

        assertLimits(work, true, TOTAL, SESSION);
    }

    @Test
    public void cannotProceedIfExceedTooManySessionLimit() {
        HttpWork work = setSameSessionStream(limit.getConnectionLimit(ConnectionLimits.SESSION).getLimit() + 1);

        assertLimits(work, false, SESSION);
    }

    @Test
    public void canProceedIfUnderTooManySessionLimit() {
        HttpWork work = setSameSessionStream(limit.getConnectionLimit(ConnectionLimits.SESSION).getLimit() - 1);

        assertLimits(work, true, TOTAL, SESSION);
    }

    @Test
    public void canProceedIfExactTooManyServiceLimit() {
        HttpWork work = setSameServiceStream(limit.getConnectionLimit(ConnectionLimits.SERVICE).getLimit());

        assertLimits(work, true, TOTAL, SERVICE);
    }

    @Test
    public void cannotProceedIfExceedTooManyServiceLimit() {
        HttpWork work = setSameServiceStream(limit.getConnectionLimit(ConnectionLimits.SERVICE).getLimit() + 1);

        assertLimits(work, false, SERVICE);
    }

    @Test
    public void canProceedIfUnderTooManyServiceLimit() {
        HttpWork work = setSameServiceStream(limit.getConnectionLimit(ConnectionLimits.SERVICE).getLimit() - 1);

        assertLimits(work, true, SERVICE, TOTAL);
    }

    @Test
    public void sameAll() {
        HttpWork work = setSameWorkStream(limit.getConnectionLimit(ConnectionLimits.TOTAL).getLimit() + 1, TEST_SERVICE, TEST_SESSION, TEST_USER);

        assertLimits(work, false, SESSION);
    }

    @Test
    public void sameServiceSameUser() {
        HttpWork work = setSameWorkStream(limit.getConnectionLimit(ConnectionLimits.SERVICE).getLimit() - 1, TEST_SERVICE, null, TEST_USER);

        assertLimits(work, false, USER);
    }

    @Test
    public void sameSessionSameUser() {
        HttpWork work = setSameWorkStream(limit.getConnectionLimit(ConnectionLimits.SESSION).getLimit() + 1, null, TEST_SESSION, TEST_USER);

        assertLimits(work, false, SESSION);
    }

    @Test
    public void sameSessionSameService() {
        HttpWork work = setSameWorkStream(limit.getConnectionLimit(ConnectionLimits.SERVICE).getLimit() - 1, TEST_SERVICE, TEST_SESSION, null);

        assertLimits(work, false, SESSION);
    }

    @Test
    public void notRejectedIfAlreadyPassedServiceLimit() {
        HttpWork work = setSameWorkStream(limit.getConnectionLimit(ConnectionLimits.SERVICE).getLimit() - 10, TEST_SERVICE, null, TEST_USER);

        assertLimits(work, true, SERVICE, USER, TOTAL);
        assertLimitsNotTriggered(work, SERVICE, USER, TOTAL);
    }

    @Test
    public void notRejectedIfAlreadyPassedSessionLimit() {
        HttpWork work = setSameWorkStream(limit.getConnectionLimit(ConnectionLimits.SESSION).getLimit() - 10, null, TEST_SESSION, TEST_USER);

        assertLimits(work, true, SESSION, USER, TOTAL);
        assertLimitsNotTriggered(work, SESSION, USER, TOTAL);
    }

    @Test
    public void notRejectedIfAlreadyPassedUserLimit() {
        HttpWork work = setSameWorkStream(limit.getConnectionLimit(ConnectionLimits.USER).getLimit() - 10, null, null, TEST_USER);

        assertLimits(work, true, USER, TOTAL);
        assertLimitsNotTriggered(work, USER, TOTAL);
    }

    @Test
    public void notRejectedIfAlreadyPassedTotalLimit() {
        HttpWork work = setSameWorkStream(10, TEST_SERVICE, TEST_SESSION, TEST_USER);

        assertLimits(work, true, SERVICE, SESSION, USER, TOTAL);
        assertLimitsNotTriggered(work, SERVICE, SESSION, USER, TOTAL);

    }

    @Test
    public void rejectedWhenUserIsKnownIfAlreadyExceeds() {
        HttpWork work = setSameWorkStream(10, TEST_SERVICE, null, null);
        assertLimits(work, true, SERVICE, TOTAL);

        setSameWorkStream(limit.getConnectionLimit(ConnectionLimits.TOTAL).getLimit() + 1, work);
        assertLimits(work, true, SERVICE, TOTAL);

        work.setRemoteUser(TEST_USER);
        setSameWorkStream(limit.getConnectionLimit(ConnectionLimits.TOTAL).getLimit() + 1, work);
        assertLimits(work, false, SERVICE, USER, TOTAL);
    }

    @Test
    public void rejectedWhenServiceIsKnownIfAlreadyExceeds() {
        HttpWork work = setSameWorkStream(10, null, null, TEST_USER);
        assertLimits(work, true, USER, TOTAL);

        setSameWorkStream(limit.getConnectionLimit(ConnectionLimits.TOTAL).getLimit() + 1, work);
        assertLimits(work, true, USER, TOTAL);

        work.setService(TEST_SERVICE);
        setSameWorkStream(limit.getConnectionLimit(ConnectionLimits.TOTAL).getLimit() + 1, work);
        assertLimits(work, false, USER, SERVICE, TOTAL);
    }

    @Test
    public void defaultsToPredicateIfFunctionIsNull() {
        try {
            HttpWork work = setSameUserStream(limit.getConnectionLimit(ConnectionLimits.TOTAL).getLimit());
            floodSensor.shouldRetryLater(work, limit.getConnectionLimit(ConnectionLimits.TOTAL));
        } catch (NullPointerException e) {
            fail("should default to predicate");
        }
    }

    @Test
    public void mayProceedIsTrueIfCheckLimitsIsEmpty() {
        MockEmptyFloodSensor eFloodSensor = new MockEmptyFloodSensor(outstanding);

        HttpServletResponse response = mock(HttpServletResponse.class);

        boolean proceed = eFloodSensor.mayProceedOrRedirectTooManyRequest(response);

        assertThat(proceed, is(true));
    }

    @Test
    public void mayProceedIsFalseIfCheckLimitsHasInteger() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        Optional<HttpWork> work = Optional.of(new HttpWork(null));
        when(outstanding.current()).thenReturn(work);

        MockFloodSensor mockFloodSensor = new MockFloodSensor(outstanding);
        boolean proceed = mockFloodSensor.mayProceedOrRedirectTooManyRequest(response);

        assertThat(proceed, is(false));
    }

    @Test
    public void responseIsRedirectedIfCheckLimitsHasInteger() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        Optional<HttpWork> work = Optional.of(new HttpWork(null));
        when(outstanding.current()).thenReturn(work);

        MockFloodSensor mockFloodSensor = new MockFloodSensor(outstanding);
        mockFloodSensor.mayProceedOrRedirectTooManyRequest(response);

        verify(response).setStatus(SC_TOO_MANY_REQUESTS);
        verify(response).setHeader(RETRY_AFTER_HEADER, "5");
    }

    @Test
    public void responseNotRedirectedIfCheckLimitsIsEmpty() {
        MockEmptyFloodSensor eFloodSensor = new MockEmptyFloodSensor(outstanding);

        HttpServletResponse response = mock(HttpServletResponse.class);

        eFloodSensor.mayProceedOrRedirectTooManyRequest(response);

        verify(response, never()).setStatus(SC_TOO_MANY_REQUESTS);
        verify(response, never()).setHeader(RETRY_AFTER_HEADER, "5");
    }

    private void assertLimitsNotTriggered(HttpWork work, String... limits) {
        setSameWorkStream(limit.getConnectionLimit(ConnectionLimits.USER).getLimit() + 1, work);
        assertLimits(work, true, limits);

        setSameWorkStream(limit.getConnectionLimit(ConnectionLimits.SESSION).getLimit() + 1, work);
        assertLimits(work, true, limits);

        setSameWorkStream(limit.getConnectionLimit(ConnectionLimits.SERVICE).getLimit() + 1, work);
        assertLimits(work, true, limits);

        setSameWorkStream(limit.getConnectionLimit(ConnectionLimits.TOTAL).getLimit() + 1, work);
        assertLimits(work, true, limits);
    }

    private void assertLimits(HttpWork work, boolean mayProceed, String... items) {
        assertThat(floodSensor.mayProceedOrRedirectTooManyRequest(response), is(mayProceed));
        assertThat(work.getLimits(), hasItems(items));
        assertThat(work.getLimits(), hasSize(items.length));
    }

    private HttpWork setSameUserStream(int limit) {
        return setSameWorkStream(limit, null, null, TEST_USER);
    }

    private HttpWork setSameSessionStream(int limit) {
        return setSameWorkStream(limit, null, TEST_SESSION, null);
    }

    private HttpWork setSameServiceStream(int limit) {
        return setSameWorkStream(limit, TEST_SERVICE, null, null);
    }

    private HttpWork setSameWorkStream(int limit, String service, String session, String user) {
        HttpWork work = new HttpWork(null);
        work.setSessionId(session);
        work.setService(service);
        work.setRemoteUser(user);
        return setSameWorkStream(limit, work);
    }

    private HttpWork setSameWorkStream(int limit, HttpWork work) {
        setCurrentStream(work, createAllConditionsList(limit, work.getService(), work.getSessionId(), work.getRemoteUser()));
        return work;
    }

    private void setCurrentStream(HttpWork work, List<HttpWork> workList) {
        when(outstanding.current()).thenReturn(Optional.of(work));
        when(outstanding.stream()).thenAnswer(invocation -> workList.stream());
    }

    private class MockEmptyFloodSensor extends HttpFloodSensor {
        MockEmptyFloodSensor(OutstandingWork<HttpWork> outstanding) {
            super(outstanding);
        }

        @Override
        protected Stream<Function<HttpWork, Optional<Integer>>> checkLimits() {
            return Stream.of(i -> Optional.empty());
        }
    }

    private class MockFloodSensor extends HttpFloodSensor<HttpWork> {
        MockFloodSensor(OutstandingWork<HttpWork> outstanding) {
            super(outstanding);
        }

        @Override
        protected Stream<Function<HttpWork, Optional<Integer>>> checkLimits() {
            return Stream.of(i -> Optional.of(5));
        }
    }
}
