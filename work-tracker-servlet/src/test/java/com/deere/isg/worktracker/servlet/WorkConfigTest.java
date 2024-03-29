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

package com.deere.isg.worktracker.servlet;

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.Work;
import com.deere.isg.worktracker.ZombieDetector;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class WorkConfigTest {
    private OutstandingWork<HttpWork> outstanding;
    private OutstandingWork<Work> allOutstanding;
    private ZombieDetector detector;
    private ConnectionLimits<HttpWork> limit;

    @Before
    public void setUp() {
        outstanding = new OutstandingWork<>();
        allOutstanding = new OutstandingWork<>();
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
            assertThat(e.getMessage()).isEqualTo("Outstanding cannot be null");
        }
    }

    @Test
    public void combinationNullDetectorCannotBeNull() {
        try {
            new WorkConfig.Builder<>(outstanding)
                    .setZombieDetector(null)
                    .setHttpFloodSensor(null);

        } catch (AssertionError e) {
            assertThat(e.getMessage()).isEqualTo("Detector cannot be null");
        }
    }

    @Test
    public void combinationNullFloodSensorCannotBeNull() {
        try {
            new WorkConfig.Builder<>(outstanding)
                    .setZombieDetector(detector)
                    .setHttpFloodSensor(null);

        } catch (AssertionError e) {
            assertThat(e.getMessage()).isEqualTo("FloodSensor cannot be null");
        }
    }

    @Test
    public void combinationNullLimitCannotBeNull() {
        try {
            new WorkConfig.Builder<>(outstanding)
                    .setZombieDetector(detector)
                    .setHttpFloodSensorWithLimit(null);

        } catch (AssertionError e) {
            assertThat(e.getMessage()).isEqualTo("Connection Limit cannot be null");
        }
    }

    @Test
    public void variablesNotNullWithDefaultBuild() {
        WorkConfig<HttpWork> config = new WorkConfig.Builder<>(outstanding)
                .withHttpFloodSensor()
                .withZombieDetector()
                .build();

        assertThat(config.getOutstanding()).isEqualTo(outstanding);
        assertThat(config.getAllOutstanding()).isEqualTo(outstanding);
        assertThat(config.getFloodSensor()).isNotNull();
        assertThat(config.getDetector()).isNotNull();
    }

    @Test
    public void constructorOnlyProvidesOutstanding() {
        WorkConfig<HttpWork> config = new WorkConfig.Builder<>(outstanding)
                .build();

        assertThat(config.getOutstanding()).isEqualTo(outstanding);
        assertThat(config.getAllOutstanding()).isEqualTo(outstanding);
        assertThat(config.getFloodSensor()).isNull();
        assertThat(config.getDetector()).isNull();
    }

    @Test
    public void noZombieProvided() {
        WorkConfig<HttpWork> config = new WorkConfig.Builder<>(outstanding)
                .withHttpFloodSensor()
                .build();

        assertThat(config.getOutstanding()).isEqualTo(outstanding);
        assertThat(config.getAllOutstanding()).isEqualTo(outstanding);
        assertThat(config.getFloodSensor()).isNotNull();
        assertThat(config.getDetector()).isNull();
    }

    @Test
    public void noFloodSensorProvided() {
        WorkConfig<HttpWork> config = new WorkConfig.Builder<>(outstanding)
                .withZombieDetector()
                .build();

        assertThat(config.getOutstanding()).isEqualTo(outstanding);
        assertThat(config.getAllOutstanding()).isEqualTo(outstanding);
        assertThat(config.getFloodSensor()).isNull();
        assertThat(config.getDetector()).isNotNull();
    }

    @Test
    public void checkIFloodSensorIsDefaultWithDefaultConfig() {
        final int defaultTotalLimit = limit.getConnectionLimit(ConnectionLimits.TOTAL).getLimit();

        WorkConfig<HttpWork> config = new WorkConfig.Builder<>(outstanding)
                .setHttpFloodSensor(new HttpFloodSensor<>(outstanding))
                .build();

        assertThat(config.getFloodSensor().getConnectionLimit(ConnectionLimits.TOTAL).getLimit()).isEqualTo(defaultTotalLimit);

    }

    @Test
    public void checkIfFloodSensorIsOverridden() {
        final int newTotalLimit = 50;
        limit.addConnectionLimit(newTotalLimit, ConnectionLimits.TOTAL).test(x -> true);

        WorkConfig<HttpWork> config = new WorkConfig.Builder<>(outstanding)
                .setHttpFloodSensorWithLimit(limit)
                .build();

        assertThat(config.getFloodSensor().getConnectionLimit(ConnectionLimits.TOTAL).getLimit()).isEqualTo(newTotalLimit);
    }

    @Test
    public void allOutstandingIsFiltered() {
        WorkConfig<HttpWork> config = new WorkConfig.Builder<>(allOutstanding, HttpWork.class)
                .withHttpFloodSensor()
                .withZombieDetector()
                .build();

        assertThat(config.getOutstanding()).isNotNull();
        assertThat(config.getAllOutstanding()).isEqualTo(allOutstanding);
        assertThat(config.getFloodSensor()).isNotNull();
        assertThat(config.getDetector()).isNotNull();

        Work otherWork = mock(Work.class);
        when(otherWork.isZombie()).thenReturn(true);
        allOutstanding.create(otherWork);

        assertThat(config.getOutstanding().current().isPresent()).isFalse();
        assertThat(config.getAllOutstanding().current().orElse(null)).isEqualTo(otherWork);

        config.getDetector().killRunaway(); // no exception should be thrown
        verify(otherWork, times(0)).isZombie();

        // Flood detector does not see all this other work going on.
        // Note that in the future we may decide for this to change and FloodDetector to honor background work in the limits.
        // but when we do, probably the limits themselves will be configured to honor this work.
        for (int i = 0; i < 100; i++) {
            allOutstanding.create(otherWork);
        }
        HttpServletResponse response = mock(HttpServletResponse.class);
        assertThat(config.getFloodSensor().mayProceedOrRedirectTooManyRequest(response)).isTrue();
    }
}
