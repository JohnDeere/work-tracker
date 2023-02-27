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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.Filter;

import static integration.helpers.MockTestUtilities.returnResponse;


import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = MockKeyCleanserConfiguration.class )
public class KeyCleanserTest {
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
    public void standardKeysGetsConverted() throws Exception {
        MockHttpServletResponse response = returnResponse(mockMvc, "/aId/some ant/a bear/another cat/any dog");

        String contentAsString = response.getContentAsString();
        assertThat(contentAsString).contains("\"ant_id\":\"some ant\"");
        assertThat(contentAsString).contains("\"bear_id\":\"a bear\"");
        assertThat(contentAsString).contains("\"cat_id\":\"another cat\"");
        assertThat(contentAsString).contains("\"dog_id\":\"any dog\"");
    }

    @Test
    public void bannedKeysGetsConverted() throws Exception {
        MockHttpServletResponse response = returnResponse(mockMvc, "/transform/bean");

        assertThat(response.getContentAsString()).doesNotContain("\"transform\":\"bean\"");
        assertThat(response.getContentAsString()).contains("\"unknown_transform\":\"bean\"");
    }

    @Test
    public void testGetsTransformed() throws Exception {
        MockHttpServletResponse response = returnResponse(mockMvc, "/test/12345");

        String contentAsString = response.getContentAsString();
        assertThat(contentAsString).doesNotContain("\"test_id\":\"12345\"");
        assertThat(contentAsString).contains("\"mock_id\":\"12345\"");
    }
}
