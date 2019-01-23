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
import com.deere.isg.worktracker.servlet.WorkLogger;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.deere.isg.worktracker.servlet.WorkContextListener.OUTSTANDING_ATTR;

/**
 * This class logs the start of a request
 */
public class SpringLoggerHandlerInterceptor extends HandlerInterceptorAdapter implements ServletContextAware {
    private OutstandingWork<?> outstanding;
    private WorkLogger logger = WorkLogger.getLogger();

    public SpringLoggerHandlerInterceptor() {
    }

    public SpringLoggerHandlerInterceptor(OutstandingWork<? extends SpringWork> outstanding) {
        this.outstanding = outstanding;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (outstanding != null) {
            logger.logStart(request, outstanding.current().orElse(null));
        }
        return true;
    }

    void setLogger(WorkLogger logger) {
        this.logger = logger;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setServletContext(ServletContext context) {
        this.outstanding = (OutstandingWork<? extends SpringWork>) context.getAttribute(OUTSTANDING_ATTR);
    }
}
