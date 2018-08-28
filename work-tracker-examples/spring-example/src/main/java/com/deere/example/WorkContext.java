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
