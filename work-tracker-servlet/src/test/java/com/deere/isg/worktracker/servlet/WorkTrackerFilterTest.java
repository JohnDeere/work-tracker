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

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.ZombieDetector;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

import static com.deere.isg.worktracker.servlet.WorkContextListener.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class WorkTrackerFilterTest {
    private static final List<BaseFilter> SPY_FILTERS = Arrays.asList(
            spy(HttpWorkFilter.class),
            spy(LoggerFilter.class),
            spy(RequestBouncerFilter.class),
            spy(ZombieFilter.class)
    );

    private WorkTrackerFilter workTrackerFilter;
    private FilterConfig filterConfig;
    private OutstandingWork<HttpWork> outstanding;
    private HttpFloodSensor<HttpWork> floodSensor;
    private ZombieDetector detector;

    @Before
    public void setUp() {
        outstanding = new OutstandingWork<>();
        floodSensor = new HttpFloodSensor<>(outstanding);
        detector = new ZombieDetector(outstanding);

        ServletContext context = mock(ServletContext.class);

        workTrackerFilter = new WorkTrackerFilter();

        filterConfig = mock(FilterConfig.class);
        when(filterConfig.getServletContext()).thenReturn(context);

        when(context.getAttribute(OUTSTANDING_ATTR)).thenReturn(outstanding);
        when(context.getAttribute(FLOOD_SENSOR_ATTR)).thenReturn(floodSensor);
        when(context.getAttribute(ZOMBIE_ATTR)).thenReturn(detector);
    }

    @Test
    public void hasDefaultFilters() {
        assertThat(workTrackerFilter.getFilters(), contains(
                instanceOf(HttpWorkFilter.class),
                instanceOf(LoggerFilter.class),
                instanceOf(RequestBouncerFilter.class),
                instanceOf(ZombieFilter.class)
        ));
    }

    @Test
    public void initializesFilters() throws ServletException {
        workTrackerFilter.init(filterConfig);
        List<BaseFilter> filters = workTrackerFilter.getFilters();

        for (BaseFilter filter : filters) {
            assertThat(filter.getOutstanding(), is(outstanding));
            assertThat(filter.getFloodSensor(), is(floodSensor));
            assertThat(filter.getDetector(), is(detector));
        }
    }

    @Test
    public void destroysFilters() {
        List<BaseFilter> mockFilters = Arrays.asList(
                mock(HttpWorkFilter.class),
                mock(LoggerFilter.class)
        );

        WorkTrackerFilter trackerFilter = new WorkTrackerFilter(mockFilters);
        trackerFilter.destroy();

        for (BaseFilter filter : mockFilters) {
            verify(filter).destroy();
        }
    }

    @Test
    public void goesThroughListOfFiltersThenContinue() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        WorkTrackerFilter workFilter = new WorkTrackerFilter(SPY_FILTERS);
        workFilter.init(filterConfig);
        workFilter.doFilter(
                request,
                response,
                filterChain
        );

        for (BaseFilter filter : SPY_FILTERS) {
            verify(filter).doFilter(eq(request), eq(response), any());
        }

        verify(filterChain).doFilter(request, response);
    }

}