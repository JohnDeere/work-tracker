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

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.ZombieDetector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

import static com.deere.isg.worktracker.servlet.WorkContextListener.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BaseFilterTest {
    @Mock
    private OutstandingWork<?> outstanding;
    @Mock
    private ZombieDetector detector;
    @Mock
    private HttpFloodSensor<?> floodSensor;
    @Mock
    private FilterConfig config;

    @Before
    public void setUp() {
        when(config.getServletContext()).thenReturn(mock(ServletContext.class));
        when(config.getServletContext().getAttribute(OUTSTANDING_ATTR)).thenReturn(outstanding);
        when(config.getServletContext().getAttribute(FLOOD_SENSOR_ATTR)).thenReturn(floodSensor);
        when(config.getServletContext().getAttribute(ZOMBIE_ATTR)).thenReturn(detector);
    }

    @Test
    public void initVariablesFromContext() throws ServletException {
        MockBaseFilter workFilter = new MockBaseFilter();

        workFilter.init(config);

        assertThat(workFilter.getOutstanding()).isEqualTo(outstanding);
        assertThat(workFilter.getFloodSensor()).isEqualTo(floodSensor);
        assertThat(workFilter.getDetector()).isEqualTo(detector);
    }

    private class MockBaseFilter extends BaseFilter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        }
    }

}
