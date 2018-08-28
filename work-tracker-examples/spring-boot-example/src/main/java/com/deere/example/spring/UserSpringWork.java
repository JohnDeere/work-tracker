package com.deere.example.spring;

import com.deere.isg.worktracker.spring.SpringWork;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

public class UserSpringWork extends SpringWork {
    public UserSpringWork(ServletRequest request) {
        super(request);
    }

    @Override
    public void updateUserInformation(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        setRemoteUser(auth.getName());
    }
}
