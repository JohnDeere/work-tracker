/**
 * Copyright 2018-2021 Deere & Company
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

import com.deere.isg.worktracker.OutstandingWork;
import com.deere.isg.worktracker.Work;
import com.deere.isg.worktracker.servlet.WorkSummary;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.deere.isg.worktracker.servlet.HttpWork.PATH;
import static com.deere.isg.worktracker.servlet.WorkContextListener.ALL_OUTSTANDING_ATTR;
import static com.deere.isg.worktracker.spring.SpringWork.ENDPOINT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;

public class SpringWorkTest {
    private static final String GET = "GET";
    private static final String ORG = "org";
    private static final String USER = "user";

    private static final String TEST_ORG_VALUE = "/org/{org}/user/{user}";
    private static final String SOME_ATTRIBUTE = "some_attribute";
    private static final String SOME_VALUE = "some_value";
    private static final String SOME_ORG = "some org";
    private static final String SOME_USER = "some user";

    private static final String TEST_URI = "/orgId/some org/user/some user";
    private static final String ORG_USER_URI = "/org/some org/user/some user";
    private final MockKeyCleanser keyCleanser = new MockKeyCleanser();

    private SpringWork work;
    private MockHttpServletRequest request;

    @Before
    public void setUp() {
        MDC.clear();
        request = new MockHttpServletRequest();
        work = new SpringWork(request);
    }

    @Test
    public void nullRequestDoesNotSetRequestURLPattern() {
        SpringWork springWork = new SpringWork(null);

        assertThat(springWork.getRequestURLPattern(), nullValue());

    }

    @Test
    public void setRequestURLPatternReplacesValuesWithKeyName() {
        setupPatternRequest(ORG_USER_URI, getPathMap());

        assertThat(work.getRequestURLPattern(), is(TEST_ORG_VALUE));
    }

    @Test
    public void setRequestURLPatternReplacesEmptySpacesWithKeyName() {
        setupPatternRequest("/org/  some org /user/  some user  ", getPathMap());

        assertThat(work.getRequestURLPattern(), is(TEST_ORG_VALUE));
    }

    @Test
    public void setRequestURLPatternHandlesSpecialCharacters() {
        setupPatternRequest("/org/  {some org}   /user/  {some user} ",
                getPathMap("{some org}", "{some user}"));

        assertThat(work.getRequestURLPattern(), is(TEST_ORG_VALUE));
    }

    @Test
    public void stripContextPath() {
        request.setContextPath("/jd");
        request.setRequestURI("/jd/org/some org/user/ some user ");
        request.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, getPathMap());
        work.setRequestURLPattern(request, null);

        assertThat(work.getRequestURLPattern(), is(TEST_ORG_VALUE));
    }

    @Test
    public void setupEndpointForServlets() {
        String uri = "/test/some/url";
        ServletEndpointRegistry.populate(uri);
        request.setRequestURI(uri);
        request.setContextPath("/someContext");
        request.setServletPath(uri);
        request.setMethod("GET");
        work.setRequestURLPattern(request, null);

        assertThat(work.getRequestURLPattern(), is(uri));
        assertThat(work.getEndpoint(), is("GET " + uri));
    }

    @Test
    public void getEndpointReturnsHttpMethodAndURIPattern() {
        setupPatternRequest(ORG_USER_URI, getPathMap());
        work.setHttpMethod(this.request.getMethod());
        assertNullPathAndEndpoint();
        triggerEndpointWithSpringAttributes(getPathMap());

        assertThat(work.getService(), is("GET " + TEST_ORG_VALUE));
    }

    @Test
    public void endpointAndServiceAddedToMDC() {
        assertNullPathAndEndpoint();

        work.setHttpMethod(GET);
        setupPatternRequest(ORG_USER_URI, getPathMap());
        triggerEndpointWithSpringAttributes(getPathMap());
        assertTestPathAndEndpoint();
    }

    @Test
    public void removesLastSemiColonsRequests() {
        assertNullPathAndEndpoint();

        work.setHttpMethod(GET);
        setupPatternRequest("/org/some org/user/some user;start=0;end=10", getPathMap());
        triggerEndpointWithSpringAttributes(getPathMap());
        assertTestPathAndEndpoint();
    }

    @Test
    public void removesInBetweenSemiColonsRequests() {
        assertNullPathAndEndpoint();

        work.setHttpMethod(GET);
        setupPatternRequest("/org/some org;id=100/user/some user;start=0;end=10", getPathMap());
        triggerEndpointWithSpringAttributes(getPathMap());
        assertTestPathAndEndpoint();
    }

    @Test
    public void moreThanOneInBetweenSemiColonsRequests() {
        assertNullPathAndEndpoint();

        work.setHttpMethod(GET);
        setupPatternRequest("/org/some org;id=100;name=John Deere;time=today/user/some user;start=0;end=10",
                getPathMap()
        );
        triggerEndpointWithSpringAttributes(getPathMap());

        assertTestPathAndEndpoint();
    }

    @Test
    public void preCleanUriAddsEndingSlash() {
        request.setRequestURI("/org");

        assertThat(work.preCleanUri(request), is("/org/"));
    }

    @Test
    public void preCleanUriDecodesEncodedUri() throws UnsupportedEncodingException {
        String encodedUri = URLEncoder.encode("/org", UTF_8.name());
        assertThat(encodedUri, not("/org"));

        request.setRequestURI(encodedUri);

        assertThat(work.preCleanUri(request), is("/org/"));
    }

    @Test
    public void preCleanUriRemoveContextPath() {
        request.setRequestURI("/jd/org");
        request.setContextPath("/jd");

        assertThat(work.preCleanUri(request), is("/org/"));
    }

    @Test
    public void postCleanUriRemovesEndingSlash() {
        assertThat(work.postCleanUri("/org/"), is("/org"));
    }

    @Test
    public void postCleanUriWithoutEndingSlash() {
        assertThat(work.postCleanUri("/org"), is("/org"));
    }

    @Test
    public void httpMethodIsSetupForRequestUrlPattern() {
        assertThat(work.getHttpMethod(), is(""));
        setupPatternRequest(ORG_USER_URI, getPathMap());

        work.setRequestURLPattern(request, null);

        assertThat(work.getHttpMethod(), is(GET));
    }

    @Test
    public void requestHasSomeAttributeAfterWorkaround() {
        work.setupSpringVsFilterOrderingWorkaround(request, keyCleanser);
        request.setAttribute(SOME_ATTRIBUTE, SOME_VALUE);

        assertThat(request.getAttribute(SOME_ATTRIBUTE), is(SOME_VALUE));
    }

    @Test
    public void requestHasUriTemplateAttribute() {
        assertThat(work.getRequestURLPattern(), is("/"));
        assertThat(work.getHttpMethod(), is(""));
        assertThat(work.getService(), is(" "));

        setupPatternRequest(ORG_USER_URI, getPathMap());
        triggerEndpointWithSpringAttributes(getPathMap());

        assertTestPathAndEndpoint();
    }

    @Test
    public void nullSafetyForAddPathVars() {
        work.setupSpringVsFilterOrderingWorkaround(request, null);
        setUriTemplateAttribute(TEST_URI, getPathMap());
        work.setRequestURLPattern(request, null);

        assertThat(MDC.get(ORG), nullValue());
        assertThat(MDC.get(USER), nullValue());
    }

    @Test
    public void addsPathVariablesToMDC() {
        work.setupSpringVsFilterOrderingWorkaround(request, keyCleanser);
        setUriTemplateAttribute(TEST_URI, getPathMap());
        work.setRequestURLPattern(request, keyCleanser);

        assertThat(MDC.get(ORG), is(SOME_ORG));
        assertThat(MDC.get(USER), is(SOME_USER));
    }

    @Test
    public void cleansedKeyUsedInEndpoint() {
        work.setupSpringVsFilterOrderingWorkaround(request, keyCleanser);
        Map<String, String> pathMap = new HashMap<>();
        pathMap.put("User", "SomePerson");
        setUriTemplateAttribute("/users/SomePerson", pathMap);
        work.setRequestURLPattern(request, keyCleanser);
        triggerEndpointWithSpringAttributes(pathMap);
        assertThat(MDC.get("user"), is("SomePerson"));
        assertThat(MDC.get(ENDPOINT), is("GET /users/{user}"));
    }


    @Test
    public void cleansedKeyUsedWithNullValueInEndpoint() {
        work.setupSpringVsFilterOrderingWorkaround(request, keyCleanser);
        Map<String, String> pathMap = new HashMap<>();
        pathMap.put("User", null);
        setUriTemplateAttribute("/users/(null)", pathMap);
        work.setRequestURLPattern(request, keyCleanser);
        triggerEndpointWithSpringAttributes(pathMap);
        assertThat(MDC.get("user"), nullValue());
        assertThat(MDC.getCopyOfContextMap().keySet().contains("user"), is(false));
        assertThat(MDC.get(ENDPOINT), is("GET /users/(null)"));
    }


    @Test
    public void nullTurnsToEmpty() throws ServletException {
        SpringWork work = new SpringWork(null);
        OutstandingWork<SpringWork> outstandingWork = mock(OutstandingWork.class);
        when(outstandingWork.stream()).thenReturn(Stream.of(work));
        ServletConfig config = mock(ServletConfig.class);
        ServletContext context = mock(ServletContext.class);
        when(config.getServletContext()).thenReturn(context);
        when(context.getAttribute(ALL_OUTSTANDING_ATTR)).thenReturn(outstandingWork);

        SpringWorkHttpServlet servlet = new SpringWorkHttpServlet();
        servlet.init(config);

        List<WorkSummary<? extends Work>> workSummaries = servlet.mapOutstandingToSummaryList();
        assertThat(workSummaries, hasSize(1));
        assertThat(workSummaries.get(0).getService(), is(""));
    }

    @Test
    public void nonEmptyEndpoint() throws ServletException {
        SpringWork work =new SpringWork(null);
        work.setService("/test/url");
        OutstandingWork<SpringWork> outstandingWork = mock(OutstandingWork.class);
        when(outstandingWork.stream()).thenReturn(Stream.of(work));
        ServletConfig config = mock(ServletConfig.class);
        ServletContext context = mock(ServletContext.class);
        when(config.getServletContext()).thenReturn(context);
        when(context.getAttribute(ALL_OUTSTANDING_ATTR)).thenReturn(outstandingWork);

        SpringWorkHttpServlet servlet = new SpringWorkHttpServlet();
        servlet.init(config);

        List<WorkSummary<? extends Work>> workSummaries = servlet.mapOutstandingToSummaryList();
        assertThat(workSummaries, hasSize(1));
        assertThat(workSummaries.get(0).getService(), is("/test/url"));
    }


    private void triggerEndpointWithSpringAttributes(Map<String, String> pathMap) {
        HttpServletRequest springRequest = work.setupSpringVsFilterOrderingWorkaround(request, keyCleanser);
        springRequest.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathMap);
    }

    private void assertNullPathAndEndpoint() {
        assertThat(MDC.get(PATH), nullValue());
        assertThat(MDC.get(ENDPOINT), nullValue());
    }

    private void setupPatternRequest(String requestURI, Map<String, String> keyValue) {
        setUriTemplateAttribute(requestURI, keyValue);
        work.setRequestURLPattern(request, null);
    }

    private void assertTestPathAndEndpoint() {
        String actual = "GET " + TEST_ORG_VALUE;
        assertThat(MDC.get(PATH), is(actual));
        assertThat(MDC.get(ENDPOINT), is(actual));
        assertThat(work.getEndpoint(), is(work.getService()));
    }

    private void setUriTemplateAttribute(String requestURI, Map<String, String> keyValue) {
        request.setMethod(GET);
        request.setRequestURI(requestURI);
        request.setAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE, keyValue);
    }

    private Map<String, String> getPathMap() {
        return getPathMap(SOME_ORG, SOME_USER);
    }

    private Map<String, String> getPathMap(String someOrg, String someUser) {
        Map<String, String> keyValue = new HashMap<>();
        keyValue.put(ORG, someOrg);
        keyValue.put(USER, someUser);
        return keyValue;
    }

    private class MockKeyCleanser implements KeyCleanser {
        @Override
        public String cleanse(String key, String value, String requestUri) {
            return key.toLowerCase();
        }
    }
}
