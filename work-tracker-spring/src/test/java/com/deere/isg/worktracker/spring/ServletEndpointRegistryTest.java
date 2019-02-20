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

import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServletEndpointRegistryTest {

    @After
    public void tearDown() {
        ServletEndpointRegistry.clear();
    }

    @Test
    public void containsEndpoints() {
        ServletEndpointRegistry.populate("/test/value/thing");
        ServletEndpointRegistry.populate("/test/another/thing");
        ServletEndpointRegistry.populate("/yet/some/thing");

        assertThat(ServletEndpointRegistry.contains("/yet/some/thing"), is(true));
        assertThat(ServletEndpointRegistry.contains("/test/value/thing"), is(true));
        assertThat(ServletEndpointRegistry.contains("/test/another/thing"), is(true));
        assertThat(ServletEndpointRegistry.contains("/yet/here/thing"), is(false));
    }

    @Test
    public void restrictsRootAndStringNotStartingWithSlash() {
        ServletEndpointRegistry.populate((String) null);
        ServletEndpointRegistry.populate((HttpServletRequest) null);
        ServletEndpointRegistry.populate("/");
        ServletEndpointRegistry.populate("*.jsp");
        ServletEndpointRegistry.populate("something");

        assertThat(ServletEndpointRegistry.contains(null), is(false));
        assertThat(ServletEndpointRegistry.contains("/"), is(false));
        assertThat(ServletEndpointRegistry.contains("*.jsp"), is(false));
        assertThat(ServletEndpointRegistry.contains("something"), is(false));
    }

    @Test
    public void addingEndpointsFromServletRegistration() {
        TestServletContext sc = new TestServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(sc);

        ServletEndpointRegistry.populate(request);
        assertThat(ServletEndpointRegistry.contains("/serve/1"), is(true));
        assertThat(ServletEndpointRegistry.contains("/reg/abc"), is(true));
        assertThat(ServletEndpointRegistry.contains("/reg/xyz"), is(true));
        assertThat(ServletEndpointRegistry.contains("/"), is(false));
        assertThat(ServletEndpointRegistry.contains("something"), is(false));
        assertThat(ServletEndpointRegistry.contains("/something/else"), is(false));
    }

    private class TestServletContext extends MockServletContext {

        @Override
        public Map<String, ? extends ServletRegistration> getServletRegistrations() {
            Map<String, ServletRegistration> servletRegistrations = new HashMap<>();
            ServletRegistration reg1 = mock(ServletRegistration.class);
            when(reg1.getMappings()).thenReturn(Collections.singletonList("/serve/1"));
            servletRegistrations.put("servlet1", reg1);

            ServletRegistration reg2 = mock(ServletRegistration.class);
            when(reg2.getMappings()).thenReturn(Arrays.asList("/reg/abc", "/reg/xyz", "/", "something"));
            servletRegistrations.put("servlet2", reg2);

            return servletRegistrations;
        }
    }

}