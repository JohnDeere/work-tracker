package com.deere.isg.worktracker.servlet;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public interface HttpPostProcessor<W extends HttpWork> {
    void postProcess(ServletRequest request, ServletResponse response, W payload);
}
