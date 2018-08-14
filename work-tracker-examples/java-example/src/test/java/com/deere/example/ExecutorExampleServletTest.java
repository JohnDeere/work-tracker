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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.MDC;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ExecutorExampleServletTest {

    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpServletRequest request;
    @Mock
    private Logger logger;

    private ExecutorExampleServlet servlet;

    @Before
    public void setUp() throws IOException {
        servlet = new ExecutorExampleServlet();
        servlet.setLogger(logger);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));
    }

    @After
    public void tearDown() {
        servlet.destroy();
        MDC.clear();
    }

    @Test
    public void executesACommand() throws ServletException, IOException {
        servlet.doGet(request, response);
        servlet.destroy(); //await shutdown

        verify(logger).info("some background running task");
    }
}
