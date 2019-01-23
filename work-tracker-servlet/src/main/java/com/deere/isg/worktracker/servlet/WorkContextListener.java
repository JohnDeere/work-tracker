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

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.ZombieDetector;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * This class adds outstanding, floodSensor and zombieDetector to the {@link ServletContext}.
 * <p>
 * Your application is <b>required</b> to extend this class or create a new instance of this class
 * to provide the {@link WorkConfig} with the appropriate configuration for Outstanding, FloodSensor and
 * ZombieDetector.
 * <p>
 * If you do not provide an implementation for, or instance of, this class, you will not be able to
 * use any of the provided Filters as they all depend on ServletContext having Outstanding, FloodSensor
 * and ZombieDetector.
 *
 * <b>NOTE:</b> ZombieDetector and FloodSensor can be null if you don't need them. Do not initialize them
 * in the {@link WorkConfig} if you don't need them.
 * <p>
 * Usage:
 * For Java projects with Web Servlet, extend this class to provide the appropriate work configuration (notice <b>super</b>):
 * <pre>{@code
 * public class WorkTrackerContextListener extends WorkContextListener {
 *  public WorkTrackerContextListener() {
 *      super(new WorkConfig.Builder<>(new OutstandingWork<>())
 *              .withHttpFloodSensor()
 *              .withZombieDetector()
 *              .build()
 *      );
 *  }
 * }
 * }</pre>
 * <p>
 * Then, in your web.xml:
 * <pre>{@code
 *  <listener>
 *      <listener-class>com.example.WorkTrackerContextListener</listener-class>
 *  </listener>
 * }</pre>
 * <p>
 * <p>
 * For Spring projects, provide a bean to your configuration class, i.e.:
 * <pre>{@code
 * &#64;Bean
 * &#64;ConditionalOnMissingBean(WorkConfig.class)
 * public WorkConfig<W> workConfig() {
 *  return new WorkConfig.Builder<>(outstanding())
 *      .withHttpFloodSensor()
 *      .withZombieDetector()
 *      .build();
 *  }
 *
 * &#64;Bean
 * &#64;ConditionalOnMissingBean(WorkContextListener.class)
 * public WorkContextListener workContextListener(WorkConfig config) {
 *  return new WorkContextListener(config);
 * }
 * }</pre>
 */
public class WorkContextListener implements ServletContextListener {
    public static final String OUTSTANDING_ATTR = "outstanding";
    public static final String FLOOD_SENSOR_ATTR = "flood_sensor";
    public static final String ZOMBIE_ATTR = "zombie_detector";

    private OutstandingWork<?> outstanding;
    private HttpFloodSensor<?> floodSensor;
    private ZombieDetector detector;

    public WorkContextListener(WorkConfig config) {
        assert config != null : "WorkConfig cannot be null";
        this.outstanding = config.getOutstanding();
        this.floodSensor = config.getFloodSensor();
        this.detector = config.getDetector();
    }

    @Override
    public void contextInitialized(ServletContextEvent contextEvent) {
        ServletContext servletContext = contextEvent.getServletContext();
        servletContext.setAttribute(OUTSTANDING_ATTR, outstanding);
        servletContext.setAttribute(FLOOD_SENSOR_ATTR, floodSensor);
        servletContext.setAttribute(ZOMBIE_ATTR, detector);
    }

    @Override
    public void contextDestroyed(ServletContextEvent contextEvent) {
        if (detector != null) {
            detector.close();
        }
    }
}
