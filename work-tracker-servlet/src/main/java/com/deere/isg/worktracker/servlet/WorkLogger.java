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


package com.deere.isg.worktracker.servlet;

import com.deere.isg.worktracker.Work;
import net.logstash.logback.argument.StructuredArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.deere.isg.worktracker.Work.REQUEST_URL;
import static com.deere.isg.worktracker.servlet.HttpUtils.getRequestUri;
import static com.deere.isg.worktracker.servlet.HttpWork.STATUS_CODE;
import static net.logstash.logback.argument.StructuredArguments.keyValue;

/**
 * This class exists to ensure that start and end logs are easily searchable in Elasticsearch.
 * Applications should have no reason to use this logger.
 */
public class WorkLogger {
    private static WorkLogger instance = null;
    private Logger logger = LoggerFactory.getLogger(WorkLogger.class);
    private List<String> excludeUrls = new ArrayList<>();

    private WorkLogger() {
    }

    public static WorkLogger getLogger() {
        if (instance == null) {
            instance = new WorkLogger();
        }

        return instance;
    }

    protected void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void logStart(HttpServletRequest request, Work current) {
        if (shouldExclude(request)) {
            return;
        }
        final String url = getRequestUri(request);

        List<StructuredArgument> startInfo = current != null ? current.getStartInfo() : new ArrayList<>();
        startInfo.add(keyValue(REQUEST_URL, url));

        logger.info("Start of Request: url=" + url, startInfo.toArray());
    }

    public void logEnd(HttpServletRequest request, HttpServletResponse response, Work payload) {
        if (shouldExclude(request)) {
            return;
        }
        final String url = getRequestUri(request);
        final int statusCode = response.getStatus();

        List<StructuredArgument> endInfo = payload != null ? payload.getEndInfo() : new ArrayList<>();
        endInfo.add(keyValue(STATUS_CODE, statusCode));
        endInfo.add(keyValue(REQUEST_URL, url));

        logger.info("End of Request: status_code=" + statusCode + ", url=" + url, endInfo.toArray());
    }

    public void debug(String message) {
        logger.debug(message);
    }

    public void excludeUrls(String... excludeUrls) {
        if (excludeUrls != null) {
            this.excludeUrls = removeAsterisks(excludeUrls);
        }
    }

    private List<String> removeAsterisks(String[] excludeUrls) {
        return Arrays.stream(excludeUrls)
                .map(u -> u.replaceAll("[\\*]+", ""))
                .filter(u -> !u.isEmpty())
                .filter(u -> !u.equals("/"))
                .collect(Collectors.toList());
    }

    public List<String> getExcludeUrls() {
        return new ArrayList<>(excludeUrls);
    }

    private boolean shouldExclude(HttpServletRequest httpRequest) {
        return excludeUrls.stream().anyMatch(url -> httpRequest.getRequestURI().startsWith(url));
    }
}
