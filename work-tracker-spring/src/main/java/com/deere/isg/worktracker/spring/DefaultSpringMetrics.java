package com.deere.isg.worktracker.spring;

import com.deere.isg.worktracker.DefaultMetricEngine;
import com.deere.isg.worktracker.MetricEngine;
import com.deere.isg.worktracker.RootCauseTurboFilter;
import org.joda.time.Duration;
import org.slf4j.MDC;

import java.util.function.Consumer;

import static com.deere.isg.worktracker.MetricEngine.*;

public class DefaultSpringMetrics<W extends SpringWork> {

    public MetricEngine<W> build(Consumer<Bucket> output) {
        return new DefaultMetricEngine.Builder<W>(Duration.standardSeconds(30))
                .collector((b, work) -> {

                    MetricSet endpointSet = b.getMetricSet("endpoint", work.getEndpoint());
                    addAllDetails(endpointSet, work);
                    addAllDetails(b, work);
//                    String clientKey = w.getClientKey();
//                    if(clientKey != null) {
//                        addAllDetails(w, endpointSet.getMetricSet("client_key", clientKey));
//                    }

                })
                .output(output)
                .build();

    }

    private void addAllDetails(MetricSet set, W work) {
        addDetails(set, work);

        work.getStatusCode().ifPresent(status->{
            addDetails(set.getMetricSet("status", status), work);
        });

        // unfortunately, the exception name does not get set in the Work's MDC.
        String exceptionName = MDC.get(RootCauseTurboFilter.FIELD_CAUSE_NAME);
        if(exceptionName != null) {
            addDetails(set.getMetricSet("error", exceptionName), work);
        }
    }

    private void addDetails(MetricSet set, W work) {
        set.getMetric("elapsed_millis", LongMetric.class).add(work.getElapsedMillis());

        if (work.isZombie()) {
            set.getMetric("zombie_count", CountMetric.class).increment();
        }

        // these should all get covered by the MDC
//        set.getMetric("thread_count", UniqueMetric.class).add(w.getThreadName());
//        set.getMetric("user_count", UniqueMetric.class).add(w.getRemoteUser());
//        set.getMetric("session_count", UniqueMetric.class).add(w.getSessionId());

        work.getMDC().forEach((key, value) ->
                set.getMetric(key+"_count", UniqueMetric.class).add(value));
    }
}
