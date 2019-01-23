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

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PathMetadataCleanserTest {
    private static final String BEAR = "bear";
    private static final String SAD_BEAR = "sad_bear";
    private static final Map<String, String> STANDARD_KEYS;
    private static final String KEY = "key";
    private static final String SOME_KEY = "some_key";

    private static final String CAMEL_CASE_TEST = "camelCaseTest";
    private static final String SOME_TEST = "some_test";
    private static final String SNAKE_CASE_TEST = "camel_case_test";
    private static final String LOWER_ID = "id";
    private static final String TEST_ID = "test_id";
    private static final String TESTING_ID = "testing_id";

    private static final String[] RESERVED_KEYS = {
            "thread_name", "elapsed_ms", "request_url", "request_id", "zombie", "time_interval",
            "remote_user", "remote_address", "status_code", "session_id", "path", "accept",
            "task_class_name", "task_id", "id", "_id", "type", "_type", "token", "endpoint",
            "_token", "guid", "_guid"
    };

    private static final Map<String, String> BANNED_KEYS;
    private static final String PATH_USER_NAME = "path_user_name";
    private static final String UNKNOWN_TYPE = "unknown_type";
    private static final String UNKNOWN_ID = "unknown_id";

    static {
        STANDARD_KEYS = new HashMap<>();
        STANDARD_KEYS.put(SAD_BEAR, BEAR);
        STANDARD_KEYS.put(CAMEL_CASE_TEST, SOME_TEST);

        BANNED_KEYS = new HashMap<>();
        BANNED_KEYS.put("id", UNKNOWN_ID);
        BANNED_KEYS.put("_id", UNKNOWN_ID);
        BANNED_KEYS.put("type", UNKNOWN_TYPE);
        BANNED_KEYS.put("_type", UNKNOWN_TYPE);
        BANNED_KEYS.put("", UNKNOWN_TYPE);
    }

    private PathMetadataCleanser metadataCleanser;

    @Before
    public void setUp() {
        metadataCleanser = new PathMetadataCleanser(STANDARD_KEYS);
    }

    @Test
    public void reservedKeysInitialized() {
        assertThat(Arrays.stream(RESERVED_KEYS).allMatch(metadataCleanser::containsReservedKey), is(true));
    }

    @Test
    public void bannedKeyMapInitialized() {
        assertThat(BANNED_KEYS.entrySet().stream().allMatch(e -> metadataCleanser.getBannedKey(e.getKey()).equals(e.getValue())), is(true));
    }

    @Test
    public void standardMapInitialized() {
        String key1 = metadataCleanser.getStandardKey(SAD_BEAR);
        assertThat(key1, is(BEAR));

        String key2 = metadataCleanser.getStandardKey(SNAKE_CASE_TEST);
        assertThat(key2, is(SOME_TEST));
    }

    @Test
    public void addsStandardKey() {
        metadataCleanser.addStandard(KEY, SOME_KEY);
        assertThat(metadataCleanser.getStandardKey(KEY), is(SOME_KEY));

        metadataCleanser.addStandard("someId", "anyId");
        assertThat(metadataCleanser.getStandardKey("some_id"), is("any_id"));
    }

    @Test
    public void addsBannedKey() {
        metadataCleanser.addBanned(KEY, SOME_KEY);
        assertThat(metadataCleanser.getBannedKey(KEY), is(SOME_KEY));

        metadataCleanser.addBanned("someId", "anyId");
        assertThat(metadataCleanser.getBannedKey("some_id"), is("any_id"));
    }

    @Test
    public void returnsDefaultOrFalseWhenNotPresent() {
        String standardKey = metadataCleanser.getStandardKey(KEY);
        assertThat(standardKey, is(KEY));

        String bannedKey = metadataCleanser.getBannedKey(KEY);
        assertThat(bannedKey, is(KEY));

        boolean reservedKey = metadataCleanser.containsReservedKey(KEY);
        assertThat(reservedKey, is(false));
    }

    @Test
    public void convertsCamelToSnakeCaseForStandardMap() {
        String snakeKey = metadataCleanser.getStandardKey(SNAKE_CASE_TEST);
        assertThat(snakeKey, is(SOME_TEST));
    }

    @Test
    public void addContextIfFoundInReserved() {
        String key = metadataCleanser.cleanse(LOWER_ID, "/some id", "/testId/some id/");

        assertThat(key, is(TEST_ID));
    }

    @Test
    public void removesSFromContext() {
        String key = metadataCleanser.cleanse(LOWER_ID, "/some id", "/tests/some id");

        assertThat(key, is(TEST_ID));
    }

    @Test
    public void withoutSlashFromValue() {
        String key = metadataCleanser.cleanse(LOWER_ID, "some id", "/tests/some id");

        assertThat(key, is(TEST_ID));
    }

    @Test
    public void upperCaseKey() {
        String key = metadataCleanser.cleanse("ID", "some id", "/tests/some id/");

        assertThat(key, is(TEST_ID));
    }

    @Test
    public void convertsToStandardKey() {
        metadataCleanser.addStandard(TEST_ID, TESTING_ID);
        String key = metadataCleanser.cleanse(LOWER_ID, "some id", "/tests/some id");

        assertThat(key, is(TESTING_ID));
    }

    @Test
    public void transformFunction() {
        metadataCleanser.setTransformFunction(this::transformFunction);
        String key = metadataCleanser.cleanse(LOWER_ID, "some id", "/tests/some id");

        assertThat(key, is("test_id_anything"));
    }

    @Test
    public void removesBannedKeys() {
        String key = metadataCleanser.cleanse(LOWER_ID, "some id", "/some id");

        assertThat(key, is(UNKNOWN_ID));
    }

    @Test
    public void handlesDashes() {
        String oauthToken = metadataCleanser.cleanse("token", "some id", "/oauth-token/some id");

        assertThat(oauthToken, is("oauth_token"));
    }

    @Test
    public void nullSafetyForStandardMap() {
        PathMetadataCleanser cleanser = new PathMetadataCleanser(null);
        cleanser.addStandard("key", "some_key");

        assertThat(cleanser.getStandardKey("key"), is("some_key"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addsAlreadyBannedKey() {
        metadataCleanser.addBanned("some_id", "id");
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertsValueForDuplicateEntry() {
        metadataCleanser.addBanned("some_id", "ID");
    }

    @Test
    public void standardMapsHasDefaultPathUserName() {
        assertThat(metadataCleanser.getStandardKey("username"), is(PATH_USER_NAME));
        assertThat(metadataCleanser.getStandardKey("user"), is(PATH_USER_NAME));
        assertThat(metadataCleanser.getStandardKey("user_id"), is(PATH_USER_NAME));
        assertThat(metadataCleanser.getStandardKey("user_name"), is(PATH_USER_NAME));
    }

    @Test
    public void cleanseKeyWhenBlankURIOrValue() {
        assertThat(metadataCleanser.cleanse("id", null, null), is(UNKNOWN_ID));
        assertThat(metadataCleanser.cleanse("id", null, "     "), is(UNKNOWN_ID));
        assertThat(metadataCleanser.cleanse("id", null, "a"), is(UNKNOWN_ID));
        assertThat(metadataCleanser.cleanse("id", "   ", "a"), is(UNKNOWN_ID));
    }

    @Test
    public void underscoreArgsAreHandled() {
        assertThat(metadataCleanser.cleanse("_", null, null), is(UNKNOWN_TYPE));
    }

    private String transformFunction(String key) {
        return key.toLowerCase() + "_anything";
    }

}
