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


package com.deere.isg.worktracker;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.loggingevent.ThreadNameJsonProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.MDC;

import java.io.IOException;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MdcThreadNameJsonProviderTest {
    private static final String THREAD_NAME = "thread_name";
    @Mock
    private JsonGenerator generator;
    private MdcThreadNameJsonProvider provider;

    @Before
    public void setUp() {
        provider = new MdcThreadNameJsonProvider();
    }

    @After
    public void tearDown() {
        MDC.clear();
    }

    @Test
    public void writesThreadName() throws IOException {
        ILoggingEvent event = new LoggingEvent();
        provider.writeTo(generator, event);

        verify(generator).writeStringField(ThreadNameJsonProvider.FIELD_THREAD_NAME, "main");
    }

    @Test
    public void writesThreadNameFromMDC() throws IOException {
        MDC.put(THREAD_NAME, "test_thread");

        ILoggingEvent event = new LoggingEvent();
        provider.writeTo(generator, event);

        verify(generator).writeStringField(ThreadNameJsonProvider.FIELD_THREAD_NAME, "test_thread");

    }

    @Test
    public void writesCurrentThreadNameMdcIsNull() throws IOException {
        MDC.put(THREAD_NAME, null);

        ILoggingEvent event = new LoggingEvent();
        provider.writeTo(generator, event);

        verify(generator).writeStringField(ThreadNameJsonProvider.FIELD_THREAD_NAME, "main");
    }

    @Test
    public void writesCurrentThreadNameIfMdcIsEmpty() throws IOException {
        MDC.put(THREAD_NAME, "");

        ILoggingEvent event = new LoggingEvent();
        provider.writeTo(generator, event);

        verify(generator).writeStringField(ThreadNameJsonProvider.FIELD_THREAD_NAME, "main");
    }
}
