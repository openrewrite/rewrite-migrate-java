/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.search;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.JavaSourceFile;

@Value
@EqualsAndHashCode(callSuper = false)
public class AboutJavaVersion extends Recipe {
    @Option(required = false,
            description = "Only mark the Java version when this type is in use.",
            example = "lombok.val")
    @Nullable
    String whenUsesType;

    @Override
    public String getDisplayName() {
        return "List calculated information about Java version on source files";
    }

    @Override
    public String getDescription() {
        return "A diagnostic for studying the applicability of Java version constraints.";
    }

    @Override
    protected @Nullable TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return StringUtils.isBlank(whenUsesType) ? null : new UsesType<>(whenUsesType);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext executionContext) {
                return cu.getMarkers().findFirst(JavaVersion.class)
                        .map(version -> (JavaSourceFile) cu.withMarkers(cu.getMarkers()
                                .searchResult("Java version: " + version.getMajorVersion())))
                        .orElse(cu);
            }
        };
    }
}
