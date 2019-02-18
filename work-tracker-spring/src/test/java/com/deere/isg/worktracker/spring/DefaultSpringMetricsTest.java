package com.deere.isg.worktracker.spring;

import com.deere.clock.Clock;
import com.deere.isg.worktracker.MetricEngine;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DefaultSpringMetricsTest {

    private SpringWorkFilter filter;

    @Before
    public void setup() {
        MetricEngine<SpringWork> engine = new DefaultSpringMetrics<>().build(this::writeLog);

        filter = new SpringWorkFilter() {
            @Override
            protected void postProcess(ServletRequest request, ServletResponse response, SpringWork payload) {
                engine.postProcess(payload);
            }
        };
    }

    @Test
    public void second() throws IOException, ServletException {
        Clock.freeze();
        for(int i=0; i<50; i++) {
            runOnce();
        }
    }

    private void runOnce() throws IOException, ServletException {
        MockHttpServletRequest request = MockMvcRequestBuilders
                .get("/hello")
                .accept("text/html")
                .principal(() -> "fred")
                .buildRequest(new MockServletContext());

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, ((req, res) -> {
            ((HttpServletResponse) res).setStatus(200);
            Clock.freeze(Clock.now().plusSeconds(2));
        }));
    }

    private void writeLog(MetricEngine.Bucket bucket) {
        String orig = new MetricJSONBuilder().marshall(bucket);
//        System.out.println(orig);
        System.out.println(getPretty(orig));
    }

    private String getPretty(String ugly) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(ugly);
        return gson.toJson(je);
    }
}
