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
import integration.helpers.MockWorkConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.Filter;

import static integration.helpers.MockTestUtilities.returnResponse;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = MockWorkConfiguration.class)
public class PathVariablesToMDCTest {
    @Autowired
    private ApplicationContext context;

    @Autowired
    private Filter springWorkFilter;
    @Autowired
    private Filter requestBouncerFilter;
    @Autowired
    private Filter zombieFilter;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MockController())
                .addFilters(springWorkFilter, requestBouncerFilter, zombieFilter)
                .build();
    }

    @After
    public void tearDown() {
        MDC.clear();
    }

    @Test
    public void keyCleanserBeanExist() {
        assertThat(context.getBean("keyCleanser")).isNotNull();
    }

    @Test
    public void testIdInMDC() throws Exception {
        MockHttpServletResponse response = returnResponse(mockMvc, "/test/1");

        assertThat(response.getContentAsString()).contains("\"test_id\":\"1\"");
    }

    @Test
    public void userIdRoleInMDC() throws Exception {
        MockHttpServletResponse response = returnResponse(mockMvc, "/user/10/role/admin");

        String contentAsString = response.getContentAsString();
        assertThat(contentAsString).contains("\"path_user_name\":\"10\"");
        assertThat(contentAsString).contains("\"role\":\"admin\"");
    }

    @Test
    public void queryGuidInMDC() throws Exception {
        MockHttpServletResponse response = returnResponse(mockMvc, "/query/123456789/start=10;end=100;middle=50");

        assertThat(response.getContentAsString()).contains("\"query_guid\":\"123456789\"");
    }

    @Test
    public void oauthTokenInMDC() throws Exception {
        MockHttpServletResponse response = returnResponse(mockMvc, "/oauth-token/12345");

        assertThat(response.getContentAsString()).contains("\"oauth_token\":\"12345\"");
    }

    @Test
    public void sumInMDC() throws Exception {
        MockHttpServletResponse response = returnResponse(mockMvc, "/sum/1/2");

        String contentAsString = response.getContentAsString();
        assertThat(contentAsString).contains("\"first\":\"1\"");
        assertThat(contentAsString).contains("\"second\":\"2\"");
    }

    @Test
    public void idElapsedMsAndTokenInMDC() throws Exception {
        MockHttpServletResponse response = returnResponse(mockMvc, "/userId/123/oauth-token/456/logic/5");

        String contentAsString = response.getContentAsString();
        assertThat(contentAsString).contains("\"path_user_name\":\"123\"");
        assertThat(contentAsString).contains("\"oauth_token\":\"456\"");
        assertThat(contentAsString).contains("\"logic_elapsed_ms\":\"5\"");
    }
}
