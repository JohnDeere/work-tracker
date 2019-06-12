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


package com.deere.isg.worktracker.spring;

import com.deere.isg.worktracker.servlet.RequestBouncerFilter;
import com.deere.isg.worktracker.servlet.ZombieFilter;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

/**
 * Filter order matters. If you are defining your own filter beans,
 * you should follow the order defined by the {@link FilterOrder}.
 * <p>
 * {@link SpringWorkFilter} should be first since it creates the work payload that
 * gets passed to the subsequent filters.
 * {@link RequestBouncerFilter} should be second since it protects the applications from request overflow.
 * {@link ZombieFilter} should be third since it starts a background job to track for zombie threads.
 * {@link SpringWorkPostAuthFilter} should be fourth since it updates the work metadata for the known user.
 * This filter is optional if you do not have a user authentication service for your application. Otherwise,
 * you need to provide the implementation for {@link SpringWork#updateUserInformation(javax.servlet.http.HttpServletRequest)}
 * in order to use this filter.
 */
public enum FilterOrder {
    WORK_FILTER,
    FLOOD_SENSOR_FILTER,
    ZOMBIE_FILTER,
    PRE_AUTH_FILTER(1000),
    USER_POST_AUTH_FILTER(2390),
    FILTER_SECURITY_INTERCEPTOR(2400);

    /**
     * <a href="https://github.com/spring-projects/spring-security/blob/master/config/src/main/java/org/springframework/security/config/http/SecurityFilters.java">
     * SecurityFilters</a>
     * According to the SecurityFilters, these are the required Order for PreAuthFilter and FilterSecurityInterceptor;
     * PRE_AUTH_FILTER = 1000
     * FILTER_SECURITY_INTERCEPTOR = 2400, so USER_POST_AUTH_FILTER is 2390 to be set before the security interceptor
     */

    private final int order;

    FilterOrder(int order) {
        this.order = order;
    }

    FilterOrder() {
        this.order = ordinal() + 10 + HIGHEST_PRECEDENCE;
    }

    public int getOrder() {
        return order;
    }
}
