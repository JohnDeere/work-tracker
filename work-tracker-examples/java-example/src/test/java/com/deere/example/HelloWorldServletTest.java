/**
 * Copyright 2019 Deere & Company
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.deere.example;

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
public class HelloWorldServletTest {
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private HelloWorldServlet servlet;

    @Before
    public void setUp() {
        servlet = new HelloWorldServlet();
    }

    @Test
    public void checkContentType() throws ServletException, IOException {
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));
        servlet.doGet(request, response);

        verify(response).setContentType("text/html");
        verify(response.getWriter()).print("Hello World");
    }
}
