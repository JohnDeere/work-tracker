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


package com.deere.example;

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.servlet.HttpWork;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ContextServletTest {
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private OutstandingWork<HttpWork> outstanding;

    private ContextServlet servlet;

    @Before
    public void setUp() throws IOException {
        MDC.init(outstanding);
        servlet = new ContextServlet();
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));
    }

    @Test
    public void checkContentTypeAndText() throws ServletException, IOException {
        servlet.doGet(request, response);

        verify(response).setContentType("text/html");
        verify(response.getWriter()).print("You added some data in the MDC");
    }

    @Test
    public void putDataInContext() throws ServletException, IOException {
        servlet.doGet(request, response);

        verify(outstanding).putInContext("short_id", "some value");
    }
}
