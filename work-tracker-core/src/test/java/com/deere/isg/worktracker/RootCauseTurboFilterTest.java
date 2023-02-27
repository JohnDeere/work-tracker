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

package com.deere.isg.worktracker;

import ch.qos.logback.core.spi.FilterReply;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(reply).isEqualTo(FilterReply.NEUTRAL);
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

        assertThat(rootCauseName).isNull();
        assertThat(rootCause).isNull();
    }

    @Test
    public void findOneLayerExceptionRootCause() {
        Exception cause = new Exception(TEXT);
        Throwable rootCause = filter.findRootCause(cause);

        assertThat(cause.getCause()).isNull();
        assertThat(rootCause).isEqualTo(cause);
        assertThat(rootCause.getMessage()).isEqualTo(TEXT);
    }

    @Test
    public void findTwoLayerExceptionRootCause() {
        Error error = new Error(TEXT);
        Exception cause = new Exception(error);
        String causeName = filter.getCauseClassName(cause);

        Throwable rootCause = filter.findRootCause(cause);
        String rootCauseName = filter.getCauseClassName(rootCause);

        assertThat(causeName).isEqualTo(cause.getClass().getName());
        assertThat(rootCauseName).isEqualTo(error.getClass().getName());

        assertThat(rootCause).isEqualTo(error);
        assertThat(rootCause.getMessage()).isEqualTo(TEXT);
    }

    @Test
    public void findThreeLayerErrorRootCause() {
        Error error = new Error(TEXT);
        Exception innerEx = new Exception(error);
        Exception ex = new Exception(innerEx);

        Throwable rootCause = filter.findRootCause(ex);

        assertThat(rootCause).isEqualTo(error);
        assertThat(rootCause).isNotEqualTo(innerEx);

        assertThat(rootCause.getMessage()).isEqualTo(TEXT);
    }

    @Test
    public void circularExceptionCheck() {
        Exception a = new Exception();
        Exception b = new Exception(a);
        a.initCause(b);

        Throwable rootCauseA = filter.findRootCause(a);
        assertThat(rootCauseA).isEqualTo(b);

        Throwable rootCauseB = filter.findRootCause(b);
        assertThat(rootCauseB).isEqualTo(a);
    }

    private void assertMdcIsNull() {
        assertThat(MDC.get(causeFieldName)).isNull();
        assertThat(MDC.get(rootCauseFieldName)).isNull();
    }

    private void assertMdcHasClassName() {
        assertThat(MDC.get(causeFieldName)).isEqualTo(className);
        assertThat(MDC.get(rootCauseFieldName)).isEqualTo(className);
    }
}
