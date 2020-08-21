/**
 * Copyright 2020 Deere & Company
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


package integration.helpers;

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.servlet.WorkConfig;
import com.deere.isg.worktracker.spring.SpringWork;
import com.deere.isg.worktracker.spring.ZombieHttpInterceptor;
import com.deere.isg.worktracker.spring.boot.WorkTrackerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.ServletRequest;
import java.util.function.Function;

import static com.deere.isg.worktracker.servlet.WorkContextListener.*;

@Configuration
public class MockWorkConfiguration extends WorkTrackerConfigurer<SpringWork> {

    public MockWorkConfiguration() {
        MockServletContext context = new MockServletContext();
        WorkConfig<SpringWork> config = new WorkConfig.Builder<SpringWork>(new OutstandingWork<>())
                .withZombieDetector()
                .withHttpFloodSensor()
                .build();
        context.setAttribute(OUTSTANDING_ATTR, config.getOutstanding());
        context.setAttribute(FLOOD_SENSOR_ATTR, config.getFloodSensor());
        context.setAttribute(ZOMBIE_ATTR, config.getDetector());
        setServletContext(context);
    }

    @Override
    public Function<ServletRequest, SpringWork> workFactory() {
        return SpringWork::new;
    }

    @Bean
    public RestTemplate zombieRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new ZombieHttpInterceptor());
        return restTemplate;
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        UrlPathHelper urlPathHelper = new UrlPathHelper();
        urlPathHelper.setRemoveSemicolonContent(false);
        configurer.setUrlPathHelper(urlPathHelper);
    }
}
