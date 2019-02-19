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


package com.deere.isg.worktracker.servlet;

import com.deere.isg.outstanding.Outstanding;
import com.deere.isg.worktracker.OutstandingWork;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import static com.deere.isg.worktracker.StringUtils.isNotBlank;
import static com.deere.isg.worktracker.servlet.WorkContextListener.OUTSTANDING_ATTR;
import static java.util.stream.Collectors.toList;

/**
 * A default template page is provided to show all the outstanding works.
 * However, you can override that page by specifying your own template.
 * Just add an init-param with <b>templatePath</b> as the param-name, example:
 * <pre>
 * {@code
 *  <servlet>
 *      ...
 *      <init-param>
 *          <param-name>templatePath</param-name>
 *          <param-value>/WEB-INF/templates/outstanding.jsp</param-value>
 *      </init-param>
 *      ...
 *  </servlet>
 * }
 * </pre>
 */
public class WorkHttpServlet extends HttpServlet {
    public static final String TEMPLATE_PATH = "templatePath";
    public static final String WORK_LIST = "work_list";

    private List<WorkSummary<? extends HttpWork>> workSummaries;
    private String templatePath;
    private Outstanding<? extends HttpWork> outstanding;
    private HtmlPage page;

    @Override
    @SuppressWarnings("unchecked")
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        String tempPath = getInitParameter(TEMPLATE_PATH);
        if (isNotBlank(tempPath)) {
            templatePath = tempPath;
        } else {
            page = new HtmlPage();
        }

        outstanding = (OutstandingWork<? extends HttpWork>) getServletContext().getAttribute(OUTSTANDING_ATTR);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletContext context = getServletConfig().getServletContext();
        response.setContentType("text/html;charset=UTF-8");
        if (context != null && outstanding != null) {
            workSummaries = mapOutstandingToSummaryList();
            request.setAttribute(WORK_LIST, workSummaries);
            if (templatePath != null) {
                context.getRequestDispatcher(templatePath).forward(request, response);
            } else {
                try (PrintWriter writer = response.getWriter()) {
                    writer.write(page.render(workSummaries));
                }
            }
        }
    }

    protected List<WorkSummary<? extends HttpWork>> mapOutstandingToSummaryList() {
        return getOutstanding().stream().map(w -> new WorkSummary<>(w)).collect(toList());
    }

    protected Outstanding<? extends HttpWork> getOutstanding() {
        return outstanding;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public List<WorkSummary<? extends HttpWork>> getWorkSummaries() {
        return workSummaries;
    }

    static class HtmlPage {
        private static final String STYLE = "<style>" +
                "body { font-family: \"Helvetica Neue\", \"Calibri\", sans-serif !important; }" +
                "h1 { text-align: center; }" +
                "table { width: 100%; }" +
                "table, th, td { border: 1px solid black; border-collapse: collapse; }" +
                "th, td { padding: 5px; text-align: center; }" +
                "table.work-table tr:nth-child(even) { background-color: #eee; }" +
                "table.work-table tr:nth-child(odd) { background-color: #fff; }" +
                "table.work-table th { color: white; background-color: #222222; }" +
                ".red { color: red; }" +
                "</style>";

        private static final String METADATA = "<meta charset=\"UTF-8\">" +
                "<title>Outstanding Requests</title>";

        private static final String HEAD = "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" + METADATA + STYLE + "</head>";

        private static final String BODY_START = "<body>" +
                "<h1 class=\"content-title\">Outstanding Requests</h1>";

        private static final String TABLE_START = "<table class=\"work-table\">" +
                "<tr>" +
                "<th>Service</th>" +
                "<th>Request Id</th>" +
                "<th>Start Time</th>" +
                "<th>Elapsed Time</th>" +
                "<th>Thread Name</th>" +
                "<th>Accept Headers</th>" +
                "</tr>";

        private static final String TABLE_END = "</table>";
        private static final String BODY_END = "</body>" + "</html>";

        String render(List<WorkSummary<? extends HttpWork>> workSummaries) {
            StringBuilder builder = new StringBuilder();
            builder.append(HEAD)
                    .append(BODY_START)
                    .append(TABLE_START);

            if (workSummaries != null) {
                for (WorkSummary summary : workSummaries) {
                    String row = "<tr" + isZombie(summary) + ">" +
                            "<td>" + summary.getService() + "</td>" +
                            "<td>" + summary.getRequestId() + "</td>" +
                            "<td>" + summary.getStartTime() + "</td>" +
                            "<td>" + summary.getElapsedMillis() + "</td>" +
                            "<td>" + summary.getThreadName() + "</td>" +
                            "<td>" + summary.getAcceptHeader() + "</td>" +
                            "</tr>";
                    builder.append(row);
                }
            }

            return builder.append(TABLE_END)
                    .append(BODY_END)
                    .toString();
        }

        private String isZombie(WorkSummary summary) {
            return summary.isZombie() ? " class='red'" : "";
        }
    }
}
