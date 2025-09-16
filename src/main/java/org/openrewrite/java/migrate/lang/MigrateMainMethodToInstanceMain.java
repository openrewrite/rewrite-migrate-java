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
package org.openrewrite.java.migrate.lang;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singleton;

public class MigrateMainMethodToInstanceMain extends Recipe {
    @Override
    public String getDisplayName() {
        return "Migrate `public static void main(String[] args)` to instance `void main()`";
    }

    @Override
    public String getDescription() {
        return "Migrate `public static void main(String[] args)` method to instance `void main()` method when the `args` parameter is unused, as supported by JEP 512 in Java 21+.";
    }

    @Override
    public Set<String> getTags() {
        return singleton("java21");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesJavaVersion<>(21),
                new MigrateMainMethodVisitor()
        );
    }

    private static class MigrateMainMethodVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);

            // Check if this is a main method: public static void main(String[] args)
            if (!"main".equals(md.getSimpleName()) ||
                md.getReturnTypeExpression() == null ||
                !md.getReturnTypeExpression().toString().equals("void") ||
                md.getParameters().size() != 1) {
                return md;
            }

            // Check modifiers - must have public and static
            boolean hasPublic = false;
            boolean hasStatic = false;
            for (J.Modifier modifier : md.getModifiers()) {
                if (modifier.getType() == J.Modifier.Type.Public) {
                    hasPublic = true;
                } else if (modifier.getType() == J.Modifier.Type.Static) {
                    hasStatic = true;
                }
            }

            if (!hasPublic || !hasStatic) {
                return md;
            }

            // Check parameter type
            if (!(md.getParameters().get(0) instanceof J.VariableDeclarations)) {
                return md;
            }

            J.VariableDeclarations param = (J.VariableDeclarations) md.getParameters().get(0);
            if (param.getVariables().isEmpty()) {
                return md;
            }

            J.VariableDeclarations.NamedVariable paramVar = param.getVariables().get(0);
            String paramName = paramVar.getSimpleName();

            // Check if parameter is String[] type
            JavaType paramType = param.getType();
            if (paramType == null || !TypeUtils.isOfClassType(paramType, "java.lang.String")) {
                return md;
            }

            // Ensure it's an array
            if (!(paramType instanceof JavaType.Array)) {
                return md;
            }

            // Check if the parameter is used in the method body
            if (md.getBody() == null || isParameterUsed(md.getBody(), paramName)) {
                return md;
            }

            // Remove public and static modifiers, preserve spacing
            List<J.Modifier> newModifiers = new ArrayList<>();
            Space leadingSpace = null;

            for (int i = 0; i < md.getModifiers().size(); i++) {
                J.Modifier modifier = md.getModifiers().get(i);
                if (modifier.getType() != J.Modifier.Type.Public &&
                    modifier.getType() != J.Modifier.Type.Static) {
                    if (!newModifiers.isEmpty() || leadingSpace == null) {
                        newModifiers.add(modifier);
                    } else {
                        // Apply the leading space to the first remaining modifier
                        newModifiers.add(modifier.withPrefix(leadingSpace));
                    }
                } else if (leadingSpace == null) {
                    // Capture the leading space from the first public/static modifier
                    leadingSpace = modifier.getPrefix();
                }
            }

            // If no modifiers remain and we have a return type, preserve the spacing
            if (newModifiers.isEmpty() && leadingSpace != null && md.getReturnTypeExpression() != null) {
                md = md.withReturnTypeExpression(md.getReturnTypeExpression().withPrefix(leadingSpace));
            }

            // Remove the parameter
            md = md.withModifiers(newModifiers)
                   .withParameters(new ArrayList<>());

            return md;
        }

        private boolean isParameterUsed(J.Block body, String paramName) {
            AtomicBoolean used = new AtomicBoolean(false);
            new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                    if (paramName.equals(identifier.getSimpleName())) {
                        // Check if this identifier is a variable declaration (not a usage)
                        J.VariableDeclarations.NamedVariable namedVar = getCursor().firstEnclosing(J.VariableDeclarations.NamedVariable.class);
                        if (namedVar == null || !paramName.equals(namedVar.getSimpleName())) {
                            used.set(true);
                        }
                    }
                    return super.visitIdentifier(identifier, ctx);
                }
            }.visit(body, new InMemoryExecutionContext());
            return used.get();
        }
    }
}