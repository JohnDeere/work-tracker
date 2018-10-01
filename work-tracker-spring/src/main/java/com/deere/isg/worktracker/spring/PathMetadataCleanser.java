/**
 * Copyright 2018 Deere & Company
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

import com.deere.isg.worktracker.ContextualTaskDecorator;
import com.deere.isg.worktracker.Work;
import com.deere.isg.worktracker.servlet.HttpWork;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.deere.isg.worktracker.StringUtils.camelToSnakeCase;
import static com.deere.isg.worktracker.StringUtils.isNotBlank;

/**
 * By default, when adding a standardMap to this cleanser,
 * it will convert all CamelCase keys and values to snake_case.
 * This is to make sure that the key is added properly using
 * {@code Work#addToMDC(key, value)}
 * <p>
 * STANDARD is for changing non-standard keys to standard keys
 * Example:
 * test_user_id becomes user_id
 * another_user_id becomes user_id
 * <p>
 * BANNED is for blacklisting keys
 * Example:
 * id becomes unknown_id
 * <p>
 * Setup
 * <pre>{@code
 * &#64;Bean
 * public KeyCleanser keyCleanser() {
 *     return new PathMetadataCleanser(STANDARD_MAP);
 * }
 * }</pre>
 */
public class PathMetadataCleanser implements KeyCleanser {
    private static final String UNKNOWN_ID = "unknown_id";
    private static final String UNKNOWN_TYPE = "unknown_type";

    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String GUID = "guid";
    private static final String TOKEN = "token";

    private static final String UNDERSCORE_ID = "_id";
    private static final String UNDERSCORE_TYPE = "_type";
    private static final String UNDERSCORE_GUID = "_guid";
    private static final String UNDERSCORE_TOKEN = "_token";

    private static final Set<String> RESERVED_MAP;
    private static final String PATH_USER_NAME = "path_user_name";

    static {
        RESERVED_MAP = new HashSet<>();
        RESERVED_MAP.addAll(Arrays.asList(
                Work.THREAD_NAME,
                Work.ELAPSED_MS,
                Work.REQUEST_URL,
                Work.REQUEST_ID,
                Work.ZOMBIE,
                Work.TIME_INTERVAL,
                HttpWork.REMOTE_USER,
                HttpWork.REMOTE_ADDRESS,
                HttpWork.STATUS_CODE,
                HttpWork.SESSION_ID,
                HttpWork.PATH,
                HttpWork.ACCEPT,
                ContextualTaskDecorator.TASK_CLASS_NAME,
                ContextualTaskDecorator.TASK_ID,
                SpringWork.ENDPOINT,
                ID,
                UNDERSCORE_ID,
                TYPE,
                UNDERSCORE_TYPE,
                TOKEN,
                UNDERSCORE_TOKEN,
                GUID,
                UNDERSCORE_GUID
        ));
    }

    private Map<String, String> bannedMap;
    private Map<String, String> standardMap;
    private Function<String, String> function = Function.identity();

    public PathMetadataCleanser() {
        this(new HashMap<>());
    }

    /**
     * standardMap keys and values will be converted from CamelCase
     * to snake_case
     *
     * @param standardMap keys that you want to replace for standardization
     */
    public PathMetadataCleanser(Map<String, String> standardMap) {
        initMaps(standardMap);
    }

    @Override
    public String cleanse(String key, String value, String requestUri) {
        String cleanKey = camelToSnakeCase(key);
        if (containsReservedKey(cleanKey) && isNotBlank(value) && isNotBlank(requestUri)) {
            cleanKey = prefixContextToKey(cleanKey, value, requestUri);
        }
        cleanKey = getStandardKey(cleanKey);
        cleanKey = transform(cleanKey);
        cleanKey = getBannedKey(cleanKey);
        return cleanKey;
    }

    /**
     * Converts CamelCase to snake_case when adding to standardMap
     *
     * @param key   the key that you want to replace
     * @param value the key that you are replacing with
     */
    public void addStandard(String key, String value) {
        putIfAbsent(standardMap, key, value);
    }

    /**
     * Converts CamelCase to snake_case when adding to bannedMap
     *
     * @param key   the key that you want to restrict
     * @param value the key that you are replacing with
     */
    public void addBanned(String key, String value) {
        if (bannedMap.containsKey(camelToSnakeCase(value))) {
            throw new IllegalArgumentException("Value cannot be a key that is already banned");
        }
        putIfAbsent(bannedMap, key, value);
    }

    /**
     * Sets the function to transform the key which is executed when {@link #cleanse(String, String, String)}
     * is called
     *
     * @param function The transformation function to convert the key
     */
    public void setTransformFunction(Function<String, String> function) {
        this.function = function;
    }

    boolean containsReservedKey(String key) {
        return RESERVED_MAP.contains(key);
    }

    String getStandardKey(String key) {
        return standardMap.getOrDefault(key, key);
    }

    String getBannedKey(String key) {
        return bannedMap.getOrDefault(key, key);
    }

    private String transform(String key) {
        return function.apply(key);
    }

    private String prefixContextToKey(String key, String value, final String requestUri) {
        String regex = buildContextRegex(key, value);
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(requestUri);

        if (matcher.find()) {
            String contextKey = camelToSnakeCase(matcher.group(1));
            if (!contextKey.endsWith(key)) {
                return contextKey + "_" + key;
            }
            return contextKey;
        }
        return key;
    }

    private String buildContextRegex(final String key, final String valueAppender) {
        String pathValue = valueAppender.startsWith("/") ? valueAppender : "/" + valueAppender;
        return "/([^/]+?)(?:s?-?/?" + key + ")?s?" + pathValue;
    }

    private void putIfAbsent(Map<String, String> map, String key, String value) {
        map.putIfAbsent(camelToSnakeCase(key), camelToSnakeCase(value));
    }

    private void initMaps(Map<String, String> standard) {
        standardMap = new ConcurrentHashMap<>();
        standardMap.put("username", PATH_USER_NAME);
        standardMap.put("user", PATH_USER_NAME);
        standardMap.put("user_id", PATH_USER_NAME);
        standardMap.put("user_name", PATH_USER_NAME);
        if (standard != null) {
            standard.forEach((key, value) -> putIfAbsent(standardMap, key, value));
        }

        bannedMap = new ConcurrentHashMap<>();
        bannedMap.put(ID, UNKNOWN_ID);
        bannedMap.put(UNDERSCORE_ID, UNKNOWN_ID);
        bannedMap.put(TYPE, UNKNOWN_TYPE);
        bannedMap.put(UNDERSCORE_TYPE, UNKNOWN_TYPE);
        bannedMap.put("", UNKNOWN_TYPE);
    }
}
