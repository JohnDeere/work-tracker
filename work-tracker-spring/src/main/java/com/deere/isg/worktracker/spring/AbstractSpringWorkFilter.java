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

import com.deere.isg.worktracker.servlet.AbstractHttpWorkFilter;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * An implementation of AbstractHttpWorkFilter to create the payload to track requests.
 * <p>
 * The class should be configured as a Bean to get the Autowiring working. If you plan to
 * use the default configurer {@code WorkTrackerConfigurer}, you won't need to create this bean
 * since it's already provided by that configurer (for Spring Boot).
 *
 * @param <W> A generic type that extends {@link SpringWork}
 */
public abstract class AbstractSpringWorkFilter<W extends SpringWork>
        extends AbstractHttpWorkFilter<W> {

    private KeyCleanser keyCleanser;

    public AbstractSpringWorkFilter() {
        initializeKeyCleanser();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        ServletEndpointRegistry.populate(filterConfig.getServletContext());
    }

    @Override
    protected HttpServletRequest getHttpRequest(SpringWork payload, ServletRequest servletRequest) {
        return payload.setupSpringVsFilterOrderingWorkaround((HttpServletRequest) servletRequest, getKeyCleanser());
    }

    public KeyCleanser getKeyCleanser() {
        return keyCleanser;
    }

    public void setKeyCleanser(KeyCleanser keyCleanser) {
        this.keyCleanser = keyCleanser;
    }

    private void initializeKeyCleanser() {
        if (keyCleanser == null) {
            setKeyCleanser(new PathMetadataCleanser());
        }
    }

    @Override
    protected void doStartLog(W payload, HttpServletRequest httpRequest) {
        // Delaying start log for spring until user and endpoint are available from Spring Interceptors
        if (ServletEndpointRegistry.contains(httpRequest.getRequestURI())){
            super.doStartLog(payload, httpRequest);
        }
    }
}
