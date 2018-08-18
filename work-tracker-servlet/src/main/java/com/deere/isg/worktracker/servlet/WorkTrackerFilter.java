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


package com.deere.isg.worktracker.servlet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WorkTrackerFilter implements Filter {

    private List<BaseFilter> filters;

    public WorkTrackerFilter() {
        this(new HttpWorkFilter());
    }

    public WorkTrackerFilter(BaseFilter workFilter) {
        this(Arrays.asList(
                workFilter,
                new LoggerFilter(),
                new RequestBouncerFilter(),
                new ZombieFilter()
        ));
    }

    public WorkTrackerFilter(List<BaseFilter> filters) {
        this.filters = filters;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        for (BaseFilter filter : filters) {
            filter.init(filterConfig);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        WorkTrackerFilterChain workFilterChain = new WorkTrackerFilterChain(chain, filters);
        workFilterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        for (BaseFilter filter : filters) {
            filter.destroy();
        }
    }

    public List<BaseFilter> getFilters() {
        return new ArrayList<>(filters);
    }

    private static class WorkTrackerFilterChain implements FilterChain {

        private final FilterChain chain;
        private final List<BaseFilter> workFilters;
        private int position = 0;

        public WorkTrackerFilterChain(FilterChain chain, List<BaseFilter> workFilters) {
            this.chain = chain;
            this.workFilters = workFilters;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            if (position == workFilters.size()) {
                chain.doFilter(request, response);
            } else {
                position++;
                BaseFilter currentFilter = workFilters.get(position - 1);
                currentFilter.doFilter(request, response, this);
            }
        }
    }
}
