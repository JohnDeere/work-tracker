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
import com.deere.isg.worktracker.OutstandingWorkFilter;
import com.deere.isg.worktracker.Work;
import com.deere.isg.worktracker.ZombieDetector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import static com.deere.isg.worktracker.servlet.WorkContextListener.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class WorkContextListenerTest {
    @Mock
    private OutstandingWork<HttpWork> outstanding;
    @Mock
    private HttpFloodSensor<HttpWork> httpFloodSensor;
    @Mock
    private ZombieDetector detector;
    @Mock
    private ServletContext context;

    private ServletContextEvent contextEvent;
    private WorkContextListener workContextListener;

    @Before
    public void setUp() {
        WorkConfig config = new WorkConfig.Builder<>(outstanding)
                .setHttpFloodSensor(httpFloodSensor)
                .setZombieDetector(detector)
                .build();

        workContextListener = new WorkContextListener(config);
        contextEvent = new ServletContextEvent(context);
    }

    @Test
    public void addToContext() {
        workContextListener.contextInitialized(contextEvent);

        verify(context).setAttribute(OUTSTANDING_ATTR, outstanding);
        verify(context).setAttribute(FLOOD_SENSOR_ATTR, httpFloodSensor);
        verify(context).setAttribute(ZOMBIE_ATTR, detector);
        verify(context).setAttribute(ALL_OUTSTANDING_ATTR, outstanding);
    }

    @Test
    public void zombieDetectorIsClosed() {
        workContextListener.contextDestroyed(contextEvent);

        verify(detector).close();
    }

    @Test
    public void workConfigCannotBeNull() {
        try {
            new WorkContextListener(null);
        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("WorkConfig cannot be null"));
        }
    }

    @Test
    public void addsFilteredOutstandingToContext() {
        OutstandingWork<Work> outstandingWork = new OutstandingWork<>();
        WorkConfig<HttpWork> config = new WorkConfig.Builder<>(outstandingWork, HttpWork.class)
                .build();

        workContextListener = new WorkContextListener(config);
        workContextListener.contextInitialized(contextEvent);

        verify(context).setAttribute(ALL_OUTSTANDING_ATTR, outstandingWork);
        verify(context).setAttribute(eq(OUTSTANDING_ATTR), ArgumentMatchers.isA(OutstandingWorkFilter.class));
    }
}
