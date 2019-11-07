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

import com.deere.isg.worktracker.OutstandingWorkTracker;
import org.slf4j.MDC;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Creates the payload to be tracked for the request
 *
 * @param <W> A generic type that extends HttpWork
 */
public abstract class AbstractHttpWorkFilter<W extends HttpWork>
        extends BaseTypeFilter<W> {
    private WorkLogger logger = WorkLogger.getLogger();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        W payload = createWork(request, response);
        HttpServletRequest httpRequest = getHttpRequest(payload, request);
        OutstandingWorkTracker<W> outstanding = getOutstanding();
        try {
            if (outstanding != null) {
                outstanding.<IOException, ServletException>doInTransactionChecked(payload, () -> {
                    doStartLog(payload, httpRequest);
                    chain.doFilter(httpRequest, response);
                });
            } else {
                chain.doFilter(httpRequest, response);
            }
        } catch (ClassCastException e) {
            throw new ServletException(e);
        } finally {
            try {
                postProcess(request, response, payload);
            } finally {
                if (outstanding != null) {
                    logger.logEnd(httpRequest, (HttpServletResponse) response, payload);
                }
                MDC.clear();
            }

        }
    }

    protected void postProcess(ServletRequest request, ServletResponse response, W payload) {
    }

    protected void doStartLog(W payload, HttpServletRequest httpRequest) {
        logger.logStart(httpRequest, payload);
    }

    /**
     * Should override createWork to create a new instance of {@link HttpWork } or its subclass.
     *
     * <pre>{@code
     *  protected HttpWork createWork(ServletRequest request) {
     *      return new HttpWork(request);
     *  }
     * }</pre>
     *
     * @param request the ServletRequest that gets passed by {@link #doFilter(ServletRequest, ServletResponse, FilterChain)}
     * @return new instance of {@link HttpWork} or its subclass.
     */
    protected abstract W createWork(ServletRequest request);

    protected W createWork(ServletRequest request, ServletResponse response){
        return createWork(request);
    }

    protected HttpServletRequest getHttpRequest(W payload, ServletRequest servletRequest) {
        return (HttpServletRequest) servletRequest;
    }

    protected WorkLogger getLogger() {
        return logger;
    }

    public void setLogger(WorkLogger logger) {
        this.logger = logger;
    }
}
