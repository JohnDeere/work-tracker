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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DefaultMetricEngineTest {
    private DefaultMetricEngine<Work> engine;

    @Before
    public void setup() {
       // engine = new DefaultMetricEngine<>();

    }

    @Test
    public void first() {

        assertTrue(true);
    }

    @Test
    public void second() {

//        DefaultMetricEngine.Builder<Work> builder = new DefaultMetricEngine.Builder<Work>();
//
//        builder = builder.output(this::writeLog)
//                .collector((b,w)->{
//                    if(w.isZombie()) {
//                        b.getMetric("zombie", CountMetric.class).increment();
//                    }
//                    b.getMetric("elapsed_millis", LongMetric.class).add(w.getElapsedMillis());
//                    b.getMetric("threads", UniqueMetric.class).add(w.getThreadName());
//                });
    }

    private void writeLog(MetricEngine.Bucket bucket) {
    }
}
