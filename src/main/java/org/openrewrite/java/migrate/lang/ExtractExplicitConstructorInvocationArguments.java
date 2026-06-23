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

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.VariableNameUtils;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.openrewrite.java.VariableNameUtils.GenerationStrategy.INCREMENT_NUMBER;

public class ExtractExplicitConstructorInvocationArguments extends Recipe {

    @Getter
    final String displayName = "Extract complex `super(..)` and `this(..)` arguments into local variables";

    @Getter
    final String description = "[JEP 513](https://openjdk.org/jeps/513) allows statements before an explicit `super(..)` or " +
                              "`this(..)` constructor invocation. When such a call computes one of its arguments through a method " +
                              "invocation or object creation, this recipe extracts the non-trivial arguments into local variables " +
                              "declared right before the call, surfacing the work done before construction.\n\n" +
                              "This is a strictly behavior-preserving transformation: argument expressions are already evaluated " +
                              "before the delegate constructor body runs, and such an argument can never reference the instance under " +
                              "construction, so hoisting them into preceding statements changes neither the order of side effects nor " +
                              "the set of legal references. Arguments are extracted in their original left-to-right order, and trivial " +
                              "arguments (literals and local variable references, which have no side effects) are left in place. " +
                              "Statements that follow the constructor invocation are deliberately *not* moved, as reordering them " +
                              "relative to the delegate constructor's side effects could change behavior.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesJavaVersion<>(25), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
                if (!md.isConstructor() || md.getBody() == null) {
                    return md;
                }

                J.MethodInvocation superCall = findExplicitConstructorInvocation(md.getBody().getStatements());
                if (superCall == null || superCall.getMethodType() == null) {
                    return md;
                }
                JavaType.Method ctorType = superCall.getMethodType();
                List<Expression> args = superCall.getArguments();
                List<JavaType> paramTypes = ctorType.getParameterTypes();
                List<String> paramNames = ctorType.getParameterNames();
                if (paramTypes.size() != args.size()) {
                    return md;
                }

                // Only act when at least one argument actually does work worth surfacing. This is the
                // cheapest decisive check, so make it first and bail before any name generation, scope
                // scanning, or templating that an unaffected constructor would not need.
                boolean anyComplex = false;
                for (Expression arg : args) {
                    if (arg instanceof J.MethodInvocation || arg instanceof J.NewClass) {
                        anyComplex = true;
                        break;
                    }
                }
                if (!anyComplex) {
                    return md;
                }

                // Plan the extraction: hoist everything but trivial, side-effect-free arguments, in order.
                Set<String> usedNames = new LinkedHashSet<>();
                String[] names = new String[args.size()];
                Set<String> imports = new LinkedHashSet<>();
                StringBuilder declarations = new StringBuilder();
                List<Expression> declarationArgs = new ArrayList<>();
                for (int i = 0; i < args.size(); i++) {
                    Expression arg = args.get(i);
                    if (isInlineSafe(arg)) {
                        continue;
                    }
                    JavaType paramType = paramTypes.get(i);
                    String typeName = denotableTypeName(paramType);
                    if (typeName == null) {
                        // Cannot safely name this parameter's type; bail rather than partially extract and risk reordering
                        return md;
                    }
                    String name = uniqueName(baseName(arg, paramNames.get(i)), usedNames);
                    names[i] = name;
                    declarations.append(typeName).append(' ').append(name).append(" = #{any()};\n");
                    declarationArgs.add(arg);
                    if (paramType instanceof JavaType.FullyQualified) {
                        imports.add(((JavaType.FullyQualified) paramType).getFullyQualifiedName());
                    }
                }

                // 1. Declare the extracted arguments right before the constructor invocation, preserving their order
                JavaTemplate.Builder declarationTemplate = JavaTemplate.builder(declarations.toString());
                for (String fqn : imports) {
                    declarationTemplate.imports(fqn);
                    maybeAddImport(fqn);
                }
                md = declarationTemplate.build()
                        .apply(getCursor(), superCall.getCoordinates().before(), declarationArgs.toArray());

