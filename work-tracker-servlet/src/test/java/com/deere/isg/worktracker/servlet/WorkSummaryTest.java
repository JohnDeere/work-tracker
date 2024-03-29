/**
 * Copyright 2018-2023 Deere & Company
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

import org.junit.Before;
import org.junit.Test;


import static org.assertj.core.api.Assertions.assertThat;

public class WorkSummaryTest {
    private WorkSummary workSummary;

    @Before
    public void setUp() {
        workSummary = new WorkSummary(null);
    }

    @Test
    public void setsMsToElapsedTime() {
        workSummary.setElapsedMillis("1");
        assertThat(workSummary.getElapsedMillis()).isEqualTo("1 ms");
    }

    @Test
    public void addsSpaceToAcceptHeader() {
        workSummary.setAcceptHeader("text/html,application/xml;  q=0.9,image/webp,image/apng,*/*;q=0.8");

        assertThat(workSummary.getAcceptHeader())
                .isEqualTo("text/html,application/xml;&#8203;q=0.9,image/webp,image/apng,*/*;&#8203;q=0.8");
    }
}
