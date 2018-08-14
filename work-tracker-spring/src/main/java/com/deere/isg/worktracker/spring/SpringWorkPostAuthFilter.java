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


package com.deere.isg.worktracker.spring;

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.servlet.BaseTypeFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * This class updates the user information in the payload work
 * (i.e. work tracked by {@link OutstandingWork}).
 */
public class SpringWorkPostAuthFilter extends BaseTypeFilter {
    @Override
    @SuppressWarnings("unchecked")
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (getOutstanding() != null) {
            ((OutstandingWork<? extends SpringWork>) getOutstanding()).current()
                    .ifPresent(work -> addUserInformation((HttpServletRequest) request, work));
        }
        chain.doFilter(request, response);
    }

    private void addUserInformation(HttpServletRequest request, SpringWork work) {
        work.updateUserInformation(request);
    }

    void setFilterOutstanding(OutstandingWork outstanding) {
        super.setOutstanding(outstanding);
    }
}
