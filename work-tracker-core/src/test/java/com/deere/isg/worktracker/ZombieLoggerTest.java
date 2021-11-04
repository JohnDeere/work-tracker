/**
 * Copyright 2021 Deere & Company
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

import com.deere.clock.Clock;
import net.logstash.logback.argument.StructuredArgument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import java.util.List;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ZombieLoggerTest {
    private static final String MESSAGE = "This is a message";
    private static final Exception EXCEPTION = new Exception("This is an error");

    private static final Work WORK = new Work() {
    };

    @Mock
    private Logger logger;

    private ZombieLogger zombieLogger;

    @Before
    public void setUp() {
        Clock.freeze();
        zombieLogger = ZombieLogger.getLogger();
        zombieLogger.setLogger(logger);
    }

    @After
    public void tearDown() {
        Clock.clear();
    }

    @Test
    public void killAZombieIsLoggedAsInfo() {
        zombieLogger.logKill(MESSAGE, WORK);
        List<StructuredArgument> metadata = WORK.getMetadata();

        verify(logger).info(MESSAGE, metadata.toArray());
    }

    @Test
    public void failureIsWarned() {
        zombieLogger.warnFailure(MESSAGE, WORK);
        List<StructuredArgument> metadata = WORK.getMetadata();

        verify(logger).warn(MESSAGE, metadata.toArray());
    }

    @Test
    public void zombieIsLoggedAsInfo() {
        zombieLogger.logZombie(MESSAGE, WORK);
        final List<StructuredArgument> metadata = WORK.getMetadata();

        verify(logger).info(MESSAGE, metadata.toArray());
    }

    @Test
    public void errorIsLogged() {
        zombieLogger.logError(MESSAGE, EXCEPTION);

        verify(logger).error(MESSAGE + ": " + EXCEPTION.getMessage(), EXCEPTION);
    }
}
