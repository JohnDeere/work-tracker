/**
 * Copyright 2018 Deere & Company
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.deere.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@RestController
public class HelloWorldController {
    protected static final String EXAMPLE_URL = "https://www.example.com";
    private final WorkContext context;
    private final Executor mdcExecutor;
    private final RestTemplate restTemplate;
    private Logger logger = LoggerFactory.getLogger(HelloWorldController.class);

    @Autowired
    public HelloWorldController(WorkContext context,
                                @Qualifier("mdcExecutor") Executor mdcExecutor,
                                RestTemplate restTemplate) {
        this.context = context;
        this.mdcExecutor = mdcExecutor;
        this.restTemplate = restTemplate;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String sayHello() {
        return "Hello World";
    }

    @RequestMapping(value = "/executor", method = RequestMethod.GET)
    public String executesCommand() {
        mdcExecutor.execute(() -> {
            ResponseEntity<String> resp = restTemplate.getForEntity(EXAMPLE_URL, String.class);
            logger.info("response is {}", resp.getBody());
        });
        return "Check your console logs";
    }

    @RequestMapping(value = "/zombie", method = RequestMethod.GET)
    @SuppressWarnings("Duplicates")
    public void walkingDead() {
        try {
            while (true) {
                // if thread exceeds 5 minutes, it will kill it immediately
                context.getDetector().killRunaway();
                Thread.sleep(TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS));
            }
        } catch (InterruptedException e) {
            logger.error("Thread interrupted", e);
        }
    }

    @GetMapping("/user/{id}/role/{role}")
    @SuppressWarnings("Duplicates")
    public String userRole(@PathVariable("id") String user,
            @PathVariable("role") String role) {
        return user + ", " + role;
    }

    @RequestMapping(value = "/exceptional", method = RequestMethod.GET)
    public String throwsError() {
        throw new RuntimeException("This is Charlie");
    }

    void setLogger(Logger logger) {
        this.logger = logger;
    }

}
