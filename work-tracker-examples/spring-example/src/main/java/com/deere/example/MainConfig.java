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

package com.deere.example;

import com.deere.isg.worktracker.servlet.MdcExecutor;
import com.deere.isg.worktracker.spring.ZombieExceptionHandler;
import com.deere.isg.worktracker.spring.ZombieHttpInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.concurrent.Executor;

@EnableWebMvc
@Configuration
public class MainConfig {
    @Bean
    public ZombieExceptionHandler zombieExceptionHandler() {
        return new ZombieExceptionHandler();
    }

    @Bean
    public RestTemplate zombieRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new ZombieHttpInterceptor());
        return restTemplate;
    }

    @Bean
    public Executor mdcExecutor(Executor executor) {
        return new MdcExecutor(executor);
    }
}