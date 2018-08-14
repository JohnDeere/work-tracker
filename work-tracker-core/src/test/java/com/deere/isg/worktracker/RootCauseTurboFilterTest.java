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


package com.deere.isg.worktracker;

import ch.qos.logback.core.spi.FilterReply;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class RootCauseTurboFilterTest {
    private static final String TEXT = "text";
    private static final Exception EXCEPTION = new Exception(TEXT);
    private RootCauseTurboFilter filter;
    private String causeFieldName;
    private String rootCauseFieldName;
    private String className;

    @Before
    public void setUp() {
        filter = new RootCauseTurboFilter();

        className = filter.getCauseClassName(EXCEPTION);
        causeFieldName = filter.getCauseFieldName();
        rootCauseFieldName = filter.getRootCauseFieldName();
    }

    @After
    public void tearDown() {
        MDC.clear();
    }

    @Test
    public void decideAddsToMDCReturnsNeutral() {
        assertMdcIsNull();

        FilterReply reply = filter.decide(null, null, null, null, null, EXCEPTION);

        assertThat(reply, is(FilterReply.NEUTRAL));
        assertMdcHasClassName();
    }

    @Test
    public void decideDoesNotAddNullRootCauseToMDC() {
        assertMdcIsNull();

        filter.decide(null, null, null, null, null, null);

        assertMdcIsNull();
    }

    @Test
    public void nullRootCause() {
        Throwable rootCause = filter.findRootCause(null);
        String rootCauseName = filter.getCauseClassName(rootCause);

        assertThat(rootCauseName, nullValue());
        assertThat(rootCause, nullValue());
    }

    @Test
    public void findOneLayerExceptionRootCause() {
        Exception cause = new Exception(TEXT);
        Throwable rootCause = filter.findRootCause(cause);

        assertThat(cause.getCause(), nullValue());
        assertThat(rootCause, is(cause));
        assertThat(rootCause.getMessage(), is(TEXT));
    }

    @Test
    public void findTwoLayerExceptionRootCause() {
        Error error = new Error(TEXT);
        Exception cause = new Exception(error);
        String causeName = filter.getCauseClassName(cause);

        Throwable rootCause = filter.findRootCause(cause);
        String rootCauseName = filter.getCauseClassName(rootCause);

        assertThat(causeName, is(cause.getClass().getName()));
        assertThat(rootCauseName, is(error.getClass().getName()));

        assertThat(rootCause, is(error));
        assertThat(rootCause.getMessage(), is(TEXT));
    }

    @Test
    public void findThreeLayerErrorRootCause() {
        Error error = new Error(TEXT);
        Exception innerEx = new Exception(error);
        Exception ex = new Exception(innerEx);

        Throwable rootCause = filter.findRootCause(ex);

        assertThat(rootCause, is(error));
        assertThat(rootCause, not(innerEx));

        assertThat(rootCause.getMessage(), is(TEXT));
    }

    @Test
    public void circularExceptionCheck() {
        Exception a = new Exception();
        Exception b = new Exception(a);
        a.initCause(b);

        Throwable rootCauseA = filter.findRootCause(a);
        assertThat(rootCauseA, is(b));

        Throwable rootCauseB = filter.findRootCause(b);
        assertThat(rootCauseB, is(a));
    }

    private void assertMdcIsNull() {
        assertThat(MDC.get(causeFieldName), nullValue());
        assertThat(MDC.get(rootCauseFieldName), nullValue());
    }

    private void assertMdcHasClassName() {
        assertThat(MDC.get(causeFieldName), is(className));
        assertThat(MDC.get(rootCauseFieldName), is(className));
    }
}
