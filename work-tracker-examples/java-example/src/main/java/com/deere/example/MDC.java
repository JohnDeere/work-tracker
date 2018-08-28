package com.deere.example;

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.Work;

public final class MDC {
    private static OutstandingWork<?> outstandingWork;

    private MDC() {

    }

    static <W extends Work> OutstandingWork<W> init(OutstandingWork<W> ws) {
        outstandingWork = ws;
        return ws;
    }

    public static void put(String key, String value) {
        assert outstandingWork != null : "Call init to initialize outstandingWork";
        outstandingWork.putInContext(key, value);
    }
}
