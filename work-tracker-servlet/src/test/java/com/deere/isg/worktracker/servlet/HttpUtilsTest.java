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

import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.deere.isg.worktracker.servlet.HttpUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpUtilsTest {
    private static final String TEST_URL = "http://localhost:8080/query";
    private static final String TEST_NO_HOST_URL = "/query";
    private static final String TEST_QUERY_FULL = "first_name=John&&last_name=Doe";
    private static final String TEST_QUERY = "first_name=John";
    private HttpServletRequest request;

    @Before
    public void setUp() {
        request = mock(HttpServletRequest.class);
    }

    @Test
    public void nullUrlReturnsNull() {
        assertThat(getRequestUrl(request)).isNull();
    }

    @Test
    public void returnsUrl() {
        setupRequestURL();

        assertThat(getRequestUrl(request)).isEqualTo(TEST_URL);
    }

    @Test
    public void returnsUrlWithQueryString() {
        setupRequestURL();
        setupRequestQueryString();

        assertThat(getRequestUrl(request)).isEqualTo(TEST_URL + "?" + TEST_QUERY);
    }

    @Test
    public void returnsFullUrlWithQueryStringFull() {
        setupRequestURL();
        setupRequestQueryStringFull();

        assertThat(getRequestUrl(request)).isEqualTo(TEST_URL + "?" + TEST_QUERY_FULL);
    }

    @Test
    public void encodedValueIsDecoded() throws UnsupportedEncodingException {
        String url = decodeUrl(getEncodedUrl(TEST_URL));

        assertThat(url).isEqualTo(TEST_URL);
    }

    @Test
    public void decodingUrl() {
        String url = decodeUrl("http://www.example.com/some%20query");

        assertThat(url).isEqualTo("http://www.example.com/some query");
    }

    @Test
    public void nullValueReturnsNullDecoded() {
        assertThat(decodeUrl(null)).isNull();
    }

    @Test
    public void encodedUrlIsDecoded() throws UnsupportedEncodingException {
        setupEncodedUrlFull();

        assertThat(getRequestUrl(request)).isEqualTo(TEST_URL);
    }

    @Test
    public void trimsHost() {
        setupRequestURL();
        setupRequestQueryString();
        String uri = getRequestUri(request);

        assertThat(uri).isEqualTo(TEST_NO_HOST_URL + "?" + TEST_QUERY);
    }

    @Test
    public void hasDecodedUri() throws UnsupportedEncodingException {
        setupEncodedUrlFull();
        setupRequestQueryString();

        String uri = getRequestUri(request);
        assertThat(uri).isEqualTo(TEST_NO_HOST_URL + "?" + TEST_QUERY);
    }

    @Test
    public void ignoresDecodeFailures() {
        String badURL = "http://example.com/?foo=%uf";
        assertThat(decodeUrl(badURL)).isEqualTo(badURL);
    }

    private String getEncodedUrl(String url) throws UnsupportedEncodingException {
        String encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.name());
        assertThat(encodedUrl).isNotEqualTo(url);
        return encodedUrl;
    }

    private void setupRequestQueryString() {
        when(request.getQueryString()).thenReturn(TEST_QUERY);
    }

    private void setupRequestQueryStringFull() {
        when(request.getQueryString()).thenReturn(TEST_QUERY_FULL);
    }

    private void setupRequestURL() {
        when(request.getRequestURL()).thenReturn(new StringBuffer(TEST_URL));
        when(request.getRequestURI()).thenReturn(TEST_NO_HOST_URL);
    }

    private void setupEncodedUrlFull() throws UnsupportedEncodingException {
        when(request.getRequestURL()).thenReturn(new StringBuffer(getEncodedUrl(TEST_URL)));
        when(request.getRequestURI()).thenReturn(getEncodedUrl(TEST_NO_HOST_URL));
    }
}
