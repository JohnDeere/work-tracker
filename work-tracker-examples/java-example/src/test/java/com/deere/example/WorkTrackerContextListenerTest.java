/**
 * Copyright 2018 Deere & Company
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.deere.example;

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.ZombieDetector;
import com.deere.isg.worktracker.servlet.HttpFloodSensor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import static com.deere.isg.worktracker.servlet.WorkContextListener.*;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WorkTrackerContextListenerTest {

    @Mock
    private ServletContextEvent contextEvent;

    private WorkTrackerContextListener listener;

    @Before
    public void setUp() {
        when(contextEvent.getServletContext()).thenReturn(mock(ServletContext.class));

        listener = new WorkTrackerContextListener();
    }

    @Test
    public void verifyVariablesInitialized() {
        listener.contextInitialized(contextEvent);

        ServletContext servletContext = contextEvent.getServletContext();
        verify(servletContext).setAttribute(eq(OUTSTANDING_ATTR), isA(OutstandingWork.class));
        verify(servletContext).setAttribute(eq(FLOOD_SENSOR_ATTR), isA(HttpFloodSensor.class));
        verify(servletContext).setAttribute(eq(ZOMBIE_ATTR), isA(ZombieDetector.class));

        try {
            MDC.put("thing", "value");
        } catch (Exception ignored) {
            fail("Should not throw an exception");
        }
    }
}
