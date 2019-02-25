/**
 * Copyright 2019 Deere & Company
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

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;

class ServletEndpointRegistry {
    private static ConcurrentHashMap.KeySetView<String, Boolean> map = ConcurrentHashMap.newKeySet();

    static void populate(ServletContext context) {
        if (context == null){
            return;
        }

        context.getServletRegistrations().values().forEach(ServletEndpointRegistry::addMappings);
    }

    static void populate(String value){
        if (isInvalidEndpoint(value)) {
            return;
        }
        map.add(value);
    }

    static boolean contains(HttpServletRequest request){
        if (request == null){
            return false;
        }
        return contains(request.getServletPath());
    }

    static boolean contains(String value){
        if (value == null) {
            return false;
        }
        return map.contains(value);
    }

    static void clear(){
        map.clear();
    }

    private static boolean isInvalidEndpoint(String value) {
        return value == null || isRoot(value) || !isEndpoint(value);
    }

    private static boolean isRoot(String value) {
        return value.trim().equals("/");
    }

    private static boolean isEndpoint(String value) {
        return value.startsWith("/");
    }

    private static void addMappings(ServletRegistration registration) {
        registration.getMappings().forEach(ServletEndpointRegistry::populate);
    }

}
