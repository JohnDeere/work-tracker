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
import com.deere.isg.worktracker.Work;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.deere.isg.worktracker.servlet.TestWorkUtils.createWorkList;
import static com.deere.isg.worktracker.servlet.WorkContextListener.ALL_OUTSTANDING_ATTR;
import static com.deere.isg.worktracker.servlet.WorkHttpServlet.TEMPLATE_PATH;
import static com.deere.isg.worktracker.servlet.WorkHttpServlet.WORK_LIST;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WorkHttpServletTest {
    private static final String TEST_PATH = "test/path";

    private static final WorkHttpServlet.HtmlPage PAGE = new WorkHttpServlet.HtmlPage();

    private static final List<HttpWork> TEST_WORKS = createWorkList(10);
    private static final List<WorkSummary<? extends Work>> WORK_SUMMARIES = TEST_WORKS.stream()
            .map(WorkSummary::new)
            .collect(Collectors.toList());

    @Mock
    private ServletConfig config;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private OutstandingWork<HttpWork> outstanding;
    @Captor
    private ArgumentCaptor<List<WorkSummary>> workSummaryCaptor;
    @Captor
    private ArgumentCaptor<String> strCaptor;
    private WorkHttpServlet servlet;

    @Before
    public void setUp() {
        servlet = new WorkHttpServlet();
        when(config.getServletContext()).thenReturn(mock(ServletContext.class));
        when(config.getServletContext().getAttribute(ALL_OUTSTANDING_ATTR)).thenReturn(outstanding);
    }

    @Test
    public void contentTypeSetToHTML() throws ServletException, IOException {
        initPath(TEST_PATH);

        servlet.doGet(request, response);

        verify(response).setContentType("text/html;charset=UTF-8");
    }

    @Test
    public void rendererPathIsSet() throws ServletException {
        assertThat(servlet.getTemplatePath()).isNull();

        initPath(TEST_PATH);

        assertThat(servlet.getTemplatePath()).isEqualTo(TEST_PATH);
    }

    @Test
    public void forwardsToDispatcher() throws ServletException, IOException {
        initPath(TEST_PATH);

        servlet.doGet(request, response);

        verify(config.getServletContext().getRequestDispatcher(TEST_PATH))
                .forward(request, response);
    }

    @Test
    public void nullPathRenderDefaultView() throws ServletException, IOException {
        initPath(null);
        PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        servlet.doGet(request, response);

        assertThat(servlet.getTemplatePath()).isNull();
        verify(config.getServletContext().getRequestDispatcher(null), never())
                .forward(request, response);

        verify(writer).write(ArgumentMatchers.startsWith("<!DOCTYPE html>"));
        verify(writer).write(ArgumentMatchers.endsWith("</html>"));
    }

    @Test
    public void addsListToRequest() throws ServletException, IOException {
        initPath(TEST_PATH);

        servlet.doGet(request, response);

        verify(request).setAttribute(strCaptor.capture(), workSummaryCaptor.capture());
        assertThat(strCaptor.getValue()).isEqualTo(WORK_LIST);

        List<WorkSummary> workSummaries = workSummaryCaptor.getValue();
        for (WorkSummary summary : workSummaries) {
            assertThat(hasSameValues(summary)).isTrue();
        }
    }

    @Test
    public void nullPathSetsAttributeToRequest() throws ServletException, IOException {
        initPath(null);
        PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        servlet.doGet(request, response);

        verify(request).setAttribute(strCaptor.capture(), workSummaryCaptor.capture());
        assertThat(strCaptor.getValue()).isEqualTo(WORK_LIST);

        List<WorkSummary> workSummaries = workSummaryCaptor.getValue();
        for (WorkSummary summary : workSummaries) {
            assertThat(hasSameValues(summary)).isTrue();
        }
    }

    @Test
    public void htmlHasTableRows() {
        String html = PAGE.render(WORK_SUMMARIES);

        int count = 0;
        int total = 0;

        for (int i = 0; i < html.length() - 4; i++) {
            String substring = html.substring(i, i + 4);
            if (substring.equals("<td>")) {
                //opening td
                count = count + 1;
                total = total + 1;
            }

            if (substring.equals("/td>")) {
                //closing td
                count = count - 1;
            }
        }

        assertThat(count).isEqualTo(0);
        assertThat(total).isEqualTo(WORK_SUMMARIES.size() * 6);
    }

    @Test
    public void headerValidation() {
        String html = PAGE.render(WORK_SUMMARIES);
        assertThat(html).contains("<tr>" +
                "<th>Service</th>" +
                "<th>Request Id</th>" +
                "<th>Start Time</th>" +
                "<th>Elapsed Time</th>" +
                "<th>Thread Name</th>" +
                "<th>Accept Headers</th>" +
                "</tr>"
        );
    }

    @Test
    public void nullOutstandingHaveNoOutput() throws ServletException, IOException {
        when(config.getServletContext().getAttribute(ALL_OUTSTANDING_ATTR)).thenReturn(null);
        initPath(TEST_PATH);

        servlet.doGet(request, response);

        verify(request, never()).setAttribute(eq(WORK_LIST), any());
    }

    @Test
    public void setsRowToRedForZombie() {
        WorkSummary<HttpWork> zombieSummary = new WorkSummary<>(null);
        zombieSummary.setZombie(true);
        List<WorkSummary<? extends Work>> zombieSummaries = Collections.singletonList(zombieSummary);

        String html = PAGE.render(zombieSummaries);
        assertThat(html).contains("<tr class='red'>");
    }

    @Test
    public void setsRowToNeutralForNonZombie() {
        WorkSummary<HttpWork> zombieSummary = new WorkSummary<>(null);
        List<WorkSummary<? extends Work>> zombieSummaries = Collections.singletonList(zombieSummary);

        String html = PAGE.render(zombieSummaries);
        assertThat(html).contains("<tr>");
        assertThat(html).doesNotContain("class='red'");
    }

    private boolean hasSameValues(WorkSummary workSummary) {
        return TEST_WORKS.stream().anyMatch(work -> work.getRequestId()
                .equalsIgnoreCase(workSummary.getRequestId()));
    }

    private void initPath(String testPath) throws ServletException {
        when(outstanding.stream()).thenAnswer(invocationOnMock -> TEST_WORKS.stream());
        when(config.getInitParameter(TEMPLATE_PATH)).thenReturn(testPath);

        servlet.init(config);
        when(config.getServletContext().getRequestDispatcher(servlet.getTemplatePath()))
                .thenReturn(mock(RequestDispatcher.class));
    }

}
