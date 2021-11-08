/**
 * Copyright 2018-2021 Deere & Company
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

import com.deere.clock.Clock;
import com.deere.isg.worktracker.OutstandingWork;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SpringWorkPostAuthFilterTest {
    private static final String AUTHENTICATED = "authenticated_user";
    private static final String USER = "user";

    @Mock
    private OutstandingWork<SpringWork> outstanding;

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;
    private SpringWorkPostAuthFilter filter;

    @Before
    public void setUp() {
        MDC.clear();
        Clock.freeze();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        filter = new SpringWorkPostAuthFilter();
        filter.setFilterOutstanding(outstanding);
    }

    @After
    public void tearDown() {
        Clock.clear();
        MDC.clear();
    }

    @Test
    public void userIsUpdateInMDC() throws IOException, ServletException {
        assertThat(MDC.get(USER), nullValue());

        TestUserSpringWork work = new TestUserSpringWork(request);
        when(outstanding.current()).thenReturn(Optional.of(work));

        filter.doFilter(request, response, chain);

        assertThat(MDC.get(USER), is(AUTHENTICATED));
    }

    @Test
    public void nullOutstandingDoesNotUpdateUser() throws IOException, ServletException {
        assertThat(MDC.get(USER), nullValue());

        SpringWorkPostAuthFilter nullFilter = new SpringWorkPostAuthFilter();
        nullFilter.setFilterOutstanding(null);

        nullFilter.doFilter(request, response, chain);

        assertThat(MDC.get(USER), nullValue());

    }

    protected class TestUserSpringWork extends SpringWork {
        TestUserSpringWork(ServletRequest request) {
            super(request);
        }

        @Override
        public void updateUserInformation(HttpServletRequest request) {
            addToMDC(USER, AUTHENTICATED);
        }
    }
}
