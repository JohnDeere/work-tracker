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

package integration;

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.servlet.ConnectionLimits;
import com.deere.isg.worktracker.servlet.HttpFloodSensor;
import com.deere.isg.worktracker.servlet.RequestBouncerFilter;
import com.deere.isg.worktracker.spring.SpringWork;
import com.deere.isg.worktracker.spring.SpringWorkFilter;
import integration.helpers.Conditions;
import integration.helpers.MockController;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.Filter;
import javax.servlet.ServletException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static com.deere.isg.worktracker.servlet.WorkContextListener.FLOOD_SENSOR_ATTR;
import static com.deere.isg.worktracker.servlet.WorkContextListener.OUTSTANDING_ATTR;

import static org.assertj.core.api.Assertions.assertThat;



import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class FloodSensorTest {
    private static final int TOTAL_LIMIT = 8;
    private static final int SERVICE_LIMIT = 6;
    private static final int USER_LIMIT = 4;
    private static final int SESSION_LIMIT = 2;

    private static final String TYPE_NAME = "typeName";
    private static final String TEST_USER = "test_user";

    private static final String REJECTED_MESSAGE = "Request rejected to protect JVM from too many requests ";
    private static final String TOTAL_MESSAGE = REJECTED_MESSAGE + "total";

    private static final String FROM_SAME = REJECTED_MESSAGE + "from same ";
    private static final String SESSION_MESSAGE = FROM_SAME + "session";
    private static final String USER_MESSAGE = FROM_SAME + "user";
    private static final String SERVICE_MESSAGE = FROM_SAME + "service";

    private ConnectionLimits<SpringWork> limit;
    private MockMvc mockMvc;
    private Logger logger;
    private MockHttpSession session;

    @Before
    public void setUp() {
        limit = reducedLimits();
        logger = mock(Logger.class);
        session = new MockHttpSession();
    }

    @Test
    public void limitsInCorrectOrder() {
        Conditions.assertConditionTypes(limit.getConnectionLimits());
    }

    @Test
    public void tooManySameSession() throws Exception {
        buildMockMvc(SESSION_LIMIT, session.getId(), false, false);
        MockHttpServletRequestBuilder requestBuilder = get("/").session(session);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        assertTooMany(result, SESSION_MESSAGE);
    }

    @Test
    public void tooManySameUser() throws Exception {
        buildMockMvc(USER_LIMIT, null, false, false);
        MockHttpServletRequestBuilder requestBuilder = get("/").with(request -> {
            request.setRemoteUser(TEST_USER);
            return request;
        });

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        assertTooMany(result, USER_MESSAGE);
    }

    @Test
    public void tooManyService() throws Exception {
        buildMockMvc(SERVICE_LIMIT, null, true, false);

        MockHttpServletRequestBuilder requestBuilder = get("/").with(request -> {
            request.setServletPath("/");
            return request;
        });

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        assertTooMany(result, SERVICE_MESSAGE);
    }

    @Test
    public void tooManyTotal() throws Exception {
        buildMockMvc(TOTAL_LIMIT, null, true, true);
        MockHttpServletRequestBuilder requestBuilder = get("/");

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
        assertTooMany(result, TOTAL_MESSAGE);
    }

    private void assertTooMany(MvcResult result, String message) throws UnsupportedEncodingException {
        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());

        String content = response.getContentAsString();
        assertThat(content).isEmpty();
        verify(logger).warn(eq(message), (Object[]) any());
    }

    private void buildMockMvc(int threshold, String sessionId, boolean randomUser, boolean randomService) throws ServletException {
        OutstandingWork<SpringWork> outstanding = new OutstandingWork<>();
        IntStream.range(0, threshold).forEach(i -> outstanding.create(createSpringWork(sessionId, randomUser, randomService)));
        MockFloodSensor floodSensor = new MockFloodSensor(outstanding, limit);

        MockFilterConfig filterConfig = getMockFilterConfig(limit, outstanding, floodSensor);

        Filter workFilter = new SpringWorkFilter();
        workFilter.init(filterConfig);

        Filter requestBouncerFilter = new RequestBouncerFilter();
        requestBouncerFilter.init(filterConfig);

        mockMvc = MockMvcBuilders.standaloneSetup(new MockController(outstanding))
                .addFilters(workFilter, requestBouncerFilter)
                .build();
    }

    private SpringWork createSpringWork(String sessionId, boolean randomUser, boolean randomService) {
        SpringWork work = new SpringWork(null);
        work.setSessionId(createRandomOrDefault(sessionId == null, sessionId));
        work.setRemoteUser(createRandomOrDefault(randomUser, TEST_USER));
        work.setService(createRandomOrDefault(randomService, "GET /"));
        return work;
    }

    private String createRandomOrDefault(boolean random, String defaultValue) {
        return random
                ? String.valueOf(ThreadLocalRandom.current().nextInt(10))
                : defaultValue;
    }

    private ConnectionLimits<SpringWork> reducedLimits() {
        ConnectionLimits<SpringWork> limit = new ConnectionLimits<>(true);
        limit.updateLimit(SESSION_LIMIT, ConnectionLimits.SESSION);
        limit.updateLimit(USER_LIMIT, ConnectionLimits.USER);
        limit.updateLimit(SERVICE_LIMIT, ConnectionLimits.SERVICE);
        limit.updateLimit(TOTAL_LIMIT, ConnectionLimits.TOTAL);
        return limit;
    }

    private MockFilterConfig getMockFilterConfig(ConnectionLimits<SpringWork> limit, OutstandingWork<SpringWork> outstanding, MockFloodSensor floodSensor) {
        MockFilterConfig config = new MockFilterConfig();
        config.getServletContext().setAttribute(OUTSTANDING_ATTR, outstanding);
        config.getServletContext().setAttribute(FLOOD_SENSOR_ATTR, floodSensor);
        return config;
    }

    private class MockFloodSensor extends HttpFloodSensor<SpringWork> {
        MockFloodSensor(OutstandingWork<SpringWork> outstanding, ConnectionLimits<SpringWork> connectionLimits) {
            super(outstanding, connectionLimits);
            setLogger(logger);
        }
    }
}
