/**
 * Copyright 2019 Deere & Company
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.deere.example.spring;

import com.deere.clock.Clock;
import com.deere.isg.worktracker.ZombieDetector;
import com.deere.isg.worktracker.servlet.MdcExecutor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

import static com.deere.example.spring.HelloWorldController.EXAMPLE_URL;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(MockitoJUnitRunner.class)
public class HelloWorldControllerTest {
    @Mock
    private ZombieDetector detector;
    @Mock
    private Logger logger;

    private MockRestServiceServer server;
    private ThreadPoolTaskExecutor taskExecutor;

    private HelloWorldController controller;

    @Before
    public void setUp() {
        taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(5);
        taskExecutor.setCorePoolSize(3);
        taskExecutor.initialize();
        taskExecutor.setAwaitTerminationSeconds(10);

        Executor mdcExecutor = new MdcExecutor(taskExecutor);

        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();

        controller = new HelloWorldController(detector, mdcExecutor, restTemplate);
        controller.setLogger(logger);
    }

    @After
    public void tearDown() {
        taskExecutor.shutdown();
        MDC.clear();
        Clock.clear();
    }

    @Test
    public void printHello() {
        assertThat(controller.sayHello("World"), is("Hello World"));
    }

    @Test
    public void runsACommand() {
        server.expect(ExpectedCount.manyTimes(), requestTo(EXAMPLE_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("example", MediaType.TEXT_PLAIN));

        controller.executesCommand();
        taskExecutor.shutdown();

        verify(logger).info("response is {}", "example");
    }

    @Test
    public void loggingIgnored() {
        assertThat(controller.ignoreLogging(), is("Add URL Patterns to the Configurer by setting " +
                "`excludePathPatterns` to ignore logging"));
    }

    @Test
    public void saysCheese() {
        assertThat(controller.sayCheese(), is("Cheese"));
    }

    @Test
    public void returnsUserRole() {
        String actual = controller.userRole("user", "role");

        assertThat(actual, is("user, role"));
    }
}
