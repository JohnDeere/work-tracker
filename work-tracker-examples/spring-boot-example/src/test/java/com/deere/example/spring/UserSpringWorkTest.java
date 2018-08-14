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


package com.deere.example.spring;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.deere.isg.worktracker.servlet.HttpWork.REMOTE_USER;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class UserSpringWorkTest {
    private static final String TEST_USER = "jd1234";
    private UserSpringWork work;

    @Before
    public void setUp() {
        Authentication auth = new UsernamePasswordAuthenticationToken("jd1234", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        work = new UserSpringWork(null);
    }

    @Test
    public void updateUserInfoSetsUsernameToMDC() {
        work.updateUserInformation(null);

        assertThat(MDC.get(REMOTE_USER), is(TEST_USER));
        assertThat(work.getRemoteUser(), is(TEST_USER));
    }
}
