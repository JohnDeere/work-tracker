package com.deere.isg.worktracker;

public interface PostProcessor<W extends Work> {
    void postProcess(W work);
}
