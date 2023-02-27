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
package integration.helpers;

import com.deere.isg.worktracker.servlet.ConnectionLimits;
import com.deere.isg.worktracker.spring.SpringWork;
import org.assertj.core.api.Condition;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class Conditions {
    public static Condition<ConnectionLimits<SpringWork>.Limit> typeNamed(String name) {
        return new Condition<ConnectionLimits<SpringWork>.Limit>() {
            @Override
            public boolean matches(ConnectionLimits<SpringWork>.Limit value) {
                return value.getTypeName().equals(name);
            }
        };
    }

    public static Condition<ConnectionLimits<SpringWork>.Limit> limitIs(int limit) {
        return new Condition<ConnectionLimits<SpringWork>.Limit>() {
            @Override
            public boolean matches(ConnectionLimits<SpringWork>.Limit value) {
                return Objects.equals(value.getLimit(), limit);
            }
        };
    }

    public static void assertConnectionLimitsNumbers(List<ConnectionLimits<SpringWork>.Limit> limits, int limit) {
        assertThat(limits)
                .haveAtLeastOne(limitIs((int) (limit * 0.4)))
                .haveAtLeastOne(limitIs((int) (limit * 0.5)))
                .haveAtLeastOne(limitIs((int) (limit * 0.6)))
                .haveAtLeastOne(limitIs((int) (limit * 0.9)));
    }

    public static void assertConditionTypes(List<ConnectionLimits<SpringWork>.Limit> limits) {
        assertThat(limits)
                .haveAtLeastOne(typeNamed(ConnectionLimits.SESSION))
                .haveAtLeastOne(typeNamed(ConnectionLimits.USER))
                .haveAtLeastOne(typeNamed(ConnectionLimits.SERVICE))
                .haveAtLeastOne(typeNamed(ConnectionLimits.TOTAL));
    }
}
