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
