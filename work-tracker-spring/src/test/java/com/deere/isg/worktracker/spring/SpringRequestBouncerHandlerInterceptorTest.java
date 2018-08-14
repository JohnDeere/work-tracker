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


package com.deere.isg.worktracker.spring;

import com.deere.clock.Clock;
import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.servlet.ConnectionLimits;
import com.deere.isg.worktracker.servlet.HttpFloodSensor;
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

import static com.deere.isg.worktracker.servlet.WorkContextListener.FLOOD_SENSOR_ATTR;
import static com.deere.isg.worktracker.spring.TestWorkUtils.SIZE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SpringRequestBouncerHandlerInterceptorTest {

    @Mock
    private OutstandingWork<SpringWork> outstanding;
    @Mock
    private Object handler;
    @Mock
    private ServletContext context;

    private HttpServletRequest request;
    private HttpServletResponse response;

    private HttpFloodSensor<SpringWork> floodSensor;
    private SpringRequestBouncerHandlerInterceptor handlerInterceptor;
    private ConnectionLimits<SpringWork> limit;

    @Before
    public void setUp() {
        Clock.freeze();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        limit = new ConnectionLimits<>();
        floodSensor = new HttpFloodSensor<>(outstanding, limit);
        when(context.getAttribute(FLOOD_SENSOR_ATTR)).thenReturn(floodSensor);

        handlerInterceptor = new SpringRequestBouncerHandlerInterceptor();
        handlerInterceptor.setServletContext(context);

        when(outstanding.stream()).thenAnswer(invocationOnMock -> TestWorkUtils.getWorkList(SIZE).stream());
        when(outstanding.current()).thenReturn(Optional.of(TestWorkUtils.createWork()));
    }

    @After
    public void tearDown() {
        Clock.clear();
    }

    @Test
    public void preHandleIsTrueIfMayProceedSameUser() throws Exception {
        limit.addConnectionLimit(SIZE, ConnectionLimits.USER).method(SpringWork::getRemoteUser);

        assertThat(handlerInterceptor.preHandle(request, response, handler), is(true));
    }

    @Test
    public void preHandleIsFalseIfMayNotProceedSameUser() throws Exception {
        limit.addConnectionLimit(SIZE - 1, ConnectionLimits.USER).method(SpringWork::getRemoteUser);

        assertThat(handlerInterceptor.preHandle(request, response, handler), is(false));
    }

    @Test
    public void preHandleIsTrueIfMayProceedSameService() throws Exception {
        limit.addConnectionLimit(SIZE, ConnectionLimits.SERVICE).method(SpringWork::getService);

        assertThat(handlerInterceptor.preHandle(request, response, handler), is(true));
    }

    @Test
    public void preHandleIsFalseIfMayNotProceedSameService() throws Exception {
        limit.addConnectionLimit(SIZE - 1, ConnectionLimits.SERVICE).method(SpringWork::getService);

        assertThat(handlerInterceptor.preHandle(request, response, handler), is(false));
    }

    @Test
    public void preHandleIsTrueIfMayProceedSameSession() throws Exception {
        limit.addConnectionLimit(SIZE, ConnectionLimits.SESSION).method(SpringWork::getSessionId);

        assertThat(handlerInterceptor.preHandle(request, response, handler), is(true));
    }

    @Test
    public void preHandleIsFalseIfMayNotProceedSameSession() throws Exception {
        limit.addConnectionLimit(SIZE - 1, ConnectionLimits.SESSION).method(SpringWork::getSessionId);

        assertThat(handlerInterceptor.preHandle(request, response, handler), is(false));
    }

    @Test
    public void preHandleIsTrueIfMayProceedTooManyTotal() throws Exception {
        limit.addConnectionLimit(SIZE, ConnectionLimits.TOTAL).test(x -> true);

        assertThat(handlerInterceptor.preHandle(request, response, handler), is(true));
    }

    @Test
    public void preHandleIsFalseIfMayNotProceedTooManyTotal() throws Exception {
        limit.addConnectionLimit(SIZE - 1, ConnectionLimits.TOTAL).test(x -> true);

        assertThat(handlerInterceptor.preHandle(request, response, handler), is(false));
    }

    @Test
    public void nullFloodSensorReturnsTruePreHandle() throws Exception {
        when(context.getAttribute(FLOOD_SENSOR_ATTR)).thenReturn(null);
        handlerInterceptor.setServletContext(context);

        assertThat(handlerInterceptor.preHandle(request, response, handler), is(true));
    }
}
