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

import com.deere.isg.worktracker.servlet.MdcExecutor;
import com.deere.isg.worktracker.spring.KeyCleanser;
import com.deere.isg.worktracker.spring.PathMetadataCleanser;
import com.deere.isg.worktracker.spring.ZombieHttpInterceptor;
import com.deere.isg.worktracker.spring.boot.WorkTrackerConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.client.RestTemplate;

import javax.servlet.ServletRequest;
import java.util.concurrent.Executor;
import java.util.function.Function;

@SpringBootApplication
@EnableWebSecurity
public class ExampleApplication {

    public static void main(String[] arg) {
        SpringApplication.run(ExampleApplication.class, arg);
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new ZombieHttpInterceptor());
        return restTemplate;
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.initialize();
        return executor;
    }

    @Bean
    public Executor mdcTaskExecutor(@Qualifier("taskExecutor") TaskExecutor executor) {
        return new MdcExecutor(executor);
    }

    @Configuration
    public class WorkTrackerConfig extends WorkTrackerConfigurer<UserSpringWork> {

        public WorkTrackerConfig() {
            setLimit(30);
        }

        @Override
        public KeyCleanser keyCleanser() {
            PathMetadataCleanser cleanser = new PathMetadataCleanser();
            cleanser.addStandard("short_id", "standardized_id");
            cleanser.setTransformFunction(key -> key + "_suffix");
            cleanser.addBanned("banned", "good_id");
            return cleanser;
        }

        @Override
        public Function<ServletRequest, UserSpringWork> workFactory() {
            return UserSpringWork::new;
        }
    }

    @Configuration
    public class SecurityConfig extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.inMemoryAuthentication()
                    .withUser("user").password("password").roles("USER");
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .authorizeRequests()
                    .antMatchers("/cheese/**").hasRole("USER").anyRequest().authenticated()
                    .antMatchers("/health/**").permitAll().anyRequest().anonymous()
                    .antMatchers("/**").permitAll().anyRequest().anonymous()
                    .and()
                    .formLogin().and()
                    .httpBasic();
        }
    }
}
