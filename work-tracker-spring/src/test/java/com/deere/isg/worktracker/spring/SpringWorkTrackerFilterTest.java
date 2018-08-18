/**
 * Copyright 2018 Deere & Company
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

import com.deere.isg.worktracker.servlet.LoggerFilter;
import com.deere.isg.worktracker.servlet.RequestBouncerFilter;
import com.deere.isg.worktracker.servlet.ZombieFilter;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;

public class SpringWorkTrackerFilterTest {
    @Test
    public void hasDefaultFilters() {
        SpringWorkTrackerFilter filter = new SpringWorkTrackerFilter();
        assertThat(filter.getFilters(), contains(
                instanceOf(SpringWorkFilter.class),
                instanceOf(LoggerFilter.class),
                instanceOf(RequestBouncerFilter.class),
                instanceOf(ZombieFilter.class)
        ));
    }

}