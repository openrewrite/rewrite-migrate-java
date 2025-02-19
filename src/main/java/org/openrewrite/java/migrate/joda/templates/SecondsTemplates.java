/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.joda.templates;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

@NoArgsConstructor
public class SecondsTemplates implements Templates {
    final MethodMatcher secondsStaticMethod = new MethodMatcher(JODA_SECONDS + " seconds(int)");
    final MethodMatcher plusMethod = new MethodMatcher(JODA_SECONDS + " plus(org.joda.time.Seconds)");
    final MethodMatcher minusMethod = new MethodMatcher(JODA_SECONDS + " minus(org.joda.time.Seconds)");
    final MethodMatcher multipliedByMethod = new MethodMatcher(JODA_SECONDS + " multipliedBy(int)");
    final MethodMatcher dividedByMethod = new MethodMatcher(JODA_SECONDS + " dividedBy(int)");
    final MethodMatcher getSecondsMethod = new MethodMatcher(JODA_SECONDS + " getSeconds()");

    final JavaTemplate.Builder secondsStaticMethodTemplate = JavaTemplate.builder("Duration.ofSeconds(#{any(int)})");
    final JavaTemplate.Builder plusMethodTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.plus(#{any(java.time.Duration)})");
    final JavaTemplate.Builder minusMethodTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.minus(#{any(java.time.Duration)})");
    final JavaTemplate.Builder multipliedByMethodTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.multipliedBy(#{any(int)})");
    final JavaTemplate.Builder dividedByMethodTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.dividedBy(#{any(int)})");
    final JavaTemplate.Builder getSecondsMethodTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.getSeconds()");

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(secondsStaticMethod, build(secondsStaticMethodTemplate)));
            add(new MethodTemplate(plusMethod, build(plusMethodTemplate)));
            add(new MethodTemplate(minusMethod, build(minusMethodTemplate)));
            add(new MethodTemplate(multipliedByMethod, build(multipliedByMethodTemplate)));
            add(new MethodTemplate(dividedByMethod, build(dividedByMethodTemplate)));
            add(new MethodTemplate(getSecondsMethod, build(getSecondsMethodTemplate)));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, JAVA_DURATION);
    }
}
