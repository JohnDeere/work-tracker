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

import com.deere.isg.worktracker.ZombieDetector;
import com.deere.isg.worktracker.ZombieError;
import com.deere.isg.worktracker.ZombieException;
import com.deere.isg.worktracker.ZombieLogger;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.deere.isg.worktracker.ZombieDetector.ZOMBIE;
import static javax.servlet.http.HttpServletResponse.SC_GATEWAY_TIMEOUT;

/**
 * This filter starts and ends the {@link ZombieDetector} which is provided
 * by the {@link WorkConfig} via the {@link WorkContextListener}.
 * <p>
 * It also redirects the response to {@code SC_GATE_TIMEOUT}
 * in the case of a {@link ZombieError} or {@link ZombieException}
 */
public class ZombieFilter extends BaseFilter {
    private ZombieLogger logger = ZombieLogger.getLogger();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        if (getDetector() != null) {
            getDetector().start();
        }
    }

    @Override
    public void destroy() {
        if (getDetector() != null) {
            getDetector().close();
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (ZombieError | ZombieException e) {
            logger.logError(ZOMBIE, e);
            ((HttpServletResponse) response).sendError(SC_GATEWAY_TIMEOUT, e.getMessage());
        }
    }
}
