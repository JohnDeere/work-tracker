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

package com.deere.isg.worktracker.servlet;

import com.deere.clock.Clock;
import com.deere.isg.worktracker.OutstandingWork;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.MDC;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.deere.isg.worktracker.servlet.TestWorkUtils.createWork;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AbstractHttpWorkFilterTest {
    private static final HttpWork TEST_WORK = createWork();
    private static final String POST_PROCESSED_DATA = "postProcessedData";
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private WorkLogger logger;
    private MockHttpWorkFilter filter;
    private OutstandingWork<HttpWork> outstanding;

    @Before
    public void setUp() {
        Clock.freeze();

        outstanding = new OutstandingWork<>();

        filter = new MockHttpWorkFilter();
        filter.setLogger(logger);
    }

    @After
    public void tearDown() {
        Clock.clear();
    }

    @Test
    public void doFilterExecutesChainFilter() throws IOException, ServletException {
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);

        verify(logger).logStart(request, TEST_WORK);
        verify(logger).logEnd(request, response, TEST_WORK);
        assertThat(filter.getPostProcessedData()).isEqualTo(POST_PROCESSED_DATA);
    }

    @Test
    public void mdcIsCleared() throws IOException, ServletException {
        filter.doFilter(request, response, chain);

        verifyEmptyMDC();

        verify(logger).logStart(request, TEST_WORK);
        verify(logger).logEnd(request, response, TEST_WORK);
    }

    @Test
    public void exceptionsStillLogAndClearMDC() throws IOException, ServletException {
        try {
            filter.doFilter(request, response, (req, res) -> {
                throw new RuntimeException();
            });
        } catch (RuntimeException e) {
            verify(logger).logStart(request, TEST_WORK);
            verify(logger).logEnd(request, response, TEST_WORK);
            verifyEmptyMDC();
            assertThat(filter.getPostProcessedData()).isEqualTo(POST_PROCESSED_DATA);
        }
    }

    @Test
    public void chainIfOutstandingIsNull() throws IOException, ServletException {
        MockNullHttpWorkFilter nullFilter = new MockNullHttpWorkFilter();

        nullFilter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);

        verify(logger, never()).logStart(request, TEST_WORK);
        verify(logger, never()).logEnd(request, response, TEST_WORK);
        assertThat(nullFilter.getPostProcessedData()).isEqualTo(POST_PROCESSED_DATA);
    }

    @Test
    public void doesNotLogIfExcludedUrl() throws IOException, ServletException {
        WorkLogger workLogger = WorkLogger.getLogger();
        Logger mockLogger = mock(Logger.class);
        workLogger.setLogger(mockLogger);
        workLogger.excludeUrls("/tests/");
        filter.setLogger(workLogger);

        when(request.getRequestURI()).thenReturn("/tests/something");
        filter.doFilter(request, response, chain);

        verify(mockLogger, never()).info(anyString(), any(Object[].class));
    }

    @Test
    public void postFilterExceptionsStillLogAndClearMDC() throws IOException, ServletException {
        filter = new PostProcessExceptionThrowingMockHttpWorkFilter();
        filter.setLogger(logger);
        try {
            filter.doFilter(request, response, chain);
        } catch (RuntimeException e) {
            verify(logger).logStart(request, TEST_WORK);
            verify(logger).logEnd(request, response, TEST_WORK);
            verifyEmptyMDC();
            assertThat(filter.getPostProcessedData()).isEqualTo("");
        }
    }

    private void verifyEmptyMDC() {
        assertThat(MDC.getCopyOfContextMap()).isNull();
    }

    private class MockHttpWorkFilter extends AbstractHttpWorkFilter<HttpWork> {
        private String postProcessedData = "";

        MockHttpWorkFilter() {
            setLogger(logger);
        }
        @Override
        public OutstandingWork<HttpWork> getOutstanding() {
            return outstanding;
        }

        @Override
        protected HttpWork createWork(ServletRequest request) {
            return TEST_WORK;
        }

        @Override
        protected void postProcess(ServletRequest request, ServletResponse response, HttpWork payload) {
            postProcessedData = POST_PROCESSED_DATA;
        }

        public String getPostProcessedData() {
            return postProcessedData;
        }

    }

    private class MockNullHttpWorkFilter extends MockHttpWorkFilter {
        MockNullHttpWorkFilter() {
        }
        @Override
        public OutstandingWork<HttpWork> getOutstanding() {
            return null;
        }

    }

    private class PostProcessExceptionThrowingMockHttpWorkFilter extends MockHttpWorkFilter {
        @Override
        protected void postProcess(ServletRequest request, ServletResponse response, HttpWork payload) {
            throw new RuntimeException();
        }
    }
}
