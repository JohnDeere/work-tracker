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
import com.deere.isg.worktracker.ZombieDetector;
import com.deere.isg.worktracker.servlet.HttpFloodSensor;
import com.deere.isg.worktracker.servlet.RequestBouncerFilter;
import com.deere.isg.worktracker.servlet.ZombieFilter;
import com.deere.isg.worktracker.spring.KeyCleanser;
import com.deere.isg.worktracker.spring.PathMetadataCleanser;
import com.deere.isg.worktracker.spring.SpringWork;
import com.deere.isg.worktracker.spring.SpringWorkPostAuthFilter;
import com.deere.isg.worktracker.spring.ZombieExceptionHandler;
import com.deere.isg.worktracker.spring.boot.SpringBootWorkFilter;
import integration.helpers.MockWorkConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import javax.servlet.Filter;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = MockWorkConfiguration.class)
public class WorkTrackerConfigurerTest {
    @Autowired
    private OutstandingWork<? extends SpringWork> outstanding;
    @Autowired
    private ZombieDetector detector;
    @Autowired
    private HttpFloodSensor<? extends SpringWork> floodSensor;
    @Autowired
    private ZombieExceptionHandler handler;
    @Autowired
    private Filter springWorkFilter;
    @Autowired
    private Filter requestBouncerFilter;
    @Autowired
    private Filter zombieFilter;
    @Autowired
    private Filter authFilter;
    @Autowired
    @Qualifier("zombieRestTemplate")
    private RestTemplate zombieRestTemplate;
    @Autowired
    private KeyCleanser keyCleanser;
    @Autowired
    private ApplicationContext context;
    @Autowired
    private MockWorkConfiguration configuration;

    @Test
    public void contextLoads() {
        assertThat(context).isNotNull();
        assertThat(configuration.connectionLimits()).isNull();
        assertThat(outstanding).isNotNull();
        assertThat(detector).isNotNull();
        assertThat(floodSensor).isNotNull();
        assertThat(handler).isNotNull();
        assertThat(springWorkFilter).isNotNull();
        assertThat(requestBouncerFilter).isNotNull();
        assertThat(zombieFilter).isNotNull();
        assertThat(authFilter).isNotNull();
        assertThat(zombieRestTemplate).isNotNull();
        assertThat(keyCleanser).isNotNull();
    }

    @Test
    public void beansExist() {
        assertThat(context.getBeanDefinitionNames()).contains("authFilter", "zombieRestTemplate",
                "workFactory", "workContextListener", "workConfig",
                "outstanding", "zombieDetector", "floodSensor", "zombieExceptionHandler",
                "workHttpServlet", "springWorkFilter", "requestBouncerFilter", "zombieFilter",
                "authFilter", "logbackStatusServlet", "keyCleanser"
        );
    }

    @Test
    public void assertInstances() {
        assertThat(springWorkFilter).isInstanceOf(SpringBootWorkFilter.class);
        assertThat(requestBouncerFilter).isInstanceOf(RequestBouncerFilter.class);
        assertThat(zombieFilter).isInstanceOf(ZombieFilter.class);
        assertThat(authFilter).isInstanceOf(SpringWorkPostAuthFilter.class);
        assertThat(keyCleanser).isInstanceOf(PathMetadataCleanser.class);
    }
}
