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

package com.deere.isg.worktracker.spring;

import com.deere.isg.worktracker.servlet.HttpFloodSensor;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.deere.isg.worktracker.servlet.WorkContextListener.FLOOD_SENSOR_ATTR;

/**
 * This class checks if the request can still pass through after user is known,
 * i.e. after user authentication occurs.
 */
public class SpringRequestBouncerHandlerInterceptor extends HandlerInterceptorAdapter implements ServletContextAware {
    private HttpFloodSensor<?> floodSensor;

    public SpringRequestBouncerHandlerInterceptor() {
    }

    public SpringRequestBouncerHandlerInterceptor(HttpFloodSensor<? extends SpringWork> floodSensor) {
        this.floodSensor = floodSensor;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        return floodSensor == null || floodSensor.mayProceedOrRedirectTooManyRequest(response);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setServletContext(ServletContext servletContext) {
        this.floodSensor = (HttpFloodSensor<? extends SpringWork>) servletContext.getAttribute(FLOOD_SENSOR_ATTR);
    }
}
