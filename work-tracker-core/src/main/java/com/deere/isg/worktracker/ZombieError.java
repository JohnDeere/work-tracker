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

public class ZombieError extends Error {
    public ZombieError() {
    }

    public ZombieError(String message) {
        super(message);
    }

    public ZombieError(String message, Throwable cause) {
        super(message, cause);
    }

    public ZombieError(Throwable cause) {
        super(cause);
    }

    public ZombieException asException() {
        return new ZombieException(this);
    }
}
