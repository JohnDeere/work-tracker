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

package integration;

import com.deere.isg.worktracker.servlet.ConnectionLimits;
import com.deere.isg.worktracker.spring.SpringWork;
import integration.helpers.Conditions;
import integration.helpers.MockWorkConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = LimitConfigurationTest.MockLimitWorkConfiguration.class)
public class LimitConfigurationTest {
    private static final int MAX_LIMIT = 25;

    @Autowired
    private MockWorkConfiguration configuration;

    @Test
    public void connectionLimitsSet() {
        List<ConnectionLimits<SpringWork>.Limit> limits = configuration.connectionLimits().getConnectionLimits();
        Conditions.assertConnectionLimitsNumbers(limits, MAX_LIMIT);
        Conditions.assertConditionTypes(limits);

    }

    @Configuration
    public static class MockLimitWorkConfiguration extends MockWorkConfiguration {
        public MockLimitWorkConfiguration() {
            setLimit(MAX_LIMIT);
        }
    }
}
