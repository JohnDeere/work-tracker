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
