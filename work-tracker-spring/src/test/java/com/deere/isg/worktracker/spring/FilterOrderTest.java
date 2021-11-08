/**
 * Copyright 2018-2021 Deere & Company
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


package com.deere.isg.worktracker.spring;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

public class FilterOrderTest {
    @Test
    public void filterHasCorrespondingOrders() {
        assertThat(FilterOrder.WORK_FILTER.getOrder(), is(HIGHEST_PRECEDENCE + 10));
        assertThat(FilterOrder.FLOOD_SENSOR_FILTER.getOrder(), is(HIGHEST_PRECEDENCE + 10 + 1));
        assertThat(FilterOrder.ZOMBIE_FILTER.getOrder(), is(HIGHEST_PRECEDENCE + 10 + 2));
        assertThat(FilterOrder.PRE_AUTH_FILTER.getOrder(), is(1000));
        assertThat(FilterOrder.USER_POST_AUTH_FILTER.getOrder(), is(2390));
        assertThat(FilterOrder.FILTER_SECURITY_INTERCEPTOR.getOrder(), is(2400));
    }
}
