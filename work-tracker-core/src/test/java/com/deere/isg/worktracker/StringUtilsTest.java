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


package com.deere.isg.worktracker;

import org.junit.Test;

import static com.deere.isg.worktracker.StringUtils.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class StringUtilsTest {

    @Test
    public void emptyStringCheckIfBlank() {
        assertThat(isBlank("  "), is(true));
        assertThat(isBlank(""), is(true));
    }

    @Test
    public void emptyStringIsBlank() {
        assertThat(isNotBlank("  "), is(false));
        assertThat(isNotBlank(""), is(false));
    }

    @Test
    public void nonEmptyStringIsNotBlank() {
        assertThat(isBlank("h"), is(false));
        assertThat(isBlank(" h "), is(false));
        assertThat(isNotBlank("h"), is(true));
        assertThat(isNotBlank(" h "), is(true));
    }

    @Test
    public void nullValueIsBlank() {
        assertThat(isBlank(null), is(true));
        assertThat(isNotBlank(null), is(false));
    }

    @Test
    public void nullIsReplacedWithEmpty() {
        assertThat(replaceNullWithEmpty(null), is(""));
    }

    @Test
    public void nonEmptyReplacementReturnsValue() {
        assertThat(replaceNullWithEmpty("some value"), is("some value"));
    }

    @Test
    public void validatesSnakeCase() {
        assertThat(isSnakeCase(null), is(false));
        assertThat(isSnakeCase("some_key"), is(true));
        assertThat(isSnakeCase("JohnDeere"), is(false));
        assertThat(isSnakeCase("Upper_Case"), is(false));
        assertThat(isSnakeCase("$happy#"), is(false));
    }

    @Test
    public void trimsString() {
        String trimmedKey = trimToEmpty("  some key ");

        assertThat(trimmedKey, is("some key"));
    }

    @Test
    public void trimsNullStringToEmpty() {
        String trimmedKey = trimToEmpty(null);

        assertThat(trimmedKey, is(""));
    }

    @Test
    public void notASnakeCase() {
        assertThat(isNotSnakeCase(null), is(true));
        assertThat(isNotSnakeCase("  test_key "), is(false));
        assertThat(isNotSnakeCase("Test"), is(true));
        assertThat(isNotSnakeCase("   Test   "), is(true));
    }

    @Test
    public void checksIfEmpty() {
        assertThat(isEmpty(null), is(true));
        assertThat(isEmpty(" "), is(false));
        assertThat(isEmpty(" a "), is(false));
        assertThat(isEmpty(""), is(true));
    }

    @Test
    public void checksIfNotEmpty() {
        assertThat(isNotEmpty(null), is(false));
        assertThat(isNotEmpty(" "), is(true));
        assertThat(isNotEmpty(" a "), is(true));
        assertThat(isNotEmpty(""), is(false));
    }

    @Test
    public void removesWhitespace() {
        assertThat(killWhitepace(" test test"), is("testtest"));
        assertThat(killWhitepace(null), is(""));
    }

    @Test
    public void makesDashCaseToSnakeCase() {
        assertThat(camelToSnakeCase("dash-case"), is("dash_case"));
        assertThat(camelToSnakeCase("dollar$case"), is("dollar_case"));
        assertThat(camelToSnakeCase("percent%case"), is("percent_case"));
        assertThat(camelToSnakeCase("dash-dash--case"), is("dash_dash_case"));
        assertThat(camelToSnakeCase("$#dash^case-\\&%^&"), is("dash_case"));
    }

    @Test
    public void letsNumbersBeInKeys() {
        assertThat(camelToSnakeCase("A1234567890"), is("a1234567890"));
        assertThat(isSnakeCase("a1234567890"), is(true));
    }

    @Test
    public void makesCamelCaseToSnakeCase() {
        assertThat(camelToSnakeCase("CamelCase"), is("camel_case"));
        assertThat(camelToSnakeCase("camelCase"), is("camel_case"));
        assertThat(camelToSnakeCase(" camel case "), is("camelcase"));
        assertThat(camelToSnakeCase(" camel Case "), is("camel_case"));
        assertThat(camelToSnakeCase(" camel 123 478Case123 98129 "), is("camel123478_case12398129"));
        assertThat(camelToSnakeCase(null), is(""));
    }

    @Test
    public void allUpperCase() {
        assertThat(isAllUpperCase("ID"), is(true));
        assertThat(isAllUpperCase("iD"), is(false));
        assertThat(isAllUpperCase("GUID"), is(true));
        assertThat(isAllUpperCase("ASSERTION"), is(true));
        assertThat(isAllUpperCase("          "), is(false));
        assertThat(isAllUpperCase(null), is(false));
        assertThat(isAllUpperCase("UPPER_CASE"), is(true));
    }

    @Test
    public void allUpperCaseForSnakeCaseConversion() {
        assertThat(camelToSnakeCase("ID"), is("id"));
        assertThat(camelToSnakeCase("GUID"), is("guid"));
        assertThat(camelToSnakeCase(" ASSER TION"), is("assertion"));
        assertThat(camelToSnakeCase("aID"), is("a_id"));
        assertThat(camelToSnakeCase("UPPER_CASE"), is("upper_case"));
    }
}
