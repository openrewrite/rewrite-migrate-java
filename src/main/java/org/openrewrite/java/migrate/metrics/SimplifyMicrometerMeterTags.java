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
package org.openrewrite.java.migrate.metrics;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

import java.util.Set;

import static java.util.Collections.singleton;

public class SimplifyMicrometerMeterTags extends Recipe {
    private static final MethodMatcher COUNTER_TAGS = new MethodMatcher("io.micrometer.core.instrument.Counter.Builder tags(String[])");

    @Getter
    final String displayName = "Simplify [Micrometer](https://micrometer.io) meter tags";

    @Getter
    final String description = "Use the simplest method to add new tags.";

    @Getter
    final Set<String> tags = singleton( "micrometer" );

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (COUNTER_TAGS.matches(m)) {
                    if (m.getArguments().get(0) instanceof J.NewArray) {
                        J.NewArray arr = (J.NewArray) m.getArguments().get(0);
                        if (arr.getInitializer() != null && arr.getInitializer().size() > 1) {
                            m = JavaTemplate.builder("#{any(String)}, #{any(String)}")
                                    .contextSensitive()
                                    .build()
                                    .apply(updateCursor(m), m.getCoordinates().replaceArguments(), arr.getInitializer().get(0), arr.getInitializer().get(1));
                        }
                    } else {
                        m = JavaTemplate.builder("#{any()}[0], #{any()}[1]")
                                .contextSensitive()
                                .build()
                                .apply(updateCursor(m), m.getCoordinates().replaceArguments(), m.getArguments().get(0), m.getArguments().get(0));
                    }
                    m = m.withName(m.getName().withSimpleName("tag"));
                }
                return m;
            }
        };
    }
}
