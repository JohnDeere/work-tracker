/**
 * Copyright 2019 Deere & Company
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.deere.example;

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.servlet.ConnectionLimits;
import com.deere.isg.worktracker.servlet.WorkConfig;
import com.deere.isg.worktracker.servlet.WorkContextListener;
import com.deere.isg.worktracker.spring.SpringWork;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;

@Configuration
public class WorkTrackerContextListener extends WorkContextListener {
    public WorkTrackerContextListener() {
        super(new WorkConfig.Builder<SpringWork>(new OutstandingWork<>())
                .setHttpFloodSensorWithLimit(connectionLimits()) //add the connectionLimits here
                .withZombieDetector()
                .build());
    }

    public static ConnectionLimits<SpringWork> connectionLimits() {
        ConnectionLimits<SpringWork> limits = new ConnectionLimits<>();
        //limit, typeName and function
        limits.addConnectionLimit(25, "service").method(SpringWork::getService);
        //limit, typeName and Predicate
        limits.addConnectionLimit(20, "acceptHeader").test(w -> w.getAcceptHeader()
                .contains(MediaType.APPLICATION_XML_VALUE)
        );
        return limits;
    }
}
