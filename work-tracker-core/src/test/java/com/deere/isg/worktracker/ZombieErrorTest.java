/**
 * Copyright 2019 Deere & Company
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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ZombieErrorTest {

    private static final String ERROR_MESSAGE = "This is an error message";
    private static final String ERROR_CAUSE = "This is the cause";

    @Test(expected = ZombieError.class)
    public void throwsZombieError() {
        throw new ZombieError();
    }

    @Test(expected = ZombieError.class)
    public void throwsZombieErrorWithMessage() {
        throw new ZombieError(ERROR_MESSAGE);
    }

    @Test
    public void checkMessageForError() {
        try {
            throw new ZombieError(ERROR_MESSAGE);
        } catch (ZombieError e) {
            assertThat(e.getMessage(), is(ERROR_MESSAGE));
        }
    }

    @Test
    public void checkMessageAndCauseForError() {
        try {
            throw new ZombieError(ERROR_MESSAGE, new Throwable(ERROR_CAUSE));
        } catch (ZombieError e) {
            assertThat(e.getMessage(), is(ERROR_MESSAGE));
            assertThat(e.getCause().getMessage(), is(ERROR_CAUSE));
        }
    }

    @Test
    public void checkCauseForError() {
        try {
            throw new ZombieError(new Throwable(ERROR_CAUSE));
        } catch (ZombieError e) {
            assertThat(e.getCause().getMessage(), is(ERROR_CAUSE));
        }
    }
}
