package com.deere.isg.worktracker;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import java.util.LongSummaryStatistics;

public class LongExtendedStatistics extends LongSummaryStatistics {
    private StandardDeviation stdDev = new StandardDeviation(false);

    @Override
    public void accept(long value) {
        super.accept(value);
        stdDev.increment(value);
    }

    public double getStandardDeviation() {
        return stdDev.getResult();
    }
}
