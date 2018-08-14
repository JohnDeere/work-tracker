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


package integration;

import com.deere.example.spring.ExampleApplication;
import com.deere.isg.worktracker.servlet.WorkConfig;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { ExampleApplication.class }, webEnvironment = RANDOM_PORT)
public class WorkTrackerIntTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @SpyBean
    private WorkConfig<?> workConfig;

    @Test
    public void basicConfigurationTest() {
        assertThat("Uses work tracking", workConfig.getOutstanding(), notNullValue());
        assertThat("Detects Zombies", workConfig.getDetector(), notNullValue());
        assertThat("Has DoS protection", workConfig.getFloodSensor(), notNullValue());
    }

    @Test
    public void endToEndTest() {
        String body = this.restTemplate.getForObject("/health/outstanding", String.class);
        assertThat(body, Matchers.not(Matchers.containsString("\"status\":500")));
        assertThat(body, Matchers.containsString("<td>GET /health/outstanding</td>"));
    }
}
