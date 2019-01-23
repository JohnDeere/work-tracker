/**
 * Copyright 2019 Deere & Company
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
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
            excludePathPatterns("/favicon.ico", "/ignore");
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
