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
import org.slf4j.MDC;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.regex.Pattern;

import static com.deere.isg.worktracker.servlet.HttpWork.*;
import static net.logstash.logback.argument.StructuredArguments.keyValue;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpWorkTest {
    private static final Pattern UUID_PATTERN =
            Pattern.compile("[a-zA-Z0-9]{8}-([a-zA-Z0-9]{4}-){3}[a-zA-Z0-9]{12}");
    private static final Pattern MILLIS_PATTERN =
            Pattern.compile("\\d+");

    private static final String REMOTE_USER_VALUE = "RemoteUser";
    private static final String SESSION_ID_VALUE = "SessionId";
    private static final String MAIN_THREAD_VALUE = "main";
    private static final String REMOTE_ADDRESS_VALUE = "RemoteAddress";
    private static final String PATH_VALUE = "ThisPath";
    private static final String ACCEPT_HEADER_VALUE = "text/plain";

    private HttpWork work;

    @Before
    public void setUp() {
        Clock.freeze();
        work = new HttpWork(null);
    }

    @After
    public void tearDown() {
        Clock.clear();
    }

    @Test
    public void getMetadataContainsPath() {
        work.setService(PATH_VALUE);

        assertThat(work.getMetadata(), hasItem(keyValue(PATH, PATH_VALUE)));
    }

    @Test
    public void valuesAddedInMDC() {
        work.setRemoteAddress(REMOTE_ADDRESS_VALUE);
        assertThat(MDC.get(REMOTE_ADDRESS), is(REMOTE_ADDRESS_VALUE));

        work.setRemoteUser(REMOTE_USER_VALUE);
        assertThat(MDC.get(REMOTE_USER), is(REMOTE_USER_VALUE));

        work.setService(PATH_VALUE);
        assertThat(MDC.get(PATH), is(PATH_VALUE));

        work.setSessionId(SESSION_ID_VALUE);
        assertThat(MDC.get(SESSION_ID), is(SESSION_ID_VALUE));

        work.setAcceptHeader(ACCEPT_HEADER_VALUE);
        assertThat(MDC.get(ACCEPT), is(ACCEPT_HEADER_VALUE));
    }

    @Test
    public void getMetadataContainsRemoteAddress() {
        work.setRemoteAddress(REMOTE_ADDRESS_VALUE);

        assertThat(work.getMetadata(), hasItem(keyValue(REMOTE_ADDRESS, REMOTE_ADDRESS_VALUE)));
    }

    @Test
    public void getMetadataContainsRemoteUser() {
        work.setRemoteUser(REMOTE_USER_VALUE);

        assertThat(work.getMetadata(), hasItem(keyValue(REMOTE_USER, REMOTE_USER_VALUE)));
    }

    @Test
    public void getMetadataContainsSessionId() {
        work.setSessionId(SESSION_ID_VALUE);

        assertThat(work.getMetadata(), hasItem(keyValue(SESSION_ID, SESSION_ID_VALUE)));
    }

    @Test
    public void getMetadataContainsRequestId() {
        assertThat(containsKVPattern(work, REQUEST_ID, UUID_PATTERN), is(true));
    }

    @Test
    public void getMetadataContainsElapsedMillis() {
        assertThat(containsKVPattern(work, ELAPSED_MS, MILLIS_PATTERN), is(true));
    }

    @Test
    public void getMetadataContainsThreadName() {
        assertThat(work.getMetadata(), hasItem(keyValue(THREAD_NAME, MAIN_THREAD_VALUE)));
    }

    @Test
    public void getMetadataContainsZombie() {
        assertThat(work.getMetadata(), hasItem(keyValue(ZOMBIE, false)));
    }

    @Test
    public void httpWorkDefaultConstructorHasEmptyValues() {
        HttpWork httpWork = new HttpWork(null);

        assertThat(httpWork.getService(), nullValue());
        assertThat(httpWork.getRemoteAddress(), nullValue());
        assertThat(httpWork.getRemoteUser(), nullValue());
        assertThat(httpWork.getSessionId(), nullValue());
    }

    @Test
    public void mapsRequestToHttpWork() {
        HttpWork httpWork = new HttpWork(getMockRequest());

        assertThat(httpWork.getRemoteAddress(), is("addr"));
        assertThat(httpWork.getSessionId(), is("session1234"));
        assertThat(httpWork.getRemoteUser(), is("John Doe"));
        assertThat(httpWork.getService(), is("GET this/path"));
    }

    private HttpServletRequest getMockRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getRemoteAddr()).thenReturn("addr");

        HttpSession mockSession = mock(HttpSession.class);
        when(mockSession.getId()).thenReturn("session1234");
        when(request.getSession(false)).thenReturn(mockSession);

        when(request.getRemoteUser()).thenReturn("John Doe");
        when(request.getServletPath()).thenReturn("this/path");
        when(request.getMethod()).thenReturn("GET");
        return request;
    }

    private boolean containsKVPattern(HttpWork httpWork, String key, Pattern valuePattern) {
        return httpWork.getMetadata().stream()
                .filter(StructuredArgument.class::isInstance)
                .map(Object::toString)
                .anyMatch(str -> str.contains(key) && valuePattern.matcher(str).find());
    }
}
