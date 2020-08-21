/**
 * Copyright 2020 Deere & Company
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

import com.deere.isg.worktracker.servlet.HttpWork;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.deere.isg.worktracker.StringUtils.replaceNullWithEmpty;
import static com.deere.isg.worktracker.servlet.HttpUtils.decodeUrl;
import static java.util.regex.Pattern.quote;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;

/**
 * An implementation of {@link HttpWork}. Spring Projects
 * should use this class as their base work (i.e. providing
 * an implementation for {@link #updateUserInformation(HttpServletRequest)})
 * since this provides features that are specific to Spring.
 */
public class SpringWork extends HttpWork {
    public static final String ENDPOINT = "endpoint";

    private static final String WHITE_SPACE_REGEX = "\\s*";
    private static final String MATRIX_REGEX = "(;[\\s\\w\\d]*=[\\s\\w\\d]*)+\\/?";

    private String requestURLPattern;
    private String httpMethod;
    private String endpoint;

    public SpringWork(ServletRequest request) {
        super(request);
        if (request != null) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            setRequestURLPattern(httpRequest, null);
        }
    }

    public String getRequestURLPattern() {
        return requestURLPattern;
    }

    protected void setRequestURLPattern(HttpServletRequest request, KeyCleanser cleanser) {
        setHttpMethod(request.getMethod());
        Map<String, String> pathVariables = getPathVariablesOrElseEmpty(request);
        String requestUri = preCleanUri(request);
        final String originalUri = requestUri;

        if (requestUri != null) {
            for (Map.Entry<String, String> entry : pathVariables.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                String valueRegex = buildValueRegex(value);

                if (cleanser != null) {
                    key = cleanser.cleanse(key, value, originalUri);
                    addToMDC(key, value);
                }
                requestUri = requestUri.replaceAll(valueRegex, "/{" + key + "}/");
            }
            this.requestURLPattern = postCleanUri(requestUri);
        }
        if (ServletEndpointRegistry.contains(request)) {
            setEndpoint();
        }
    }

    public String getEndpoint() {
        return endpoint;
    }

    @Override
    public String getService() {
        String endpoint = getEndpoint();
        if(endpoint != null) {
            return endpoint;
        }
        return super.getService();
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    protected void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    protected String buildValueRegex(String value) {
        return "/" + WHITE_SPACE_REGEX +
                (value != null ?
                    (quote(value) + WHITE_SPACE_REGEX) :
                    "") +
                "/";
    }

    protected String postCleanUri(String requestUri) {
        if (requestUri != null && requestUri.endsWith("/") && requestUri.length() > 1) {
            requestUri = requestUri.substring(0, requestUri.length() - 1);
        }
        return requestUri;
    }

    protected String preCleanUri(HttpServletRequest request) {
        String requestUri = decodeUrl(request.getRequestURI());

        if (requestUri != null) {
            requestUri = requestUri.replace(request.getContextPath(), "");
            requestUri = requestUri.replaceAll(MATRIX_REGEX, "/");

            if (!requestUri.endsWith("/")) {
                requestUri += "/";
            }
        }

        return requestUri;
    }

    protected HttpServletRequest setupSpringVsFilterOrderingWorkaround(HttpServletRequest request, KeyCleanser keyCleanser) {
        return new HttpServletRequestWrapper(request) {
            @Override
            public void setAttribute(String name, Object o) {
                super.setAttribute(name, o);
                if (HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE.equals(name)) {
                    setRequestURLPattern(request, keyCleanser);
                    setEndpoint();
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getPathVariablesOrElseEmpty(HttpServletRequest request) {
        Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        return Optional.ofNullable(pathVariables).orElseGet(Collections::emptyMap);
    }

    private void setEndpoint() {
        this.endpoint = replaceNullWithEmpty(httpMethod) + " " + replaceNullWithEmpty(requestURLPattern);
        addToMDC(ENDPOINT, endpoint);
        setService(endpoint);
    }

}
