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

import com.deere.isg.worktracker.OutstandingWorkTracker;
import com.deere.isg.worktracker.ZombieDetector;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import static com.deere.isg.worktracker.servlet.WorkContextListener.*;

/**
 * This class initializes outstanding, floodSensor and detector to make
 * them available to its subclasses
 * <p>
 * You are required to override {@link Filter#doFilter(ServletRequest, ServletResponse, FilterChain)}
 * and {@link Filter#destroy()}.
 * <p>
 * Overriding {@link BaseFilter#init(FilterConfig)} is optional. However, if you do have to override it,
 * you need to call {@code super.init(filterConfig)} to initialize outstanding, floodSensor and detector.
 */
public abstract class BaseFilter implements Filter {
    private OutstandingWorkTracker<?> outstanding;
    private HttpFloodSensor<?> floodSensor;
    private ZombieDetector detector;

    /**
     * If you have to override this method, we will <b>always have</b> to call {@code super.init(filterConfig)}
     * to initialize outstanding, floodSensor, and detector. Otherwise, those variables will be null and
     * calling {@link #getOutstanding()}, {@link #getFloodSensor()}  and {@link #getDetector()}
     * will all return null value.
     *
     * @param filterConfig contains the {@link WorkConfig} that is provided by the {@link WorkContextListener}
     * @throws ServletException throws exception if something went wrong
     */
    @Override
    @SuppressWarnings("unchecked")
    public void init(FilterConfig filterConfig) throws ServletException {
        outstanding = (OutstandingWorkTracker<?>) filterConfig.getServletContext().getAttribute(OUTSTANDING_ATTR);
        floodSensor = (HttpFloodSensor<?>) filterConfig.getServletContext().getAttribute(FLOOD_SENSOR_ATTR);
        detector = (ZombieDetector) filterConfig.getServletContext().getAttribute(ZOMBIE_ATTR);
    }

    @Override
    public void destroy() {

    }

    protected OutstandingWorkTracker<?> getOutstanding() {
        return outstanding;
    }

    protected void setOutstanding(OutstandingWorkTracker<?> outstanding) {
        this.outstanding = outstanding;
    }

    protected HttpFloodSensor<?> getFloodSensor() {
        return floodSensor;
    }

    protected void setFloodSensor(HttpFloodSensor<?> floodSensor) {
        this.floodSensor = floodSensor;
    }

    protected ZombieDetector getDetector() {
        return detector;
    }

    protected void setDetector(ZombieDetector detector) {
        this.detector = detector;
    }
}
