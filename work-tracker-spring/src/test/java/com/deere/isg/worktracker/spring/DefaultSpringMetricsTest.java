/**
 * Copyright 2019 Deere & Company
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.deere.isg.worktracker.spring;

import com.deere.clock.Clock;
import com.deere.isg.worktracker.MetricEngine;
import com.deere.isg.worktracker.MetricEngine.CountMetric;
import com.deere.isg.worktracker.MetricEngine.MetricList;
import com.deere.isg.worktracker.MetricEngine.PercentageMetric;
import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.servlet.WorkContextListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultSpringMetricsTest {

    private static final Random RANDOM = new Random();
    private SpringWorkFilter filter;

    public static <T> T random(T... values) {
        return values[RANDOM.nextInt(values.length)];
    }

    @Before
    public void setup() throws ServletException {
        MetricEngine<SpringWork> engine = new DefaultSpringMetrics<>()
                .tag("endpoint", SpringWork::getEndpoint)
                .tag("accept", SpringWork::getAcceptHeader)
                .build(this::writeLog);

        filter = new SpringWorkFilter() {
            @Override
            protected void postProcess(ServletRequest request, ServletResponse response, SpringWork payload) {
                engine.postProcess(payload);
            }

            @Override
            public void init(FilterConfig filterConfig) throws ServletException {
                super.init(filterConfig);
                engine.init(getOutstanding());
            }
        };

        OutstandingWork<SpringWork> outstandingWork = new OutstandingWork<>();
        FilterConfig filterConfig = mock(FilterConfig.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getAttribute(WorkContextListener.OUTSTANDING_ATTR)).thenReturn(outstandingWork);

        filter.init(filterConfig);
    }

    @After
    public void tearDown() {
        Clock.clear();
    }

    @Test
    public void second() throws IOException, ServletException {
//        Clock.freeze();
        for (int i = 0; i < 50; i++) {
            runOnce();
        }
    }

    private void runOnce() throws IOException, ServletException {
        MockHttpServletRequest request = MockMvcRequestBuilders
                .get(random("/hello", "/foo", "/bar", "/baz", "/zap"))
                .accept(random("text/html", "application/json", "application/xml"))
                .principal(() -> random("fred", "george", "charlie", "ron", "ginny"))
                .buildRequest(new MockServletContext());

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, ((req, res) -> {
            ((HttpServletResponse) res).setStatus(random(200, 500, 429, 403, 200, 402, 200));
            int elapsed = RANDOM.nextInt(5000);
            try {
                req.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Collections.emptyMap());
                Thread.sleep(elapsed);
            } catch (InterruptedException e) {
                // ignore
            }
//            Clock.freeze(Clock.now().plusMillis(elapsed));
        }));
    }

    private void writeLog(MetricEngine.Bucket bucket) {
        String orig = new MetricJSONBuilder().marshall(bucket);
//        System.out.println(orig);
        System.out.println(getPretty(orig));
        validate(bucket);
    }

    private void validate(MetricEngine.Bucket bucket) {
        bucket.findMetric("count", CountMetric.class)
                .map(m -> m.getValue())
                .ifPresent(count ->
                {
                    for (MetricEngine.Metric metric : bucket.getMetrics()) {
                        if (metric instanceof MetricList) {
                            MetricList list = (MetricList) metric;
                            validate(list, count);
                        }
                    }
                });
    }

    private void validate(MetricList list, long parentCount) {
        int total = list.stream().map(set -> set.findMetric("count", CountMetric.class)
                .map(CountMetric::getValue).map(count -> {
                    assertThat(set.toString(), (long) count, Matchers.lessThanOrEqualTo(parentCount));
                    assertThat(set.toString(), count, Matchers.greaterThanOrEqualTo(1));
                    validatePercent(parentCount, set, count);
                    return count;
                })).filter(Optional::isPresent).map(Optional::get)
                .mapToInt(Integer::intValue).sum();
        assertEquals(list.getKey(), parentCount, total);
    }

    private void validatePercent(double parentCount, MetricEngine.MetricSet set, Integer count) {
        Optional<Double> percent = set.findMetric("percent", PercentageMetric.class)
                .map(PercentageMetric::getValue);
        if(percent.isPresent()) {
            double prct = percent.get();
            double expected = 100* ((double)count / parentCount);
            assertEquals(set.toString(), prct, expected, .001);
        }
    }

    private String getPretty(String ugly) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(ugly);
        return gson.toJson(je);
    }
}
