/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.openrewrite.json.JsonParser;
import org.openrewrite.json.tree.Json;

import static org.assertj.core.api.Assertions.assertThat;

class RegexToGlobConverterTest {

    @ParameterizedTest
    @CsvSource({
        "'.*\\.txt', '**/*.txt'",
        "'META-INF/.*', 'META-INF/**'",
        "'[^/]*\\.txt', '*.txt'",
        "'config/[^/]*\\.properties', 'config/*.properties'",
        "'exact/path/file\\.txt', 'exact/path/file.txt'",
        "'.*', '**'",
        "'[^/]*/[^/]*\\.xml', '*/*.xml'",
        "'static/.*', 'static/**'",
        "'templates/[^/]*\\.html', 'templates/*.html'",
        "'.*\\.json', '**/*.json'",
        "'.*\\.xml', '**/*.xml'",
        "'[^/]+\\.txt', '*.txt'",
    })
    void convertCommonPatterns(String regex, String expectedGlob) {
        RegexToGlobConverter.ConversionResult result = RegexToGlobConverter.convert(regex);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.glob()).isEqualTo(expectedGlob);
        assertThat(result.warningMessage()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "file[0-9]\\.txt",       // character class with digits
        "file[a-z]\\.txt",       // character class with letters
        "(foo|bar)\\.txt",       // alternation
        "file\\d+\\.txt",        // digit class
        "test.{2,4}\\.txt",      // bounded quantifier
        "^start",                // start anchor
        "end$",                  // end anchor
        "look(?=ahead)",         // positive lookahead
        "look(?!ahead)",         // negative lookahead
    })
    void unconvertiblePatterns(String regex) {
        RegexToGlobConverter.ConversionResult result = RegexToGlobConverter.convert(regex);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.glob()).isNull();
        assertThat(result.warningMessage()).isNotNull();
    }

    @Test
    void emptyPatternReturnsError() {
        RegexToGlobConverter.ConversionResult result = RegexToGlobConverter.convert("");
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.warningMessage()).contains("Empty pattern");
    }

    @Test
    void nullPatternReturnsError() {
        RegexToGlobConverter.ConversionResult result = RegexToGlobConverter.convert(null);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.warningMessage()).contains("Empty pattern");
    }

    @Test
    void literalPathWithEscapedDot() {
        RegexToGlobConverter.ConversionResult result = RegexToGlobConverter.convert("path/to/file\\.txt");
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.glob()).isEqualTo("path/to/file.txt");
    }

    @Test
    void multipleWildcardLevels() {
        RegexToGlobConverter.ConversionResult result = RegexToGlobConverter.convert("[^/]*/[^/]*/[^/]*\\.java");
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.glob()).isEqualTo("*/*/*.java");
    }

    @Test
    void mixedWildcardsAndLiteralPath() {
        RegexToGlobConverter.ConversionResult result = RegexToGlobConverter.convert("src/[^/]*/resources/.*");
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.glob()).isEqualTo("src/*/resources/**");
    }

    @Test
    void jsonEscapedPatternConversion() {
        // OpenRewrite's JSON parser preserves JSON escape sequences in values.
        // So "\\." in JSON source appears as "\\\\.properties" in the value (double backslash).
        // Our converter needs to handle this.

        // This is the pattern value as it would be extracted from OpenRewrite's JSON AST
        // when the JSON source is: "config/[^/]*\\.properties"
        String jsonValue = "config/[^/]*\\\\.properties";

        RegexToGlobConverter.ConversionResult result = RegexToGlobConverter.convert(jsonValue);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.glob()).isEqualTo("config/*.properties");
    }

    @Test
    void jsonEscapedDotOnly() {
        // Test double-backslash dot that comes from JSON
        String jsonValue = ".*\\\\.txt";
        RegexToGlobConverter.ConversionResult result = RegexToGlobConverter.convert(jsonValue);
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.glob()).isEqualTo("**/*.txt");
    }
}
