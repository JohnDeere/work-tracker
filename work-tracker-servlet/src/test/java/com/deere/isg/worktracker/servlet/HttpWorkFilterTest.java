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


package com.deere.isg.worktracker.servlet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HttpWorkFilterTest {
    private static final String TEST_URL = "this/path";

    @Mock
    private HttpServletRequest request;

    private HttpWorkFilter filter;

    @Before
    public void setUp() {
        filter = new HttpWorkFilter();
    }

    @Test
    public void createsInstanceOfHttpWorkWithRequest() {
        when(request.getMethod()).thenReturn("GET");
        when(request.getServletPath()).thenReturn(TEST_URL);

        HttpWork work = filter.createWork(request);

        assertThat(work.getService(), is("GET " + TEST_URL));

    }
}
