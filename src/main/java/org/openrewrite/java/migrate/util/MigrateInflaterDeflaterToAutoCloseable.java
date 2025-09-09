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
package org.openrewrite.java.migrate.util;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Recipe;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

@EqualsAndHashCode(callSuper = false)
@Value
public class MigrateInflaterDeflaterToAutoCloseable extends Recipe {

    private static final String JAVA_UTIL_ZIP_DEFLATER = "java.util.zip.Deflater";
    private static final String JAVA_UTIL_ZIP_INFLATER = "java.util.zip.Inflater";

    private static final MethodMatcher JAVA_UTIL_ZIP_DEFLATER_END_MATCHER = new MethodMatcher(JAVA_UTIL_ZIP_DEFLATER + " end()");
    private static final MethodMatcher JAVA_UTIL_ZIP_INFLATER_END_MATCHER = new MethodMatcher(JAVA_UTIL_ZIP_INFLATER + " end()");

    @Override
    public String getDisplayName() {
        return "Replace `Inflater` and `Deflater` `end()` calls with `close()` calls";
    }

    @Override
    public String getDescription() {
        return "Replace `end()` method calls with `close()` method calls for `Inflater` and `Deflater` classes in Java 25+, as they now implement AutoCloseable.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.and(
                        new UsesJavaVersion<>(25),
                        Preconditions.or(
                                new UsesType<>(JAVA_UTIL_ZIP_DEFLATER, false),
                                new UsesType<>(JAVA_UTIL_ZIP_INFLATER, false))),
                new JavaIsoVisitor<ExecutionContext>() {

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                        if (JAVA_UTIL_ZIP_DEFLATER_END_MATCHER.matches(mi) ||
                            JAVA_UTIL_ZIP_INFLATER_END_MATCHER.matches(mi)) {
                            return mi.withName(mi.getName().withSimpleName("close"));
                        }
                        return mi;
                    }
                });
    }
}
