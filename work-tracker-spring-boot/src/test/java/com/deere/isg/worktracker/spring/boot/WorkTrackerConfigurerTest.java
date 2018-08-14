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


package com.deere.isg.worktracker.spring.boot;

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.ZombieDetector;
import com.deere.isg.worktracker.servlet.ConnectionLimits;
import com.deere.isg.worktracker.servlet.HttpFloodSensor;
import com.deere.isg.worktracker.servlet.RequestBouncerFilter;
import com.deere.isg.worktracker.servlet.WorkConfig;
import com.deere.isg.worktracker.servlet.ZombieFilter;
import com.deere.isg.worktracker.spring.SpringLoggerHandlerInterceptor;
import com.deere.isg.worktracker.spring.SpringRequestBouncerHandlerInterceptor;
import com.deere.isg.worktracker.spring.SpringWork;
import com.deere.isg.worktracker.spring.SpringWorkPostAuthFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import javax.servlet.ServletRequest;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.function.Function;

import static com.deere.isg.worktracker.servlet.WorkContextListener.*;
import static com.deere.isg.worktracker.spring.boot.WorkTrackerConfigurer.DATA_SOURCE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WorkTrackerConfigurerTest {
    private static final String TYPE_NAME = "typeName";
    private static final String LIMIT = "limit";
    private static final int DATA_SOURCE_LIMIT = 20;
    private static final String ANY_DATA_SOURCE = "anyDataSource";
    private static final String NO_FLOOD_SENSOR_WARNING = "Since 'Limit' or 'DataSource' is not set, " +
            "FloodSensor (DoS Protection) will not be available";
    private static final String NO_DATA_SOURCE_MSG = "Could not find DataSource bean named ";

    @Mock
    private OutstandingWork<SpringWork> outstanding;
    @Mock
    private ZombieDetector detector;
    @Mock
    private HttpFloodSensor<SpringWork> floodSensor;
    @Mock
    private Logger logger;

    private WorkTrackerConfigurer<SpringWork> configurer;
    private MockFilterConfig config;

    @Before
    public void setUp() {
        configurer = new MockWorkTrackerConfiguration();
        config = new MockFilterConfig();
        config.getServletContext().setAttribute(OUTSTANDING_ATTR, outstanding);
        config.getServletContext().setAttribute(FLOOD_SENSOR_ATTR, floodSensor);
        config.getServletContext().setAttribute(ZOMBIE_ATTR, detector);

        configurer.setLogger(logger);
    }

    @Test
    public void instancesAreCreated() {
        assertThat(configurer.workContextListener(mock(WorkConfig.class)), notNullValue());
        assertThat(configurer.keyCleanser(), notNullValue());
    }

    @Test
    public void logbackServletInstantiated() {
        ServletRegistrationBean bean = configurer.logbackStatusServlet();

        assertThat(bean.getServletName(), is("viewStatusMessagesServlet"));
        assertThat(bean.getUrlMappings(), contains("/lbClassicStatus"));
    }

    @Test
    public void workConfigInstantiated() {
        WorkConfig<SpringWork> config = configurer.workConfig();

        assertThat(config.getOutstanding(), notNullValue());
        assertThat(config.getFloodSensor(), nullValue());
        assertThat(config.getDetector(), notNullValue());

        verify(logger).warn(NO_FLOOD_SENSOR_WARNING);
    }

    @Test
    public void workConfigInstantiatedWithDataSource() throws SQLException {
        setupDataSource(DATA_SOURCE, DATA_SOURCE_LIMIT);
        WorkConfig<SpringWork> config = configurer.workConfig();

        assertThat(config.getOutstanding(), notNullValue());
        assertThat(config.getFloodSensor(), notNullValue());
        assertThat(config.getDetector(), notNullValue());

        verify(logger, never()).warn(NO_FLOOD_SENSOR_WARNING);
    }

    @Test
    public void defaultLimitsSet() {
        ConnectionLimits<SpringWork> limit = configurer.connectionLimits();

        assertThat(limit, nullValue());
        verify(logger).warn(eq(NO_DATA_SOURCE_MSG + "'dataSource'"));
    }

    @Test
    public void limitsFromDataSourceName() {
        configurer.setDataSourceName(ANY_DATA_SOURCE);
        ConnectionLimits<SpringWork> limit = configurer.connectionLimits();

        assertThat(limit, nullValue());
        verify(logger).warn(eq(NO_DATA_SOURCE_MSG + "'" + ANY_DATA_SOURCE + "'"));
    }

    @Test
    public void determineConnectionLimitsFromDataSource() throws SQLException {
        setupDataSource(DATA_SOURCE, DATA_SOURCE_LIMIT);
        ConnectionLimits<SpringWork> limit = configurer.connectionLimits();

        assertThat(limit, notNullValue());
        assertThat(limit.getConnectionLimits(), contains(
                hasProperty(TYPE_NAME, is("session")),
                hasProperty(TYPE_NAME, is("user")),
                hasProperty(TYPE_NAME, is("service")),
                hasProperty(TYPE_NAME, is("total"))
        ));
        assertThat(limit.getConnectionLimits(), contains(
                hasProperty(LIMIT, is((int) (DATA_SOURCE_LIMIT * .4))),
                hasProperty(LIMIT, is((int) (DATA_SOURCE_LIMIT * .5))),
                hasProperty(LIMIT, is((int) (DATA_SOURCE_LIMIT * .6))),
                hasProperty(LIMIT, is((int) (DATA_SOURCE_LIMIT * .9)))
        ));
        verify(logger, never()).warn(eq(NO_DATA_SOURCE_MSG + "dataSource"), ArgumentMatchers.isA(Exception.class));
    }

    @Test
    public void limitEqualToTen() throws SQLException {
        setupDataSource(DATA_SOURCE, 10);

        ConnectionLimits<SpringWork> limit = configurer.connectionLimits();

        assertThat(limit, notNullValue());
    }

    @Test
    public void limitGreaterThanTen() throws SQLException {
        setupDataSource(DATA_SOURCE, 11);

        ConnectionLimits<SpringWork> limit = configurer.connectionLimits();

        assertThat(limit, notNullValue());
    }

    @Test
    public void limitLessThanTenReturnNull() throws SQLException {
        setupDataSource(DATA_SOURCE, 9);

        ConnectionLimits<SpringWork> limit = configurer.connectionLimits();

        assertThat(limit, nullValue());

    }

    @Test
    public void getComponentsFromContext() {
        configurer.setServletContext(config.getServletContext());

        assertThat(configurer.outstanding(), is(outstanding));
        assertThat(configurer.floodSensor(), is(floodSensor));
        assertThat(configurer.zombieDetector(), is(detector));
    }

    @Test
    public void interceptorsAdded() {
        InterceptorRegistry registry = mock(InterceptorRegistry.class);
        configurer.setServletContext(config.getServletContext());

        configurer.addInterceptors(registry);

        verify(registry).addInterceptor(any(SpringLoggerHandlerInterceptor.class));
        verify(registry).addInterceptor(any(SpringRequestBouncerHandlerInterceptor.class));
    }

    @Test
    public void defaultOutstandingPath() {
        assertThat(configurer.getOutstandingPath(), is("/health/outstanding"));
    }

    @Test
    public void outstandingPathUpdated() {
        configurer.setOutstandingPath("foo/bar");

        assertThat(configurer.getOutstandingPath(), is("foo/bar"));
    }

    @Test
    public void filtersAreAdded() {
        assertThat(configurer.springWorkFilter(), instanceOf(SpringBootWorkFilter.class));
        assertThat(configurer.springWorkFilter(), notNullValue());

        assertThat(configurer.requestBouncerFilter(), instanceOf(RequestBouncerFilter.class));
        assertThat(configurer.requestBouncerFilter(), notNullValue());

        assertThat(configurer.zombieFilter(), instanceOf(ZombieFilter.class));
        assertThat(configurer.zombieFilter(), notNullValue());

        assertThat(configurer.authFilter(), instanceOf(SpringWorkPostAuthFilter.class));
        assertThat(configurer.authFilter(), notNullValue());
    }

    @Test
    public void getsLimitsFromDataSource() throws Exception {
        setupDataSource(DATA_SOURCE, DATA_SOURCE_LIMIT);

        int limit = configurer.determineLimit();

        assertThat(limit, is(DATA_SOURCE_LIMIT));
    }

    @Test
    public void getLimitFromAnotherDataSource() throws Exception {
        configurer.setDataSourceName(ANY_DATA_SOURCE);
        setupDataSource(ANY_DATA_SOURCE, DATA_SOURCE_LIMIT);

        int limit = configurer.determineLimit();

        assertThat(limit, is(DATA_SOURCE_LIMIT));
    }

    @Test
    public void returnDefinedLimit() {
        configurer.setLimit(DATA_SOURCE_LIMIT);

        assertThat(configurer.determineLimit(), is(DATA_SOURCE_LIMIT));
    }

    @Test(expected = AssertionError.class)
    public void getsExceptionIfLessThanTenForLimit() {
        configurer.setLimit(9);
    }

    @Test
    public void getsExceptionIfEqualToTenForLimit() {
        configurer.setLimit(10);

        assertThat(configurer.determineLimit(), is(10));
    }

    @Test
    public void lessThanMinLimitFromNullConnection() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        configurer.setApplicationContext(applicationContext);

        assertThat(configurer.determineLimit(), is(9));
    }

    private void setupDataSource(String dataSourceName, int dataSourceLimit) throws SQLException {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        configurer.setApplicationContext(applicationContext);

        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getMaxConnections()).thenReturn(dataSourceLimit);

        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenReturn(mock(Connection.class));
        when(dataSource.getConnection().getMetaData()).thenReturn(metaData);

        when(applicationContext.getBean(dataSourceName)).thenReturn(dataSource);
    }

    private class MockWorkTrackerConfiguration extends WorkTrackerConfigurer<SpringWork> {
        @Override
        public Function<ServletRequest, SpringWork> workFactory() {
            return SpringWork::new;
        }
    }
}
