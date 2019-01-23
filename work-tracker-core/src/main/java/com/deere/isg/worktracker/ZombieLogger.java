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


package com.deere.isg.worktracker;

import net.logstash.logback.argument.StructuredArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ZombieLogger {
    private static ZombieLogger instance = null;
    private Logger logger = LoggerFactory.getLogger(ZombieLogger.class);

    private ZombieLogger() {
    }

    public static ZombieLogger getLogger() {
        if (instance == null) {
            instance = new ZombieLogger();
        }

        return instance;
    }

    protected void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void logKill(String message, Work work) {
        List<StructuredArgument> metadata = work.getMetadata();
        logger.info(message, metadata.toArray());
    }

    public void warnFailure(String message, Work work) {
        logger.warn(message, work.getMetadata().toArray());
    }

    public void logZombie(String message, Work work) {
        List<StructuredArgument> metadata = work.getMetadata();
        logger.info(message, metadata.toArray());
    }

    public void logError(String message, Throwable throwable) {
        logger.error(message + ": " + throwable.getMessage(), throwable);
    }
}
