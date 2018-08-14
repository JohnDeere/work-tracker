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


package com.deere.isg.worktracker.servlet;

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.ZombieDetector;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class WorkConfigTest {
    private OutstandingWork<HttpWork> outstanding;
    private ZombieDetector detector;
    private ConnectionLimits<HttpWork> limit;

    @Before
    public void setUp() {
        outstanding = new OutstandingWork<>();
        detector = new ZombieDetector(outstanding);
        limit = new ConnectionLimits<>();
    }

    @Test(expected = AssertionError.class)
    public void outstandingCannotBeNull() {
        new WorkConfig.Builder<>(null);
    }

    @Test(expected = AssertionError.class)
    public void floodSensorCannotBeNull() {
        new WorkConfig.Builder<>(outstanding).setHttpFloodSensor(null);
    }

    @Test(expected = AssertionError.class)
    public void limitCannotBeNull() {
        new WorkConfig.Builder<>(outstanding)
                .setHttpFloodSensorWithLimit(null);
    }

    @Test(expected = AssertionError.class)
    public void detectorCannotBeNull() {
        new WorkConfig.Builder<>(outstanding).setZombieDetector(null);
    }

    @Test
    public void combinationCannotBeNull() {
        try {
            new WorkConfig.Builder<>(null)
                    .setZombieDetector(null)
                    .setHttpFloodSensor(null);

        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("Outstanding cannot be null"));
        }
    }

    @Test
    public void combinationNullDetectorCannotBeNull() {
        try {
            new WorkConfig.Builder<>(outstanding)
                    .setZombieDetector(null)
                    .setHttpFloodSensor(null);

        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("Detector cannot be null"));
        }
    }

    @Test
    public void combinationNullFloodSensorCannotBeNull() {
        try {
            new WorkConfig.Builder<>(outstanding)
                    .setZombieDetector(detector)
                    .setHttpFloodSensor(null);

        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("FloodSensor cannot be null"));
        }
    }

    @Test
    public void combinationNullLimitCannotBeNull() {
        try {
            new WorkConfig.Builder<>(outstanding)
                    .setZombieDetector(detector)
                    .setHttpFloodSensorWithLimit(null);

        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("Connection Limit cannot be null"));
        }
    }

    @Test
    public void variablesNotNullWithDefaultBuild() {
        WorkConfig<HttpWork> config = new WorkConfig.Builder<>(outstanding)
                .withHttpFloodSensor()
                .withZombieDetector()
                .build();

        assertThat(config.getOutstanding(), notNullValue());
        assertThat(config.getFloodSensor(), notNullValue());
        assertThat(config.getDetector(), notNullValue());
    }

    @Test
    public void constructorOnlyProvidesOutstanding() {
        WorkConfig<HttpWork> config = new WorkConfig.Builder<>(outstanding)
                .build();

        assertThat(config.getOutstanding(), notNullValue());
        assertThat(config.getFloodSensor(), nullValue());
        assertThat(config.getDetector(), nullValue());
    }

    @Test
    public void noZombieProvided() {
        WorkConfig<HttpWork> config = new WorkConfig.Builder<>(outstanding)
                .withHttpFloodSensor()
                .build();

        assertThat(config.getOutstanding(), notNullValue());
        assertThat(config.getFloodSensor(), notNullValue());
        assertThat(config.getDetector(), nullValue());
    }

    @Test
    public void noFloodSensorProvided() {
        WorkConfig<HttpWork> config = new WorkConfig.Builder<>(outstanding)
                .withZombieDetector()
                .build();

        assertThat(config.getOutstanding(), notNullValue());
        assertThat(config.getFloodSensor(), nullValue());
        assertThat(config.getDetector(), notNullValue());
    }

    @Test
    public void checkIFloodSensorIsDefaultWithDefaultConfig() {
        final int defaultTotalLimit = limit.getConnectionLimit(ConnectionLimits.TOTAL).getLimit();

        WorkConfig<HttpWork> config = new WorkConfig.Builder<>(outstanding)
                .setHttpFloodSensor(new HttpFloodSensor<>(outstanding))
                .build();

        assertThat(config.getFloodSensor().getConnectionLimit(ConnectionLimits.TOTAL).getLimit(), is(defaultTotalLimit));

    }

    @Test
    public void checkIfFloodSensorIsOverridden() {
        final int newTotalLimit = 50;
        limit.addConnectionLimit(newTotalLimit, ConnectionLimits.TOTAL).test(x -> true);

        WorkConfig<HttpWork> config = new WorkConfig.Builder<>(outstanding)
                .setHttpFloodSensorWithLimit(limit)
                .build();

        assertThat(config.getFloodSensor().getConnectionLimit(ConnectionLimits.TOTAL).getLimit(), is(newTotalLimit));
    }
}
