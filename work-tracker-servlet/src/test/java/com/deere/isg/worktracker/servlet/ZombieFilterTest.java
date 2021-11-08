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

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.ZombieDetector;
import com.deere.isg.worktracker.ZombieError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.deere.isg.worktracker.servlet.WorkContextListener.*;
import static javax.servlet.http.HttpServletResponse.SC_GATEWAY_TIMEOUT;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ZombieFilterTest {
    private static final String MESSAGE = "This is a message";

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;
    @Mock
    private FilterConfig config;

    @Mock
    private OutstandingWork<?> outstanding;
    @Mock
    private HttpFloodSensor<?> floodSensor;
    @Mock
    private ZombieDetector detector;

    private ZombieFilter filter;

    @Before
    public void setUp() throws ServletException {
        when(config.getServletContext()).thenReturn(mock(ServletContext.class));
        when(config.getServletContext().getAttribute(OUTSTANDING_ATTR)).thenReturn(outstanding);
        when(config.getServletContext().getAttribute(FLOOD_SENSOR_ATTR)).thenReturn(floodSensor);
        when(config.getServletContext().getAttribute(ZOMBIE_ATTR)).thenReturn(detector);

        filter = new ZombieFilter();
        filter.init(config);

    }

    @After
    public void tearDown() {
        filter.destroy();
    }

    @Test
    public void filterInitStartZombie() {
        verify(detector).start();
    }

    @Test
    public void filterDestroyEndsZombie() {
        filter.destroy();

        verify(detector).close();
    }

    @Test
    public void zombieIsRunningDuringFilter() throws IOException, ServletException {
        verify(detector).start();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    public void zombieErrorIsLogged() throws IOException, ServletException {
        ZombieError error = new ZombieError(MESSAGE);

        filter.doFilter(request, response, (req, resp) -> {
            throw error;
        });

        verify(response).sendError(SC_GATEWAY_TIMEOUT, MESSAGE);
    }
}
