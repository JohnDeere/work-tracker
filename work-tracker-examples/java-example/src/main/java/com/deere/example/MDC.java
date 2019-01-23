/**
 * Copyright 2019 Deere & Company
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

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
