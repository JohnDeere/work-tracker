/**
 * Copyright 2021 Deere & Company
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


package com.deere.isg.worktracker.spring;

import com.deere.isg.worktracker.ZombieDetector;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.io.IOException;

import static com.deere.isg.worktracker.servlet.WorkContextListener.ZOMBIE_ATTR;

public class ZombieHttpInterceptor implements ClientHttpRequestInterceptor, ServletContextAware {

    private ZombieDetector detector;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        if (detector != null) {
            detector.killRunaway();
        }
        return execution.execute(request, body);
    }

    @Override
    public void setServletContext(ServletContext context) {
        detector = (ZombieDetector) context.getAttribute(ZOMBIE_ATTR);
    }
}
