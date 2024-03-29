/**
 * Copyright 2018-2023 Deere & Company
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

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Optional;

public class HttpWork extends Work {
    public static final String REMOTE_ADDRESS = "remote_address";
    public static final String PATH = "path";
    public static final String REMOTE_USER = "remote_user";
    public static final String SESSION_ID = "session_id";
    public static final String STATUS_CODE = "status_code";
    public static final String ACCEPT_HEADER = "Accept";
    public static final String ACCEPT = "accept";

    private String remoteAddress;
    private String path;
    private String remoteUser;
    private String sessionId;
    private String acceptHeader;

    public HttpWork(ServletRequest request) {
        if (request != null) {
            setRemoteAddress(request.getRemoteAddr());

            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                setSessionId(Optional.ofNullable(httpRequest.getSession(false))
                        .map(HttpSession::getId)
                        .orElse(null)
                );
                setRemoteUser(httpRequest.getRemoteUser());
                setService(httpRequest.getMethod() + " " + httpRequest.getServletPath());
                setAcceptHeader(httpRequest.getHeader(ACCEPT_HEADER));
            }
        }
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = addToMDC(REMOTE_ADDRESS, remoteAddress);
    }

    @Override
    public String getService() {
        return path;
    }

    public void setService(String path) {
        this.path = addToMDC(PATH, path);
    }

    public String getRemoteUser() {
        return remoteUser;
    }

    public void setRemoteUser(String remoteUser) {
        this.remoteUser = addToMDC(REMOTE_USER, remoteUser);
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = addToMDC(SESSION_ID, sessionId);
    }

    public String getAcceptHeader() {
        return acceptHeader;
    }

    public void setAcceptHeader(String acceptHeader) {
        this.acceptHeader = addToMDC(ACCEPT, acceptHeader);
    }

    @Override
    public String getExtraInfo() {
        return getAcceptHeader();
    }

    /**
     * If you plan to have user authentication in your web application,
     * you should extend {@link HttpWork} and override {@link #updateUserInformation(HttpServletRequest)}
     * to add the user information to the {@link org.slf4j.MDC}.
     * Make sure to use {@link HttpWork#setRemoteUser(String)} to update
     * {@link HttpWork#REMOTE_USER} for FloodSensor to be
     * effective against `too many same user requests`.
     * <p>
     * <b>WARNING:</b> Please do not provide any password!
     *
     * @param request Can be useful if using JWT or header authentication
     */
    public void updateUserInformation(HttpServletRequest request) {

    }
}
