package com.deere.example;

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.servlet.ConnectionLimits;
import com.deere.isg.worktracker.servlet.HttpWork;
import com.deere.isg.worktracker.servlet.WorkConfig;
import com.deere.isg.worktracker.servlet.WorkContextListener;

public class WorkTrackerContextListener extends WorkContextListener {
    public WorkTrackerContextListener() {
        super(new WorkConfig.Builder<>(MDC.init(new OutstandingWork<>()))
                .setHttpFloodSensorWithLimit(connectionLimits()) //add the connectionLimits here
                .withZombieDetector()
                .build()
        );
    }

    public static ConnectionLimits<HttpWork> connectionLimits() {
        ConnectionLimits<HttpWork> limits = new ConnectionLimits<>();
        //limit, typeName and function
        limits.addConnectionLimit(25, "service").method(HttpWork::getService);
        //limit, typeName and Predicate
        limits.addConnectionLimit(20, "acceptHeader").test(w -> w.getAcceptHeader().contains("xml"));
        return limits;
    }
}
