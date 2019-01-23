/**
 * Copyright 2019 Deere & Company
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.deere.example;

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.servlet.MdcExecutor;
import com.deere.isg.worktracker.spring.SpringWork;
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

import javax.servlet.ServletContext;

import static com.deere.example.HelloWorldController.EXAMPLE_URL;
import static com.deere.isg.worktracker.servlet.WorkContextListener.OUTSTANDING_ATTR;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(MockitoJUnitRunner.class)
public class HelloWorldControllerTest {

    @Mock
    private OutstandingWork<SpringWork> outstanding;
    @Mock
    private ServletContext servletContext;
    @Mock
    private Logger logger;

    private HelloWorldController controller;
    private ThreadPoolTaskExecutor taskExecutor;
    private MockRestServiceServer server;

    @Before
    public void setUp() {
        when(servletContext.getAttribute(OUTSTANDING_ATTR)).thenReturn(outstanding);

        WorkContext context = new WorkContext();
        context.setServletContext(servletContext);

        taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setMaxPoolSize(5);
        taskExecutor.setCorePoolSize(3);
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(3);
        taskExecutor.initialize();

        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();

        controller = new HelloWorldController(context, new MdcExecutor(taskExecutor), restTemplate);
        controller.setLogger(logger);
    }

    @After
    public void tearDown() {
        taskExecutor.shutdown();
        MDC.clear();
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
    public void returnsHelloWorld() {
        assertThat(controller.sayHello(), is("Hello World"));
    }

    @Test
    public void throwsAnException() {
        try {
            controller.throwsError();
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), is("This is Charlie"));
        }
    }

    @Test
    public void returnsUserRole() {
        String actual = controller.userRole("user", "role");

        assertThat(actual, is("user, role"));
    }
}
