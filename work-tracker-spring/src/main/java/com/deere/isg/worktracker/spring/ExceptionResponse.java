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


package com.deere.isg.worktracker.spring;

import org.springframework.http.HttpStatus;

public class ExceptionResponse {
    private int code;
    private String message;
    private String status;

    public ExceptionResponse() {

    }

    public ExceptionResponse(String message, HttpStatus code) {
        this(message, code.value(), code.getReasonPhrase());
    }

    public ExceptionResponse(String message, int code, String status) {
        this.message = message;
        this.code = code;
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public void setCode(HttpStatus code) {
        this.code = code.value();
    }

    public void setStatusCode(int statusCode) {
        this.code = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
