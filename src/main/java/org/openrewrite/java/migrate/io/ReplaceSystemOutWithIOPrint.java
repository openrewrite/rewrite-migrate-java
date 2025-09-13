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
package org.openrewrite.java.migrate.io;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

public class ReplaceSystemOutWithIOPrint extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate System.out.print to Java 25 IO utility class";
    }

    @Override
    public String getDescription() {
        return "Replace System.out.print(), System.out.println() with IO.print() and IO.println(). Migrates to the new IO utility class introduced in Java 25.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final JavaTemplate printTemplate = JavaTemplate.builder("IO.print(#{any()})").build();
            private final JavaTemplate printlnTemplate = JavaTemplate.builder("IO.println(#{any()})").build();
            private final JavaTemplate printEmptyTemplate = JavaTemplate.builder("IO.print()").build();
            private final JavaTemplate printlnEmptyTemplate = JavaTemplate.builder("IO.println()").build();

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (!isSystemOutMethod(m)) {
                    return m;
                }
                String methodName = m.getName().getSimpleName();
                JavaTemplate tpl;

                if ("print".equals(methodName)) {
                    tpl = m.getArguments().isEmpty() ? printEmptyTemplate : printTemplate;
                } else if ("println".equals(methodName)) {
                    tpl = m.getArguments().isEmpty() ? printlnEmptyTemplate : printlnTemplate;
                } else {
                    return m;
                }

                return applyTemplate(tpl, m);
            }

            private J.MethodInvocation applyTemplate(JavaTemplate tpl, J.MethodInvocation m) {
                return m.getArguments().isEmpty()
                        ? tpl.apply(getCursor(), m.getCoordinates().replace())
                        : tpl.apply(getCursor(), m.getCoordinates().replace(), m.getArguments().get(0));
            }

            private boolean isSystemOutMethod(J.MethodInvocation mi) {
                if (!(mi.getSelect() instanceof J.FieldAccess)) {
                    return false;
                }

                J.FieldAccess fieldAccess = (J.FieldAccess) mi.getSelect();
                return fieldAccess.getTarget() instanceof J.Identifier &&
                        ((J.Identifier) fieldAccess.getTarget()).getSimpleName().equals("System") && fieldAccess.getName().getSimpleName().equals("out") &&
                        (mi.getName().getSimpleName().equals("print") || mi.getName().getSimpleName().equals("println"));
            }
        };
    }
}
