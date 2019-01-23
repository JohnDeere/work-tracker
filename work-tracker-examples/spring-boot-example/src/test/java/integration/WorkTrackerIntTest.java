/**
 * Copyright 2019 Deere & Company
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
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
