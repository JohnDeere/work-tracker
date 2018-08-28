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
