package com.deere.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class ContextServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContextServlet.class);

    @Override
    @SuppressWarnings("unchecked")
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        MDC.put("short_id", "some value");
        someBusinessLogic();

        response.setContentType("text/html");
        try (PrintWriter out = response.getWriter()) {
            out.print("You added some data in the MDC");
        }
    }

    private void someBusinessLogic() {
        LOGGER.info("This log will contain the context data");
    }
}
