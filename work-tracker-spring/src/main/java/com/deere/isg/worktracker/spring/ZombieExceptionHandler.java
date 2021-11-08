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

import com.deere.isg.worktracker.ZombieError;
import com.deere.isg.worktracker.ZombieException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ZombieExceptionHandler {

    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    @ExceptionHandler({ ZombieException.class, ZombieError.class })
    @ResponseBody
    public ResponseEntity<Object> handleZombieEx(Exception ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body(new ExceptionResponse(cause.getMessage(), HttpStatus.GATEWAY_TIMEOUT));
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler({ Exception.class })
    @ResponseBody
    public ResponseEntity<Object> handleAllEx(Exception exception) {
        if (exception.getCause() != null && exception.getCause() instanceof ZombieError) {
            return handleZombieEx(exception);
        }
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ExceptionResponse(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));

    }
}
