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


package com.deere.isg.worktracker.spring.boot;

import com.deere.isg.worktracker.spring.SpringWork;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

public class SpringBootWorkFilterTest {

    @Test
    public void createsSpringWorkInstance() {
        MockBootWorkFilter filter = new MockBootWorkFilter();
        filter.setWorkFactory(SpringWork::new);

        SpringWork work = filter.createWork(new MockHttpServletRequest());

        assertThat(work, notNullValue());
    }

    private class MockBootWorkFilter extends SpringBootWorkFilter<SpringWork> {
    }
}
