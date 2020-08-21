/**
 * Copyright 2020 Deere & Company
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

import com.deere.isg.worktracker.OutstandingWorkFilter;
import com.deere.isg.worktracker.OutstandingWorkTracker;
import com.deere.isg.worktracker.Work;
import com.deere.isg.worktracker.ZombieDetector;

public class WorkConfig<W extends HttpWork> {
    private OutstandingWorkTracker<? extends Work> allOutstanding;
    private OutstandingWorkTracker<? extends Work> outstanding;
    private HttpFloodSensor<W> floodSensor;
    private ZombieDetector detector;

    private WorkConfig(
            OutstandingWorkTracker<? extends Work> outstanding,
            HttpFloodSensor<W> floodSensor,
            ZombieDetector detector,
            OutstandingWorkTracker<? extends Work> allOutstanding) {
        this.outstanding = outstanding;
        this.floodSensor = floodSensor;
        this.detector = detector;
        this.allOutstanding = allOutstanding;
    }

    public OutstandingWorkTracker<? extends Work> getOutstanding() {
        return outstanding;
    }

    public HttpFloodSensor<W> getFloodSensor() {
        return floodSensor;
    }

    public ZombieDetector getDetector() {
        return detector;
    }

    public OutstandingWorkTracker<? extends Work> getAllOutstanding() {
        return allOutstanding;
    }

    public static class Builder<T extends HttpWork> {
        private OutstandingWorkTracker<? extends Work> outstanding;
        private OutstandingWorkTracker<T> filteredOutstanding;
        private OutstandingWorkTracker<? extends Work> zombieOutstanding;
        private HttpFloodSensor<T> floodSensor;
        private ZombieDetector detector;

        /**
         * Set up Work Tracker with the following behaviors: <ul>
         *     <li>WorkHttpServlet will only show work of type T</li>
         *     <li>ZombieDetector will only detect and kill work of type T when using withZombieDetector()</li>
         *     <li>HttpFloodSensor will only detect and reject work of type T when using withHttpFloodSensor
         *         and setHttpFloodSensorWithLimit</li>
         * </ul>
         *
         * This keeps backwards compatibility and a safe upgrade path.
         * @param outstanding
         */
        public Builder(final OutstandingWorkTracker<T> outstanding) {
            assert outstanding != null : "Outstanding cannot be null";
            this.outstanding = outstanding;
            this.filteredOutstanding = outstanding;
            this.zombieOutstanding = outstanding;
        }

        /**
         * Set up Work Tracker with the following behaviors: <ul>
         *     <li>WorkHttpServlet will only show all work tracked in the outstanding object</li>
         *     <li>ZombieDetector will only detect and kill work of type T when using withZombieDetector()</li>
         *     <li>HttpFloodSensor will only detect and reject work of type T when using withHttpFloodSensor
         *         and setHttpFloodSensorWithLimit</li>
         * </ul>
         *
         * Use this if you want to have visibility to all work including background or async processes along with HTTP.
         * @param outstanding
         */
        public Builder(final OutstandingWorkTracker<Work> outstanding, Class<T> workClazz) {
            assert outstanding != null : "Outstanding cannot be null";
            this.outstanding = outstanding;
            this.filteredOutstanding = new OutstandingWorkFilter<>(outstanding, workClazz);
            this.zombieOutstanding = filteredOutstanding;
        }

        public WorkConfig<T> build() {
            return new WorkConfig<T>(filteredOutstanding, floodSensor, detector, outstanding);
        }

        public Builder<T> withHttpFloodSensor() {
            this.floodSensor = new HttpFloodSensor<>(filteredOutstanding);
            return this;
        }

        public Builder<T> withZombieDetector() {
            this.detector = new ZombieDetector(zombieOutstanding);
            return this;
        }

        public Builder<T> setHttpFloodSensorWithLimit(final ConnectionLimits<T> connectionLimits) {
            assert connectionLimits != null : "Connection Limit cannot be null";
            this.floodSensor = new HttpFloodSensor<>(this.filteredOutstanding, connectionLimits);
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
