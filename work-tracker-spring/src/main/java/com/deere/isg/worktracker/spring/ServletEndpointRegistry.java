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

import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;

class ServletEndpointRegistry {
    private static EndpointTrie trie = new EndpointTrie();

    public static void populate(HttpServletRequest request){
        if (request == null){
            return;
        }

        request.getServletContext()
                .getServletRegistrations()
                .forEach((servletName, registration) -> addMappings(registration));
    }

    public static boolean contains(String value){
        return trie.isEqual(value);
    }

    static void populate(String value){
        trie.add(value);
    }

    static void clear(){
        trie = new EndpointTrie();
    }

    private static void addMappings(ServletRegistration registration) {
        registration.getMappings().forEach(ServletEndpointRegistry::populate);
    }

    private static class EndpointTrie{
        private ConcurrentHashMap<Character, EndpointTrie> map = new ConcurrentHashMap<>();
        private boolean isEnd = false;

        void add(String value){
            if (isInvalidEndpoint(value)){
                return;
            }

            EndpointTrie current = this;
            for (char ch: value.toCharArray()){
                if (!current.map.containsKey(ch)) {
                    current.map.put(ch, new EndpointTrie());
                }
                current = current.map.get(ch);
            }
            current.isEnd = true;
        }

        boolean isEqual(String value){
            if (value == null){
                return false;
            }
            char[] characters = value.toCharArray();
            EndpointTrie current = this;
            for (char ch: characters){
                if (!current.map.containsKey(ch)){
                    return false;
                }
                current = current.map.get(ch);
            }
            return current.isEnd;
        }

        private boolean isInvalidEndpoint(String value) {
            return value == null || isRoot(value) || !isEndpoint(value);
        }

        private boolean isRoot(String value) {
            return value.trim().equals("/");
        }

        private boolean isEndpoint(String value) {
            return value.startsWith("/");
        }
    }

}
