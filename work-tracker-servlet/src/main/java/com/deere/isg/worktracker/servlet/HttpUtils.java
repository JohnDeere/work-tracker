/**
 * Copyright 2021 Deere & Company
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

import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;

import static com.deere.isg.worktracker.StringUtils.isBlank;
import static com.deere.isg.worktracker.StringUtils.isNotBlank;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class HttpUtils {

    private HttpUtils() {

    }

    public static String getRequestUrl(HttpServletRequest request) {
        return getUrl(request, decodeUrl(request.getRequestURL()));
    }

    public static String getRequestUri(HttpServletRequest request) {
        return getUrl(request, decodeUrl(request.getRequestURI()));
    }

    public static String decodeUrl(String encodedUrl) {
        try {
            return URLDecoder.decode(encodedUrl, UTF_8.name());
        } catch (Exception ignore) {
            return encodedUrl;
        }
    }

    private static String getUrl(HttpServletRequest request, String url) {
        if (isBlank(url)) {
            return null;
        }

        String queryString = decodeUrl(request.getQueryString());
        String query = isNotBlank(queryString) ? "?" + queryString : "";
        return url + query;
    }

    private static String decodeUrl(StringBuffer encodedUrl) {
        return decodeUrl(encodedUrl != null ? encodedUrl.toString() : null);
    }

}