                // 2. Replace the now-redundant arguments with references to the new local variables
                StringBuilder argumentList = new StringBuilder();
                List<Expression> inlineArgs = new ArrayList<>();
                for (int i = 0; i < args.size(); i++) {
                    if (i > 0) {
                        argumentList.append(", ");
                    }
                    if (names[i] != null) {
                        argumentList.append(names[i]);
                    } else {
                        argumentList.append("#{any()}");
                        inlineArgs.add(args.get(i));
                    }
                }
                return (J.MethodDeclaration) new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx2) {
                        // Do not descend into nested/local classes; their own `super(..)`/`this(..)` calls
                        // are handled by their own constructor visits, not this one's argument list.
                        return classDecl;
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation mi, ExecutionContext ctx2) {
                        mi = super.visitMethodInvocation(mi, ctx2);
                        if (isExplicitConstructorInvocation(mi)) {
                            return JavaTemplate.builder(argumentList.toString()).build()
                                    .apply(getCursor(), mi.getCoordinates().replaceArguments(), inlineArgs.toArray());
                        }
                        return mi;
                    }
                }.visitNonNull(md, ctx, getCursor().getParentOrThrow());
            }

            private J.@Nullable MethodInvocation findExplicitConstructorInvocation(List<Statement> statements) {
                for (Statement statement : statements) {
                    if (statement instanceof J.MethodInvocation && isExplicitConstructorInvocation((J.MethodInvocation) statement)) {
                        return (J.MethodInvocation) statement;
                    }
                }
                return null;
            }

            private boolean isExplicitConstructorInvocation(J.MethodInvocation mi) {
                return mi.getSelect() == null &&
                       ("super".equals(mi.getSimpleName()) || "this".equals(mi.getSimpleName()));
            }

            /**
             * An argument is safe to leave in place only if it cannot be observably reordered relative to the
             * extracted arguments, i.e. it has no side effects, cannot throw, and cannot trigger class initialization.
             * That holds for literals and references to local variables or parameters, but not for field accesses
             * (a static field read may trigger class initialization) or any compound expression.
             */
            private boolean isInlineSafe(Expression arg) {
                if (arg instanceof J.Literal) {
                    return true;
                }
                if (arg instanceof J.Identifier) {
                    JavaType.Variable fieldType = ((J.Identifier) arg).getFieldType();
                    return fieldType != null && !(fieldType.getOwner() instanceof JavaType.FullyQualified);
                }
                return false;
            }

            private @Nullable String denotableTypeName(JavaType type) {
                if (type instanceof JavaType.Primitive) {
                    return ((JavaType.Primitive) type).getKeyword();
                }
                // Only non-generic class types can be safely named without risking out-of-scope type variables
                if (type instanceof JavaType.FullyQualified && !(type instanceof JavaType.Parameterized)) {
                    return ((JavaType.FullyQualified) type).getClassName().replace('$', '.');
                }
                return null;
            }

            private String baseName(Expression arg, @Nullable String paramName) {
                if (isValidBaseName(paramName)) {
                    return paramName;
                }
                if (arg instanceof J.MethodInvocation) {
                    return ((J.MethodInvocation) arg).getSimpleName();
                }
                JavaType.FullyQualified created = TypeUtils.asFullyQualified(arg.getType());
                if (created != null) {
                    return StringUtils.uncapitalize(created.getClassName());
                }
                return "value";
            }

            private boolean isValidBaseName(@Nullable String name) {
                // Reject synthetic parameter names (`arg0`, `arg1`, ...) from constructors compiled without `-parameters`
                return StringUtils.isNotEmpty( name ) && Character.isJavaIdentifierStart(name.charAt(0)) &&
                       !name.matches("arg\\d+");
            }

            private String uniqueName(String base, Set<String> usedNames) {
                String candidate = VariableNameUtils.generateVariableName(base, getCursor(), INCREMENT_NUMBER);
                for (int n = 1; usedNames.contains(candidate); n++) {
                    candidate = VariableNameUtils.generateVariableName(base + n, getCursor(), INCREMENT_NUMBER);
                }
                usedNames.add(candidate);
                return candidate;
            }
        });
    }
}
