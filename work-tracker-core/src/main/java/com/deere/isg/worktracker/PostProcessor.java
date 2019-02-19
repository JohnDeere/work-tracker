package com.deere.isg.worktracker;

import com.deere.isg.outstanding.Outstanding;

public interface PostProcessor<W extends Work> {
    void init(Outstanding<W> outstanding);
    void postProcess(W work);
}
