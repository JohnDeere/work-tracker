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

public class WorkConfig<W extends HttpWork> {
    private OutstandingWork<W> outstanding;
    private HttpFloodSensor<W> floodSensor;
    private ZombieDetector detector;

    private WorkConfig(OutstandingWork<W> outstanding, HttpFloodSensor<W> floodSensor, ZombieDetector detector) {
        this.outstanding = outstanding;
        this.floodSensor = floodSensor;
        this.detector = detector;
    }

    public OutstandingWork<W> getOutstanding() {
        return outstanding;
    }

    public HttpFloodSensor<W> getFloodSensor() {
        return floodSensor;
    }

    public ZombieDetector getDetector() {
        return detector;
    }

    public static class Builder<T extends HttpWork> {
        private OutstandingWork<T> outstanding;
        private HttpFloodSensor<T> floodSensor;
        private ZombieDetector detector;

        public Builder(final OutstandingWork<T> outstanding) {
            assert outstanding != null : "Outstanding cannot be null";
            this.outstanding = outstanding;
        }

        public WorkConfig<T> build() {
            return new WorkConfig<>(outstanding, floodSensor, detector);
        }

        public Builder<T> withHttpFloodSensor() {
            this.floodSensor = new HttpFloodSensor<>(outstanding);
            return this;
        }

        public Builder<T> withZombieDetector() {
            this.detector = new ZombieDetector(outstanding);
            return this;
        }

        public Builder<T> setHttpFloodSensorWithLimit(final ConnectionLimits<T> connectionLimits) {
            assert connectionLimits != null : "Connection Limit cannot be null";
            this.floodSensor = new HttpFloodSensor<>(this.outstanding, connectionLimits);
            return this;
        }

        public Builder<T> setHttpFloodSensor(final HttpFloodSensor<T> floodSensor) {
            assert floodSensor != null : "FloodSensor cannot be null";
            this.floodSensor = floodSensor;
            return this;
        }

        public Builder<T> setZombieDetector(final ZombieDetector detector) {
            assert detector != null : "Detector cannot be null";
            this.detector = detector;
            return this;
        }
    }
}
