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


package com.deere.example;

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.ZombieDetector;
import com.deere.isg.worktracker.servlet.HttpFloodSensor;
import com.deere.isg.worktracker.spring.SpringWork;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;

import static com.deere.isg.worktracker.servlet.WorkContextListener.*;

@Component
public class WorkContext implements ServletContextAware {
    private OutstandingWork<SpringWork> outstanding;
    private HttpFloodSensor<SpringWork> floodSensor;
    private ZombieDetector detector;

    @Override
    @SuppressWarnings("unchecked")
    public void setServletContext(ServletContext servletContext) {
        this.outstanding = (OutstandingWork<SpringWork>) servletContext.getAttribute(OUTSTANDING_ATTR);
        this.floodSensor = (HttpFloodSensor<SpringWork>) servletContext.getAttribute(FLOOD_SENSOR_ATTR);
        this.detector = (ZombieDetector) servletContext.getAttribute(ZOMBIE_ATTR);
    }

    public OutstandingWork<SpringWork> getOutstanding() {
        return outstanding;
    }

    public ZombieDetector getDetector() {
        return detector;
    }

    public HttpFloodSensor<SpringWork> getFloodSensor() {
        return floodSensor;
    }
}
