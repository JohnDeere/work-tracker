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

import ch.qos.logback.classic.ViewStatusMessagesServlet;
import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.ZombieDetector;
import com.deere.isg.worktracker.servlet.*;
import com.deere.isg.worktracker.spring.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.Function;

import static com.deere.isg.worktracker.servlet.WorkContextListener.*;
import static com.deere.isg.worktracker.spring.FilterOrder.*;

/**
 * {@link WorkTrackerConfigurer} provides your Spring application with a
 * default configuration for the Work Tracker Spring library.
 * <p>
 * In order to use this config file, you will be required to extend this class in your
 * configuration file and provide a {@link #workFactory()} of how to create your
 * implementation of {@link SpringWork} object. Below is an example:
 * <pre>{@code
 *  &#64;Configuration
 *  public class WorkTrackerConfig extends WorkTrackerConfigurer<SpringWork> {
 *
 *      &#64;Override
 *      public Function<ServletRequest, SpringWork> workFactory() {
 *          return SpringWork::new;
 *      }
 *  }
 * }</pre>
 * <p>
 * If you are not using a user authentication service, you can just use {@link SpringWork} as the type for
 * {@link WorkTrackerConfigurer}.
 * <p>
 * However, if you plan to have user authentication, you should create a class that extends  {@link SpringWork}
 * and overrides {@link SpringWork#updateUserInformation(HttpServletRequest)} to add user information to the {@link org.slf4j.MDC}
 * by using {@link OutstandingWork#putInContext(String, String)}
 * <p>
 * See also: work-tracker-spring-boot-example
 * <p>
 * If you want to exclude this Config file, you will not have to extend this class
 */

