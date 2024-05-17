/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;

public class RemovedToolProviderConstructor extends Recipe {

    @Override
    public String getDisplayName() {
        return "Converts method invocations to `javax.tools.ToolProvider()` to static calls";
    }

    @Override
    public String getDescription() {
        return "The `javax.tools.ToolProvider()` constructor has been removed in Java SE 16 since the class only contains Static methods." +
                "The recipe converts `javax.tools.ToolProvider getSystemJavaCompiler()`, `javax.tools.ToolProvider getSystemDocumentationTool()` and `javax.tools.ToolProvider getSystemToolClassLoader()` to static methods.";

    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return new JavaVisitor<ExecutionContext>() {

            private final JavaType.FullyQualified classType = JavaType.ShallowClass.build("javax.tools.ToolProvider");
            private final String TOOLPROVIDER_CLASS_TYPE = "javax.tools.ToolProvider";

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = method;
                JavaType.Method methodType = method.getMethodType();
                boolean isSameReceiverType = method.getSelect() != null &&
                        TypeUtils.isOfClassType(method.getSelect().getType(), TOOLPROVIDER_CLASS_TYPE);
                if (isSameReceiverType) {
                    JavaType.Method transformedType = null;
                    if (methodType != null) {
                        maybeRemoveImport(methodType.getDeclaringType());
                        transformedType = methodType.withDeclaringType(classType);
                        if (!methodType.hasFlags(Flag.Static)) {
                            Set<Flag> flags = new LinkedHashSet<>(methodType.getFlags());
                            flags.add(Flag.Static);
                            transformedType = transformedType.withFlags(flags);
                        }
                    }
                    if (m.getSelect() == null) {
                        maybeAddImport("javax.tools.ToolProvider", m.getSimpleName(), true);
                    } else {
                        maybeAddImport("javax.tools.ToolProvider", true);
                        m = method.withSelect(
                                new J.Identifier(randomId(),
                                        method.getSelect() == null ?
                                                Space.EMPTY :
                                                method.getSelect().getPrefix(),
                                        Markers.EMPTY,
                                        emptyList(),
                                        classType.getClassName(),
                                        classType,
                                        null
                                )
                        );
                    }
                    m = m.withMethodType(transformedType)
                            .withName(m.getName().withType(transformedType));
                }
                return (J.MethodInvocation) super.visitMethodInvocation(m, ctx);
            }
        };

    }
}
