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


package com.deere.isg.worktracker.spring;

import com.deere.clock.Clock;
import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.servlet.WorkLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

import static com.deere.isg.worktracker.servlet.WorkContextListener.OUTSTANDING_ATTR;
import static com.deere.isg.worktracker.spring.TestWorkUtils.createWork;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SpringLoggerHandlerInterceptorTest {
    @Mock
    private OutstandingWork<SpringWork> outstanding;
    @Mock
    private ServletContext context;
    @Mock
    private WorkLogger logger;
    @Mock
    private Object handler;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private SpringLoggerHandlerInterceptor handlerInterceptor;
    private SpringWork springWork;

    @Before
    public void setUp() {
        springWork = createWork();

        Clock.freeze();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        when(outstanding.current()).thenReturn(Optional.of(springWork));
        when(context.getAttribute(OUTSTANDING_ATTR)).thenReturn(outstanding);

        handlerInterceptor = new SpringLoggerHandlerInterceptor();
        handlerInterceptor.setServletContext(context);
        handlerInterceptor.setLogger(logger);
    }

    @After
    public void tearDown() {
        Clock.clear();
    }

    @Test
    public void startLogging() throws Exception {
        handlerInterceptor.preHandle(request, response, handler);

        verify(logger).logStart(request, springWork);
    }

    @Test
    public void noLogForNullOutstanding() throws Exception {
        when(context.getAttribute(OUTSTANDING_ATTR)).thenReturn(null);
        handlerInterceptor.setServletContext(context);

        handlerInterceptor.preHandle(request, response, handler);

        verify(logger, never()).logStart(request, springWork);
    }
}
