/**
 * Copyright 2021 Deere & Company
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

import integration.helpers.MockWorkConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = LimitConfigurationTest.MockLimitWorkConfiguration.class)
public class DataSourceLimitConfigurationTest extends BaseMockConnectionLimit {
    private static final int LIMIT = 25;

    @Autowired
    private MockWorkConfiguration configuration;

    @Test
    public void connectionLimitsSet() {
        assertConnectionLimits(configuration.connectionLimits().getConnectionLimits(), LIMIT);
    }

    @Configuration
    public static class MockLimitWorkConfiguration extends MockWorkConfiguration {
        @Bean
        public DataSource dataSource() throws SQLException {
            DataSource dataSource = mock(DataSource.class);
            when(dataSource.getConnection()).thenReturn(mock(Connection.class));
            when(dataSource.getConnection().getMetaData()).thenReturn(mock(DatabaseMetaData.class));
            when(dataSource.getConnection().getMetaData().getMaxConnections()).thenReturn(60);
            return dataSource;
        }

    }
}
