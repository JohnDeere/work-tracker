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

import org.junit.Test;

import static com.deere.isg.worktracker.StringUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

public class StringUtilsTest {

    @Test
    public void emptyStringCheckIfBlank() {
        assertThat(isBlank("  ")).isTrue();
        assertThat(isBlank("")).isTrue();
    }

    @Test
    public void emptyStringIsBlank() {
        assertThat(isNotBlank("  ")).isFalse();
        assertThat(isNotBlank("")).isFalse();
    }

    @Test
    public void nonEmptyStringIsNotBlank() {
        assertThat(isBlank("h")).isFalse();
        assertThat(isBlank(" h ")).isFalse();
        assertThat(isNotBlank("h")).isTrue();
        assertThat(isNotBlank(" h ")).isTrue();
    }

    @Test
    public void nullValueIsBlank() {
        assertThat(isBlank(null)).isTrue();
        assertThat(isNotBlank(null)).isFalse();
    }

    @Test
    public void nullIsReplacedWithEmpty() {
        assertThat(replaceNullWithEmpty(null)).isEqualTo("");
    }

    @Test
    public void nonEmptyReplacementReturnsValue() {
        assertThat(replaceNullWithEmpty("some value")).isEqualTo("some value");
    }

    @Test
    public void validatesSnakeCase() {
        assertThat(isSnakeCase(null)).isFalse();
        assertThat(isSnakeCase("some_key")).isTrue();
        assertThat(isSnakeCase("JohnDeere")).isFalse();
        assertThat(isSnakeCase("Upper_Case")).isFalse();
        assertThat(isSnakeCase("$happy#")).isFalse();
    }

    @Test
    public void trimsString() {
        String trimmedKey = trimToEmpty("  some key ");

        assertThat(trimmedKey).isEqualTo("some key");
    }

    @Test
    public void trimsNullStringToEmpty() {
        String trimmedKey = trimToEmpty(null);

        assertThat(trimmedKey).isEqualTo("");
    }

    @Test
    public void notASnakeCase() {
        assertThat(isNotSnakeCase(null)).isTrue();
        assertThat(isNotSnakeCase("  test_key ")).isFalse();
        assertThat(isNotSnakeCase("Test")).isTrue();
        assertThat(isNotSnakeCase("   Test   ")).isTrue();
    }

    @Test
    public void checksIfEmpty() {
        assertThat(isEmpty(null)).isTrue();
        assertThat(isEmpty(" ")).isFalse();
        assertThat(isEmpty(" a ")).isFalse();
        assertThat(isEmpty("")).isTrue();
    }

    @Test
    public void checksIfNotEmpty() {
        assertThat(isNotEmpty(null)).isFalse();
        assertThat(isNotEmpty(" ")).isTrue();
        assertThat(isNotEmpty(" a ")).isTrue();
        assertThat(isNotEmpty("")).isFalse();
    }

    @Test
    public void removesWhitespace() {
        assertThat(killWhitepace(" test test")).isEqualTo("testtest");
        assertThat(killWhitepace(null)).isEqualTo("");
    }

    @Test
    public void makesDashCaseToSnakeCase() {
        assertThat(camelToSnakeCase("dash-case")).isEqualTo("dash_case");
        assertThat(camelToSnakeCase("dollar$case")).isEqualTo("dollar_case");
        assertThat(camelToSnakeCase("percent%case")).isEqualTo("percent_case");
        assertThat(camelToSnakeCase("dash-dash--case")).isEqualTo("dash_dash_case");
        assertThat(camelToSnakeCase("$#dash^case-\\&%^&")).isEqualTo("dash_case");
    }

    @Test
    public void letsNumbersBeInKeys() {
        assertThat(camelToSnakeCase("A1234567890")).isEqualTo("a1234567890");
        assertThat(isSnakeCase("a1234567890")).isTrue();
    }

    @Test
    public void makesCamelCaseToSnakeCase() {
        assertThat(camelToSnakeCase("CamelCase")).isEqualTo("camel_case");
        assertThat(camelToSnakeCase("camelCase")).isEqualTo("camel_case");
        assertThat(camelToSnakeCase(" camel case ")).isEqualTo("camelcase");
        assertThat(camelToSnakeCase(" camel Case ")).isEqualTo("camel_case");
        assertThat(camelToSnakeCase(" camel 123 478Case123 98129 ")).isEqualTo("camel123478_case12398129");
        assertThat(camelToSnakeCase(null)).isEqualTo("");
    }

    @Test
    public void allUpperCase() {
        assertThat(isAllUpperCase("ID")).isTrue();
        assertThat(isAllUpperCase("iD")).isFalse();
        assertThat(isAllUpperCase("GUID")).isTrue();
        assertThat(isAllUpperCase("ASSERTION")).isTrue();
        assertThat(isAllUpperCase("          ")).isFalse();
        assertThat(isAllUpperCase(null)).isFalse();
        assertThat(isAllUpperCase("UPPER_CASE")).isTrue();
    }

    @Test
    public void allUpperCaseForSnakeCaseConversion() {
        assertThat(camelToSnakeCase("ID")).isEqualTo("id");
        assertThat(camelToSnakeCase("GUID")).isEqualTo("guid");
        assertThat(camelToSnakeCase(" ASSER TION")).isEqualTo("assertion");
        assertThat(camelToSnakeCase("aID")).isEqualTo("a_id");
        assertThat(camelToSnakeCase("UPPER_CASE")).isEqualTo("upper_case");
    }
}
