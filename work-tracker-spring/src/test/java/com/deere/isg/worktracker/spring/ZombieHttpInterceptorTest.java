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


package com.deere.isg.worktracker.spring;

import com.deere.isg.worktracker.ZombieDetector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.deere.isg.worktracker.servlet.WorkContextListener.ZOMBIE_ATTR;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ZombieHttpInterceptorTest {

    @Mock
    private HttpRequest request;
    @Mock
    private ServletContext context;
    @Mock
    private ZombieDetector detector;
    @Mock
    private ClientHttpRequestExecution execution;

    private ZombieHttpInterceptor interceptor;
    private byte[] body = "body".getBytes(StandardCharsets.UTF_8);

    @Before
    public void setUp() {
        interceptor = new ZombieHttpInterceptor();
    }

    @Test
    public void detectorKillRunawayIsCalled() throws IOException {
        when(context.getAttribute(ZOMBIE_ATTR)).thenReturn(detector);
        interceptor.setServletContext(context);

        interceptor.intercept(request, body, execution);

        verify(execution).execute(request, body);
        verify(detector).killRunaway();
    }

    @Test
    public void nullDetectorKillRunawayNotCalled() throws IOException {
        when(context.getAttribute(ZOMBIE_ATTR)).thenReturn(null);
        interceptor.setServletContext(context);

        interceptor.intercept(request, body, execution);

        verify(execution).execute(request, body);
        verify(detector, never()).killRunaway();
    }
}