public abstract class WorkTrackerConfigurer<W extends SpringWork> extends WebMvcConfigurerAdapter
        implements ServletContextAware, ApplicationContextAware {
    public static final String HEALTH_OUTSTANDING_PATH = "/health/outstanding";
    protected static final String DATA_SOURCE = "dataSource";
    private static final int MIN_LIMIT = 10;

    private String outstandingPath = HEALTH_OUTSTANDING_PATH;
    private String dataSourceName = DATA_SOURCE;
    private int limit = 0;

    private ServletContext context;
    private ApplicationContext applicationContext;
    private Logger logger = LoggerFactory.getLogger(WorkTrackerConfigurer.class);
    private String[] excludeUrls = new String[0];

    @Bean
    @ConditionalOnMissingBean(Function.class)
    public abstract Function<ServletRequest, W> workFactory();

    @Bean
    @ConditionalOnMissingBean(WorkContextListener.class)
    public WorkContextListener workContextListener(WorkConfig config) {
        return new WorkContextListener(config);
    }

    @Bean
    public ServletRegistrationBean logbackStatusServlet() {
        return new ServletRegistrationBean(new ViewStatusMessagesServlet(), "/lbClassicStatus");
    }

    /**
     * If you want to provide a different configuration for Outstanding, FloodSensor and/or ZombieDetector,
     * you need to override this method to provide that configuration. Otherwise, you will be provided
     * with the default configuration.
     * <p>
     * If you want to just update the limit for FloodSensor, you can just override {@link #connectionLimits()} instead.
     *
     *
     * @return The Work Configuration for {@link OutstandingWork}, {@link HttpFloodSensor} and {@link ZombieDetector}
     */
    @Bean
    @ConditionalOnMissingBean(WorkConfig.class)
    public WorkConfig<W> workConfig() {
        WorkConfig.Builder<W> builder = new WorkConfig.Builder<>(new OutstandingWork<W>()).withZombieDetector();

        ConnectionLimits<W> limit = connectionLimits();
        if (limit != null) {
            builder.setHttpFloodSensorWithLimit(limit);
        } else {
            logger.warn("Since 'Limit' or 'DataSource' is not set, " +
                    "FloodSensor (DoS Protection) will not be available");
        }

        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean(OutstandingWork.class)
    @SuppressWarnings("unchecked")
    public OutstandingWork<W> outstanding() {
        return (OutstandingWork<W>) context.getAttribute(OUTSTANDING_ATTR);
    }

    @Bean
    @ConditionalOnMissingBean(ZombieDetector.class)
    public ZombieDetector zombieDetector() {
        return (ZombieDetector) context.getAttribute(ZOMBIE_ATTR);
    }

    @Bean
    @ConditionalOnMissingBean(HttpFloodSensor.class)
    @SuppressWarnings("unchecked")
    public HttpFloodSensor<W> floodSensor() {
        return (HttpFloodSensor<W>) context.getAttribute(FLOOD_SENSOR_ATTR);
    }

    @Bean
    @ConditionalOnMissingBean(PathMetadataCleanser.class)
    public KeyCleanser keyCleanser() {
        return new PathMetadataCleanser();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SpringLoggerHandlerInterceptor(outstanding()));
        registry.addInterceptor(new SpringRequestBouncerHandlerInterceptor(floodSensor()));
    }

    @Bean
    @ConditionalOnMissingBean(ZombieExceptionHandler.class)
    public ZombieExceptionHandler zombieExceptionHandler() {
        return new ZombieExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean(WorkHttpServlet.class)
    public ServletRegistrationBean workHttpServlet() {
        return new ServletRegistrationBean(new WorkHttpServlet(), getOutstandingPath());
    }

    @Bean
    public FilterRegistrationBean springWorkFilterRegistrationBean() {
        WorkLogger logger = WorkLogger.getLogger();
        logger.excludeUrls(getExcludePathPatterns());

        SpringBootWorkFilter<W> filter = new SpringBootWorkFilter<>();
        filter.setWorkFactory(workFactory());
        filter.setKeyCleanser(keyCleanser());
        filter.setLogger(logger);

        return createFilter(filter, WORK_FILTER.getOrder(), "springWorkFilter");
    }

    @Bean
    public FilterRegistrationBean requestBouncerFilterRegistrationBean() {
        return createFilter(new RequestBouncerFilter(), FLOOD_SENSOR_FILTER.getOrder(), "requestBouncerFilter");
    }

    @Bean
    public FilterRegistrationBean zombieFilterRegistrationBean() {
        return createFilter(new ZombieFilter(), USER_POST_AUTH_FILTER.getOrder(), "authFilter");
    }

    @Bean
    public FilterRegistrationBean authFilterRegistrationBean() {
        return createFilter(new SpringWorkPostAuthFilter(), USER_POST_AUTH_FILTER.getOrder(), "authFilter");
    }

    @Bean
    @ConditionalOnMissingBean(SpringBootWorkFilter.class)
    public Filter springWorkFilter() {
        return springWorkFilterRegistrationBean().getFilter();
    }

    @Bean
    @ConditionalOnMissingBean(RequestBouncerFilter.class)
    public Filter requestBouncerFilter() {
        return requestBouncerFilterRegistrationBean().getFilter();
    }

    @Bean
    @ConditionalOnMissingBean(ZombieFilter.class)
    public Filter zombieFilter() {
        return zombieFilterRegistrationBean().getFilter();
    }

    @Bean
    @ConditionalOnMissingBean(SpringWorkPostAuthFilter.class)
    public Filter authFilter() {
        return authFilterRegistrationBean().getFilter();
    }

    public ConnectionLimits<W> connectionLimits() {
        int limit = determineLimit();
        if (greaterThanMinLimit(limit)) {
            return new ConnectionLimits<>(limit, true);
        }
        return null;
    }

    public String getOutstandingPath() {
        return outstandingPath;
    }

    protected void setOutstandingPath(String path) {
        this.outstandingPath = path;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.context = servletContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    protected void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public int getLimit() {
        return limit;
    }

    protected void setLimit(int limit) {
        assert greaterThanMinLimit(limit);
        this.limit = limit;
    }

    protected int determineLimit() {
        int limit = getLimit();
        if (greaterThanMinLimit(limit)) {
            return limit;
        }

        try {
            DataSource dataSource = (DataSource) applicationContext.getBean(getDataSourceName());
            try (Connection connection = dataSource.getConnection()) {
                return connection.getMetaData().getMaxConnections();
            }
        } catch (Exception e) {
            logger.warn("Could not find DataSource bean named '" + getDataSourceName() + "'");
        }

        return MIN_LIMIT - 1;
    }

    /**
     * Does not log start and end for the url patterns that excluded.
     * By default it is empty
     *
     * @param excludeUrls url patterns that do not need start and end logging
     */
    protected void excludePathPatterns(String... excludeUrls) {
        assert excludeUrls != null;
        this.excludeUrls = excludeUrls;
    }

    public String[] getExcludePathPatterns() {
        return excludeUrls;
    }

    private FilterRegistrationBean createFilter(Filter filter, int order, String name) {
        final FilterRegistrationBean filterBean = new FilterRegistrationBean();
        filterBean.setFilter(filter);
        filterBean.addUrlPatterns("/*");
        filterBean.setOrder(order);
        filterBean.setEnabled(true);
        filterBean.setName(name);
        return filterBean;
    }

    void setLogger(Logger logger) {
        this.logger = logger;
    }

    private boolean greaterThanMinLimit(int limit) {
        return limit >= MIN_LIMIT;
    }
}
