package com.deere.example;

import com.deere.isg.worktracker.servlet.MdcExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ExecutorExampleServlet extends HttpServlet {
    private ExecutorService service = Executors.newFixedThreadPool(3);
    private Executor executor = new MdcExecutor(service);
    private Logger logger = LoggerFactory.getLogger(ExecutorExampleServlet.class);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        executor.execute(() -> logger.info("some background running task"));
        resp.getWriter().print("Check your console logs");
    }

    @Override
    public void destroy() {
        service.shutdown();
        try {
            service.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("Could not complete task", e);
        }
        super.destroy();
    }

    void setLogger(Logger logger) {
        this.logger = logger;
    }
}
