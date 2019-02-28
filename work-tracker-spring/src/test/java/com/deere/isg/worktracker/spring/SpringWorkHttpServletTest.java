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

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.servlet.HttpWork;
import com.deere.isg.worktracker.servlet.WorkSummary;
import org.junit.Test;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.List;
import java.util.stream.Stream;

import static com.deere.isg.worktracker.servlet.WorkContextListener.OUTSTANDING_ATTR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpringWorkHttpServletTest {

    @Test
    public void nullTurnsToEmpty() throws ServletException {
        SpringWork work = new SpringWork(null);
        OutstandingWork<SpringWork> outstandingWork = mock(OutstandingWork.class);
        when(outstandingWork.stream()).thenReturn(Stream.of(work));
        ServletConfig config = mock(ServletConfig.class);
        ServletContext context = mock(ServletContext.class);
        when(config.getServletContext()).thenReturn(context);
        when(context.getAttribute(OUTSTANDING_ATTR)).thenReturn(outstandingWork);

        SpringWorkHttpServlet servlet = new SpringWorkHttpServlet();
        servlet.init(config);

        List<WorkSummary<? extends HttpWork>> workSummaries = servlet.mapOutstandingToSummaryList();
        assertThat(workSummaries, hasSize(1));
        assertThat(workSummaries.get(0).getService(), is(""));

    }

    @Test
    public void nonEmptyEndpoint() throws ServletException {
        SpringWork work = mock(SpringWork.class);
        when(work.getEndpoint()).thenReturn("/test/url");
        OutstandingWork<SpringWork> outstandingWork = mock(OutstandingWork.class);
        when(outstandingWork.stream()).thenReturn(Stream.of(work));
        ServletConfig config = mock(ServletConfig.class);
        ServletContext context = mock(ServletContext.class);
        when(config.getServletContext()).thenReturn(context);
        when(context.getAttribute(OUTSTANDING_ATTR)).thenReturn(outstandingWork);

        SpringWorkHttpServlet servlet = new SpringWorkHttpServlet();
        servlet.init(config);

        List<WorkSummary<? extends HttpWork>> workSummaries = servlet.mapOutstandingToSummaryList();
        assertThat(workSummaries, hasSize(1));
        assertThat(workSummaries.get(0).getService(), is("/test/url"));

    }
}