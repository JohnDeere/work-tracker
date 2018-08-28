package com.deere.example;

import com.deere.isg.worktracker.ZombieDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.deere.isg.worktracker.servlet.WorkContextListener.ZOMBIE_ATTR;

public class ZombieServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZombieServlet.class);

    @Override
    @SuppressWarnings("Duplicates")
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ZombieDetector detector = (ZombieDetector) request.getServletContext().getAttribute(ZOMBIE_ATTR);
        try {
            while (true) {
                // if thread exceeds 5 minutes, it will kill it immediately
                detector.killRunaway();
                Thread.sleep(TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS));
            }
        } catch (InterruptedException e) {
            LOGGER.error("Thread interrupted", e);
        }
    }
}
