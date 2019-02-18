package com.deere.isg.worktracker;

import com.deere.isg.worktracker.MetricEngine.CountMetric;
import com.deere.isg.worktracker.MetricEngine.LongMetric;
import com.deere.isg.worktracker.MetricEngine.UniqueMetric;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

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
