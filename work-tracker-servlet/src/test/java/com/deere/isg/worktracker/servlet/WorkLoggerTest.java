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


package com.deere.isg.worktracker.servlet;

import com.deere.clock.Clock;
import net.logstash.logback.argument.StructuredArgument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

import static com.deere.isg.worktracker.Work.REQUEST_URL;
import static com.deere.isg.worktracker.servlet.HttpWork.STATUS_CODE;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static net.logstash.logback.argument.StructuredArguments.keyValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WorkLoggerTest {
    private static final String TEST_URI = "/";

    @Mock
    private Logger logger;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Captor
    private ArgumentCaptor<String> messageCaptor;
    @Captor
    private ArgumentCaptor<Object[]> saCaptor;

    private WorkLogger workLogger;

    @Before
    public void setUp() {
        Clock.freeze();
        workLogger = WorkLogger.getLogger();
        workLogger.setLogger(logger);

        when(request.getRequestURI()).thenReturn(TEST_URI);
    }

    @After
    public void tearDown() {
        Clock.clear();
    }

    @Test
    public void logsStartInfo() {
        final HttpWork work = TestWorkUtils.createWork();

        workLogger.logStart(request, work);

        verify(logger).info(messageCaptor.capture(),
                saCaptor.capture()
        );

        List<StructuredArgument> startInfo = work.getStartInfo();
        startInfo.add(keyValue(REQUEST_URL, TEST_URI));

        assertThat(messageCaptor.getValue(), is("Start of Request: url=" + TEST_URI));
        assertThat(saCaptor.getAllValues(), is(startInfo));
    }

    @Test
    public void logsEndInfo() {
        final int statusCode = SC_OK;
        final HttpWork work = TestWorkUtils.createWork();
        when(response.getStatus()).thenReturn(statusCode);

        workLogger.logEnd(request, response, work);

        verify(logger).info(messageCaptor.capture(), saCaptor.capture());

        List<StructuredArgument> endInfo = work.getEndInfo();
        endInfo.add(keyValue(STATUS_CODE, statusCode));
        endInfo.add(keyValue(REQUEST_URL, TEST_URI));

        assertThat(messageCaptor.getValue(), is("End of Request: status_code=" +
                statusCode + ", url=" + TEST_URI));
        assertThat(saCaptor.getAllValues(), is(endInfo));
    }

    @Test
    public void logsWithDebugSeverity() {
        workLogger.debug("message");

        verify(logger).debug("message");
    }
}
