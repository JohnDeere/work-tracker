/**
 * Copyright 2019 Deere & Company
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.deere.example;

import com.deere.isg.worktracker.OutstandingWork;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MDCTest {
    @Mock
    private OutstandingWork<?> outstandingWork;

    @Test
    public void doSomethingStupid() {
        assertThat(MDC.init(outstandingWork), is(outstandingWork));

        MDC.put("thing", "value");

        verify(outstandingWork).putInContext("thing", "value");
    }
}
