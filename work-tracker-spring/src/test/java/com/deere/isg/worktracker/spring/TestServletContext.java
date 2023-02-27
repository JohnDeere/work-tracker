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

import org.springframework.mock.web.MockServletContext;

import javax.servlet.ServletRegistration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestServletContext extends MockServletContext {

    static final String ENDPOINT_1 = "/serve/1";
    static final String ENDPOINT_2 = "/reg/abc";
    static final String ENDPOINT_3 = "/reg/xyz";
    static final String ENDPOINT_4 = "/";
    static final String ENDPOINT_5 = "something";

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        Map<String, ServletRegistration> servletRegistrations = new HashMap<>();
        ServletRegistration reg1 = mock(ServletRegistration.class);
        when(reg1.getMappings()).thenReturn(Collections.singletonList(ENDPOINT_1));
        servletRegistrations.put("servlet1", reg1);

        ServletRegistration reg2 = mock(ServletRegistration.class);
        when(reg2.getMappings()).thenReturn(Arrays.asList(ENDPOINT_2, ENDPOINT_3, ENDPOINT_4, ENDPOINT_5));
        servletRegistrations.put("servlet2", reg2);

        return servletRegistrations;
    }
}
