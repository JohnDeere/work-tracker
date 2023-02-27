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

import integration.helpers.MockController;
import integration.helpers.MockKeyCleanserConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.Map;


import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = MockKeyCleanserConfiguration.class)
public class MDCVariableTest {
    @Autowired
    private Filter springWorkFilter;
    @Autowired
    private Filter requestBouncerFilter;
    @Autowired
    private Filter zombieFilter;

    private MockMvc mockMvc;
    private MockMDCFilter mdcFilter;
    private MockHttpSession session;

    @Before
    public void setUp() {
        session = new MockHttpSession();
        mdcFilter = new MockMDCFilter();
        mockMvc = MockMvcBuilders.standaloneSetup(new MockController())
                .addFilters(springWorkFilter, requestBouncerFilter, zombieFilter, mdcFilter)
                .build();
    }

    @Test
    public void hasMdcVariables() throws Exception {
        mockMvc.perform(get("/user/10/role/20").with(getRequestPostProcessor())).andReturn();
        Map<String, String> mdcMap = mdcFilter.getMdcMap();

        assertThat(mdcMap.keySet()).containsExactlyInAnyOrder(
                "path", "thread_name", "session_id",
                "remote_address", "request_id", "accept",
                "remote_user", "role", "path_user_name", "endpoint"
        );

        String actual = "GET /user/{path_user_name}/role/{role}";
        assertThat(mdcMap.get("path")).isEqualTo(actual);
        assertThat(mdcMap.get("endpoint")).isEqualTo(actual);
        assertThat(mdcMap.get("path_user_name")).isEqualTo("10");
        assertThat(mdcMap.get("role")).isEqualTo("20");
        assertThat(mdcMap.get("remote_user")).isEqualTo("test_user");
        assertThat(mdcMap.get("session_id")).isEqualTo(session.getId());
        assertThat(mdcMap.get("accept")).isEqualTo(MediaType.APPLICATION_JSON_UTF8_VALUE);
        assertThat(mdcMap.get("thread_name")).isEqualTo("main");
        assertThat(mdcMap.get("remote_address")).isEqualTo("1.2.3.4");
    }

    private RequestPostProcessor getRequestPostProcessor() {
        return request -> {
            request.setRemoteUser("test_user");
            request.setRemoteAddr("1.2.3.4");
            request.setSession(session);
            request.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_UTF8_VALUE);
            return request;
        };
    }

    private class MockMDCFilter implements Filter {
        private Map<String, String> mdcMap;

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {

        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
            filterChain.doFilter(servletRequest, servletResponse);
            mdcMap = MDC.getCopyOfContextMap();
        }

        public Map<String, String> getMdcMap() {
            return mdcMap;
        }

        @Override
        public void destroy() {

        }
    }
}
