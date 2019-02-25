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

import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.util.FastMath;

import java.util.LongSummaryStatistics;

public class LongExtendedStatistics extends LongSummaryStatistics {
    private Variance variance = new Variance(false);

    @Override
    public void accept(long value) {
        super.accept(value);
        variance.increment(value);
    }

    public double getStandardDeviation() {
        return FastMath.sqrt(variance.getResult());
    }

    public double getVariance() {
        return variance.getResult();
    }
}
