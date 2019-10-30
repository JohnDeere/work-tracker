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


package com.deere.isg.worktracker.spring;

import com.deere.clock.Clock;
import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.OutstandingWorkTracker;
import com.deere.isg.worktracker.servlet.WorkLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.deere.isg.worktracker.servlet.HttpWork.PATH;
import static com.deere.isg.worktracker.spring.SpringWork.ENDPOINT;
import static com.deere.isg.worktracker.spring.TestServletContext.ENDPOINT_1;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;

@RunWith(MockitoJUnitRunner.class)
public class AbstractSpringWorkFilterTest {
    private static final String USER = "user";

    @Mock
    private OutstandingWork<?> outstanding;
    @Mock
    private WorkLogger workLogger;
    @Mock
    private FilterChain chain;
    private MockSpringWorkFilter filter;
    private HttpServletResponse response;
    private MockHttpServletRequest request;
    private OutstandingWork<SpringWork> outstandingWork;

    @Before
    public void setUp() {
        Clock.freeze();

        outstandingWork = new OutstandingWork<>();
        filter = new MockSpringWorkFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @After
    public void tearDown() {
        Clock.clear();
        ServletEndpointRegistry.clear();
    }

    @Test
    public void hasServletEndpointsInRegistry() throws Exception {
        TestServletContext sc = new TestServletContext();
        FilterConfig config = mock(FilterConfig.class);
        when(config.getServletContext()).thenReturn(sc);
        request.setRequestURI(ENDPOINT_1);
        request.setServletPath(ENDPOINT_1);
        filter.init(config);
        filter.setOutstanding(outstandingWork);
        assertThat(ServletEndpointRegistry.contains(ENDPOINT_1), is(true));

        filter.doFilter(request, response, chain);
        verify(chain).doFilter(ArgumentMatchers.any(HttpServletRequest.class), eq(response));
        verify(workLogger).logStart(ArgumentMatchers.any(HttpServletRequest.class), eq(filter.getSpringWork()));
    }

    @Test
    public void requestIsUpdatedWithSpringHandling() throws IOException, ServletException {

        request.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, getAttributesMap());
        filter.doFilter(request, response, (request, response) -> {
            assertThat(MDC.get(PATH), is("GET /"));
            assertThat(MDC.get(ENDPOINT), is("GET /"));
        });

        assertThat(MDC.getCopyOfContextMap(), nullValue());
        verify(workLogger, never()).logStart(ArgumentMatchers.any(HttpServletRequest.class), eq(filter.getSpringWork()));
    }

    @Test
    public void backwardCompatibilityNoSpringConfigurationProvidesDefaultKeyCleanser() {
        KeyCleanser keyCleanser = filter.getKeyCleanser();

        assertThat(keyCleanser, notNullValue());
        assertThat(keyCleanser, instanceOf(PathMetadataCleanser.class));
    }

    private Map<String, String> getAttributesMap() {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put(USER, "some user");
        return keyValue;
    }

    private class MockSpringWorkFilter extends AbstractSpringWorkFilter<SpringWork> {

        private SpringWork springWork;

        public MockSpringWorkFilter() {
            setLogger(workLogger);
            setOutstanding(outstanding);
        }

        @Override
        protected SpringWork createWork(ServletRequest request) {
            springWork = new SpringWork(request);
            return springWork;
        }

        public SpringWork getSpringWork() {
            return springWork;
        }

        @Override
        protected void setOutstanding(OutstandingWorkTracker<?> outstanding) {
            super.setOutstanding(outstanding);
        }
    }
}
