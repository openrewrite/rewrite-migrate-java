/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import java.util.Arrays;
import java.util.List;

/**
 * Finds usages of locale-based date/time formatting APIs that may be affected by the
 * JDK 20+ CLDR locale data changes. Starting with JDK 20, the Unicode CLDR 42 locale data
 * changed the space character before AM/PM designators from a regular space to a narrow
 * no-break space (NNBSP, \u202F).
 * <p>
 * This can cause parsing issues when user input contains regular spaces but the formatter
 * expects NNBSP. The affected APIs include locale-based DateFormat and DateTimeFormatter
 * factory methods.
 *
 * @see <a href="https://bugs.openjdk.org/browse/JDK-8324308">JDK-8324308</a>
 * @see <a href="https://inside.java/2024/03/29/quality-heads-up/">Unicode CLDR Version 42 Heads-up</a>
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class FindLocaleDateTimeFormats extends Recipe {

    // DateFormat factory methods that return locale-sensitive formatters
    private static final MethodMatcher DATE_FORMAT_GET_TIME_INSTANCE =
            new MethodMatcher("java.text.DateFormat getTimeInstance(..)", true);
    private static final MethodMatcher DATE_FORMAT_GET_DATE_TIME_INSTANCE =
            new MethodMatcher("java.text.DateFormat getDateTimeInstance(..)", true);
    private static final MethodMatcher DATE_FORMAT_GET_INSTANCE =
            new MethodMatcher("java.text.DateFormat getInstance(..)", true);

    // DateTimeFormatter factory methods that return locale-sensitive formatters
    private static final MethodMatcher DTF_OF_LOCALIZED_TIME =
            new MethodMatcher("java.time.format.DateTimeFormatter ofLocalizedTime(..)", true);
    private static final MethodMatcher DTF_OF_LOCALIZED_DATE_TIME =
            new MethodMatcher("java.time.format.DateTimeFormatter ofLocalizedDateTime(..)", true);

    private static final List<MethodMatcher> ALL_MATCHERS = Arrays.asList(
            DATE_FORMAT_GET_TIME_INSTANCE,
            DATE_FORMAT_GET_DATE_TIME_INSTANCE,
            DATE_FORMAT_GET_INSTANCE,
            DTF_OF_LOCALIZED_TIME,
            DTF_OF_LOCALIZED_DATE_TIME
    );

    String displayName = "Find locale-sensitive date/time formatting";

    String description = "Finds usages of locale-based date/time formatting APIs that may be affected by " +
                         "JDK 20+ CLDR locale data changes, where the space before AM/PM was changed " +
                         "from a regular space to a narrow no-break space (NNBSP).";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(
                        new UsesMethod<>("java.text.DateFormat get*Instance(..)", true),
                        new UsesMethod<>("java.time.format.DateTimeFormatter ofLocalized*Time(..)", true)
                ),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                        for (MethodMatcher matcher : ALL_MATCHERS) {
                            if (matcher.matches(mi)) {
                                return SearchResult.found(mi, "JDK 20+ CLDR: may use NNBSP before AM/PM");
                            }
                        }
                        return mi;
                    }
                }
        );
    }
}
