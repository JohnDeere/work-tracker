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
package com.deere.isg.worktracker.spring;

import org.junit.After;
import org.junit.Test;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;


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

        assertThat(ServletEndpointRegistry.contains("/yet/some/thing")).isTrue();
        assertThat(ServletEndpointRegistry.contains("/test/value/thing")).isTrue();
        assertThat(ServletEndpointRegistry.contains("/test/another/thing")).isTrue();
        assertThat(ServletEndpointRegistry.contains("/yet/here/thing")).isFalse();
    }

    @Test
    public void restrictsRootAndStringNotStartingWithSlash() {
        ServletEndpointRegistry.populate((String) null);
        ServletEndpointRegistry.populate((ServletContext) null);
        ServletEndpointRegistry.populate("/");
        ServletEndpointRegistry.populate("*.jsp");
        ServletEndpointRegistry.populate("something");

        assertThat(ServletEndpointRegistry.contains((HttpServletRequest) null)).isFalse();
        assertThat(ServletEndpointRegistry.contains((String) null)).isFalse();
        assertThat(ServletEndpointRegistry.contains("/")).isFalse();
        assertThat(ServletEndpointRegistry.contains("*.jsp")).isFalse();
        assertThat(ServletEndpointRegistry.contains("something")).isFalse();
    }

    @Test
    public void addingEndpointsFromServletRegistration() {
        TestServletContext sc = new TestServletContext();

        ServletEndpointRegistry.populate(sc);
        assertThat(ServletEndpointRegistry.contains("/serve/1")).isTrue();
        assertThat(ServletEndpointRegistry.contains("/reg/abc")).isTrue();
        assertThat(ServletEndpointRegistry.contains("/reg/xyz")).isTrue();
        assertThat(ServletEndpointRegistry.contains("/")).isFalse();
        assertThat(ServletEndpointRegistry.contains("something")).isFalse();
        assertThat(ServletEndpointRegistry.contains("/something/else")).isFalse();
    }

}