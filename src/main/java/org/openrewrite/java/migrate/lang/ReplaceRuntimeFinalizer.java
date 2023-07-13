/*
 * Copyright 2023 the original author or authors.
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

package org.openrewrite.java.migrate.lang;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
@Getter
public class ReplaceRuntimeFinalizer extends Recipe {

    private static final String METHOD_PATTERN = "runFinalizersOnExit";
    public static final String NEW_METHOD_NAME = "Runtime.getRuntime().addShutdownHook(new Thread(() -> Runtime.getRuntime().exit(0)))";
    private static final String JAVA_LANG_RUNTIME = "java.lang.Runtime";
    private static final String JAVA_LANG_SYSTEM = "java.lang.System";

    @Override
    public String getDisplayName() {
        return "Replace `Runtime.runFinalizersOnExit()` and `System.runFinalizersOnExit()` with `Runtime.addShutDownHook(Thread)`";
    }

    @Override
    public String getDescription() {
        return "Replace invocations of `java.lang.Runtime.runFinalizersOnExit()` or `java.lang.System.runFinalizersOnExit()` with `java.lang.Runtime.addShutDownHook(Thread)`.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("JDK-8198250");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return Preconditions.check(
                Preconditions.or(
                        new UsesType<>(JAVA_LANG_RUNTIME, false),
                        new UsesType<>(JAVA_LANG_SYSTEM, false)),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                        if (Objects.nonNull(mi.getSelect()) && matchesType(mi.getSelect().getType()) && METHOD_PATTERN.equals(mi.getSimpleName())) {
                            mi = JavaTemplate.builder(NEW_METHOD_NAME)
                                    .build()
                                    .apply(updateCursor(mi), mi.getCoordinates().replace());
                        }
                        return mi;
                    }

                    private boolean matchesType(JavaType type) {
                        return TypeUtils.isAssignableTo(JAVA_LANG_RUNTIME, type)
                                || TypeUtils.isAssignableTo(JAVA_LANG_SYSTEM, type);
                    }
                });
    }
}
