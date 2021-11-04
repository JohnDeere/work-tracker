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


package com.deere.isg.worktracker.servlet;

import com.deere.clock.Clock;
import com.deere.isg.worktracker.OutstandingWork;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.deere.isg.worktracker.servlet.TestWorkUtils.createSameConditionWorkList;
import static com.deere.isg.worktracker.servlet.TestWorkUtils.createWorkList;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RequestBouncerFilterTest {
    private static final int SC_TOO_MANY_REQUESTS = 429;
    private static final int SIZE = 10;
    private static final List<HttpWork> WORK_LIST = createWorkList(SIZE);
    private static final String TEST_USER = "test_user";
    private static final String USER = "user";

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @Mock
    private OutstandingWork<HttpWork> outstanding;

    private RequestBouncerFilter filter;
    private HttpFloodSensor<HttpWork> floodSensor;

    @Before
    public void setUp() {
        Clock.freeze();
        floodSensor = new HttpFloodSensor<>(outstanding);

        filter = new RequestBouncerFilter();
        filter.setFloodSensor(floodSensor);

        when(outstanding.stream()).thenAnswer(invocationOnMock -> WORK_LIST.stream());
    }

    @After
    public void tearDown() {
        Clock.clear();
    }

    @Test
    public void notMayProceedRespondsTooManyRequests() throws IOException, ServletException {
        setSameUserStream();

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(request, response);
        verify(response).setStatus(SC_TOO_MANY_REQUESTS);
    }

    @Test
    public void mayProceedRunsChainFilter() throws IOException, ServletException {
        setCurrentUser();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    public void nullFloodSensorDoesChain() throws IOException, ServletException {
        filter.setFloodSensor(null);
        setCurrentUser();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    private void setSameUserStream() {
        when(outstanding.stream()).thenAnswer(invocationOnMock ->
                createSameConditionWorkList(floodSensor.getConnectionLimit(USER).getLimit() + 1, USER, TEST_USER).stream());
        setCurrentUser();
    }

    private void setCurrentUser() {
        HttpWork work = new HttpWork(null);
        work.setRemoteUser(TEST_USER);

        when(outstanding.current()).thenReturn(Optional.of(work));
    }

}
