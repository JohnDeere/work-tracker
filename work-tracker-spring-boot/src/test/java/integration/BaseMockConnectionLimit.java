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


package integration;

import com.deere.isg.worktracker.servlet.ConnectionLimits;
import com.deere.isg.worktracker.spring.SpringWork;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class BaseMockConnectionLimit {
    private static final String LIMIT = "limit";
    private static final String TYPE_NAME = "typeName";

    protected void assertConnectionLimits(List<ConnectionLimits<SpringWork>.Limit> limits, int limit) {
        assertThat(limits, contains(
                hasProperty(LIMIT, is((int) (limit * 0.4))),
                hasProperty(LIMIT, is((int) (limit * 0.5))),
                hasProperty(LIMIT, is((int) (limit * 0.6))),
                hasProperty(LIMIT, is((int) (limit * 0.9)))
        ));

        assertThat(limits, contains(
                hasProperty(TYPE_NAME, is(ConnectionLimits.SESSION)),
                hasProperty(TYPE_NAME, is(ConnectionLimits.USER)),
                hasProperty(TYPE_NAME, is(ConnectionLimits.SERVICE)),
                hasProperty(TYPE_NAME, is(ConnectionLimits.TOTAL))
        ));
    }
}
