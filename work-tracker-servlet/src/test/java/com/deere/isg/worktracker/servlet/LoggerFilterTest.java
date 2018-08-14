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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LoggerFilterTest {
    @Mock
    private WorkLogger logger;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @Mock
    private OutstandingWork<HttpWork> outstanding;

    private LoggerFilter filter;

    @Before
    public void setUp() {
        Clock.freeze();

        filter = new LoggerFilter();
        filter.setOutstanding(outstanding);
        filter.setLogger(logger);
    }

    @After
    public void tearDown() {
        Clock.clear();
    }

    @Test
    public void logsStartInfo() throws IOException, ServletException {
        final HttpWork work = TestWorkUtils.createWork();

        when(outstanding.current()).thenReturn(Optional.of(work));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(logger).logStart(request, work);
    }

    @Test
    public void doesNotLogIfNoOutstanding() throws IOException, ServletException {
        filter.setOutstanding(null);
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(logger, never()).logStart(eq(request), any());
    }
}
