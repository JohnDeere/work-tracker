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

package com.deere.isg.worktracker;

import java.util.regex.Pattern;

public final class StringUtils {
    private static final Pattern SNAKE_CASE = Pattern.compile("^([a-z][a-z0-9]*)(_[a-z0-9]+)*$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern CAMEL_TO_SNAKE_CASE = Pattern.compile("(.)(\\p{javaUpperCase})");

    private StringUtils(){

    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    public static String replaceNullWithEmpty(String value) {
        return isBlank(value) ? "" : value;
    }

    public static boolean isSnakeCase(String value) {
        return value != null && SNAKE_CASE.matcher(value.trim()).matches();
    }

    public static String trimToEmpty(String value) {
        return isNotBlank(value) ? value.trim() : "";
    }

    public static boolean isNotSnakeCase(String value) {
        return !isSnakeCase(value);
    }

    public static boolean isNotEmpty(String value) {
        return !isEmpty(value);
    }

    public static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public static String killWhitepace(String value) {
        return value == null
                ? ""
                : WHITESPACE.matcher(value.trim()).replaceAll("");
    }

    public static String camelToSnakeCase(String value) {
        final String input = replaceNonWordWithUnderscore(killWhitepace(value));
        return isAllUpperCase(input)
                ? input.toLowerCase()
                : CAMEL_TO_SNAKE_CASE.matcher(input).replaceAll("$1_$2").toLowerCase();
    }

    private static String replaceNonWordWithUnderscore(String s) {
        s = s.replaceAll("\\W+", "_");
        if (s.startsWith("_")) {
            s = s.substring(1);
        }
        if (s.endsWith("_")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    public static boolean isAllUpperCase(String value) {
        return value != null
                && value.chars().allMatch(i -> Character.isLetter(i) ? Character.isUpperCase(i) : !Character.isWhitespace(i));
    }
}
