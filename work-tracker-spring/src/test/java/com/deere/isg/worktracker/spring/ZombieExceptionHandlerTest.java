/**
 * Copyright 2021 Deere & Company
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

import com.deere.isg.worktracker.ZombieError;
import com.deere.isg.worktracker.ZombieException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ZombieExceptionHandlerTest {
    private static final String MESSAGE = "This is a message";
    private ZombieExceptionHandler handler;

    @Before
    public void setUp() {
        handler = new ZombieExceptionHandler();
    }

    @Test
    public void zombieErrorReturnsStatusCode504() {
        ResponseEntity<Object> response = handler
                .handleZombieEx(new Exception(new ZombieError(MESSAGE)));

        assertThat(response.getStatusCode(), is(HttpStatus.GATEWAY_TIMEOUT));

        ExceptionResponse body = (ExceptionResponse) response.getBody();
        assertThat(body.getMessage(), is(MESSAGE));
        assertThat(body.getCode(), is(HttpStatus.GATEWAY_TIMEOUT.value()));
        assertThat(body.getStatus(), is(HttpStatus.GATEWAY_TIMEOUT.getReasonPhrase()));
    }

    @Test
    public void zombieExceptionReturnsStatusCode504() {
        ResponseEntity<Object> response = handler
                .handleZombieEx(new Exception(new ZombieException(MESSAGE)));

        assertThat(response.getStatusCode(), is(HttpStatus.GATEWAY_TIMEOUT));

        ExceptionResponse body = (ExceptionResponse) response.getBody();
        assertThat(body.getMessage(), is(MESSAGE));
        assertThat(body.getCode(), is(HttpStatus.GATEWAY_TIMEOUT.value()));
        assertThat(body.getStatus(), is(HttpStatus.GATEWAY_TIMEOUT.getReasonPhrase()));
    }

    @Test
    public void nonNestedExceptions() {
        ResponseEntity<Object> response = handler
                .handleZombieEx(new ZombieException(MESSAGE));

        assertThat(response.getStatusCode(), is(HttpStatus.GATEWAY_TIMEOUT));

        ExceptionResponse body = (ExceptionResponse) response.getBody();
        assertThat(body.getMessage(), is(MESSAGE));
        assertThat(body.getCode(), is(HttpStatus.GATEWAY_TIMEOUT.value()));
        assertThat(body.getStatus(), is(HttpStatus.GATEWAY_TIMEOUT.getReasonPhrase()));
    }

    @Test
    public void fallbackForExceptionsReturns500() {
        ResponseEntity<Object> response = handler.handleAllEx(new Exception(MESSAGE));

        assertThat(response.getStatusCode(), is(HttpStatus.INTERNAL_SERVER_ERROR));

        ExceptionResponse body = (ExceptionResponse) response.getBody();
        assertThat(body.getMessage(), is(MESSAGE));
        assertThat(body.getCode(), is(HttpStatus.INTERNAL_SERVER_ERROR.value()));
        assertThat(body.getStatus(), is(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()));
    }

    @Test
    public void zombieErrorReturnsGatewayTimeout() {
        ResponseEntity<Object> response = handler
                .handleAllEx(new Exception(new ZombieError(MESSAGE)));

        assertThat(response.getStatusCode(), is(HttpStatus.GATEWAY_TIMEOUT));

        ExceptionResponse body = (ExceptionResponse) response.getBody();
        assertThat(body.getMessage(), is(MESSAGE));
        assertThat(body.getCode(), is(HttpStatus.GATEWAY_TIMEOUT.value()));
        assertThat(body.getStatus(), is(HttpStatus.GATEWAY_TIMEOUT.getReasonPhrase()));
    }
}
