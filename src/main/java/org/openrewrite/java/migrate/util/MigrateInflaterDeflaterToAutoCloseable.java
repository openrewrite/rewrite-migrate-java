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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

import java.util.*;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = false)
@Value
public class MigrateInflaterDeflaterToAutoCloseable extends Recipe {

    private static final String JAVA_UTIL_ZIP_DEFLATER = "java.util.zip.Deflater";
    private static final String JAVA_UTIL_ZIP_INFLATER = "java.util.zip.Inflater";

    private static final MethodMatcher END_METHOD_MATCHER = new MethodMatcher("java.util.zip..* end()");

    @Override
    public String getDisplayName() {
        return "Use try-with-resources for `Inflater` and `Deflater`";
    }

    @Override
    public String getDescription() {
        return "Convert manual resource management with `end()` calls to try-with-resources statements for `Inflater` and `Deflater` classes in Java 25+.";
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
                    public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                        J.Block b = super.visitBlock(block, ctx);

                        // Phase 1: Collect information about Inflater/Deflater usage
                        ResourceUsageCollector collector = new ResourceUsageCollector();
                        collector.visit(b, ctx);

                        if (collector.resourceUsages.isEmpty()) {
                            return b;
                        }

                        // Phase 2: Transform statements to use try-with-resources
                        return transformToTryWithResources(b, collector.resourceUsages, ctx);
                    }

                    private J.Block transformToTryWithResources(J.Block block, Map<String, ResourceUsage> resourceUsages, ExecutionContext ctx) {
                        List<Statement> newStatements = new ArrayList<>();
                        List<Statement> originalStatements = block.getStatements();

                        for (int i = 0; i < originalStatements.size(); i++) {
                            Statement stmt = originalStatements.get(i);

                            // Check if this statement starts a resource usage pattern
                            if (stmt instanceof J.VariableDeclarations) {
                                J.VariableDeclarations varDecl = (J.VariableDeclarations) stmt;
                                if (isInflaterOrDeflaterDeclaration(varDecl)) {
                                    String varName = varDecl.getVariables().get(0).getSimpleName();
                                    ResourceUsage usage = resourceUsages.get(varName);

                                    if (usage != null && usage.hasEndCall) {
                                        // Create try-with-resources block
                                        List<Statement> tryBodyStatements = extractStatementsBetween(originalStatements, i + 1, usage.lastUsageIndex);
                                        List<J.Try.Catch> catches = usage.existingCatches;
                                        J.Block finallyBlock = usage.nonEmptyFinally;

                                        J.Block tryWithResources = createTryWithResourcesBlock(varDecl, tryBodyStatements, catches, finallyBlock, ctx);
                                        newStatements.add(tryWithResources);

                                        // Skip processed statements
                                        i = usage.endCallIndex;
                                        continue;
                                    }
                                }
                            }

                            // Skip statements that were already processed as part of try-with-resources
                            if (shouldSkipStatement(stmt, resourceUsages)) {
                                continue;
                            }

                            newStatements.add(stmt);
                        }

                        return block.withStatements(newStatements);
                    }

                    private boolean shouldSkipStatement(Statement stmt, Map<String, ResourceUsage> resourceUsages) {
                        // Skip end() calls that are being converted
                        if (stmt instanceof Expression && stmt instanceof J.MethodInvocation) {
                            J.MethodInvocation mi = (J.MethodInvocation) stmt;
                            if (END_METHOD_MATCHER.matches(mi) && mi.getSelect() instanceof J.Identifier) {
                                String varName = ((J.Identifier) mi.getSelect()).getSimpleName();
                                return resourceUsages.containsKey(varName);
                            }
                        }
                        return false;
                    }

                    private List<Statement> extractStatementsBetween(List<Statement> statements, int start, int end) {
                        List<Statement> result = new ArrayList<>();
                        for (int i = start; i <= end; i++) {
                            if (i < statements.size()) {
                                Statement stmt = statements.get(i);
                                // Don't include end() calls in the try body
                                if (!isEndCall(stmt)) {
                                    result.add(stmt);
                                }
                            }
                        }
                        return result;
                    }

                    private boolean isEndCall(Statement stmt) {
                        if (stmt instanceof Expression && stmt instanceof J.MethodInvocation) {
                            J.MethodInvocation mi = (J.MethodInvocation) stmt;
                            return END_METHOD_MATCHER.matches(mi);
                        }
                        return false;
                    }

                    private J.Block createTryWithResourcesBlock(J.VariableDeclarations varDecl, List<Statement> tryBody,
                                                            List<J.Try.Catch> catches, J.Block finallyBlock, ExecutionContext ctx) {

                        J.VariableDeclarations.NamedVariable var = varDecl.getVariables().get(0);
                        String resourceDeclaration = String.format("%s %s = %s",
                            getTypeString(varDecl),
                            var.getSimpleName(),
                            getInitializerString(var));

                        // Build try-with-resources template
                        StringBuilder template = new StringBuilder();
                        template.append("try (").append(resourceDeclaration).append(") {\n");
                        for (int i = 0; i < tryBody.size(); i++) {
                            template.append("    #{any(java.lang.Object)}\n");
                        }
                        template.append("}");

                        // Add catch blocks if they exist
                        for (J.Try.Catch catchClause : catches) {
                            template.append(" catch (").append(getCatchParameterString(catchClause)).append(") {\n");
                            template.append("    #{any(java.lang.Object)}\n");
                            template.append("}");
                        }

                        // Add finally block if it exists and is non-empty
                        if (finallyBlock != null && !finallyBlock.getStatements().isEmpty()) {
                            template.append(" finally {\n");
                            for (int i = 0; i < finallyBlock.getStatements().size(); i++) {
                                template.append("    #{any(java.lang.Object)}\n");
                            }
                            template.append("}");
                        }

                        JavaTemplate javaTemplate = JavaTemplate.builder(template.toString())
                                .contextSensitive()
                                .build();

                        // Collect template arguments
                        List<Object> templateArgs = new ArrayList<>();
                        templateArgs.addAll(tryBody);

                        for (J.Try.Catch catchClause : catches) {
                            templateArgs.addAll(catchClause.getBody().getStatements());
                        }

                        if (finallyBlock != null) {
                            templateArgs.addAll(finallyBlock.getStatements());
                        }

                        return javaTemplate.apply(getCursor(), varDecl.getCoordinates().replace(),
                                templateArgs.toArray());
                    }

                    private String getTypeString(J.VariableDeclarations varDecl) {
                        if (varDecl.getTypeExpression() != null) {
                            return varDecl.getTypeExpression().toString();
                        }
                        return "var"; // fallback
                    }

                    private String getInitializerString(J.VariableDeclarations.NamedVariable var) {
                        if (var.getInitializer() != null) {
                            return var.getInitializer().toString();
                        }
                        return "null"; // fallback
                    }

                    private String getCatchParameterString(J.Try.Catch catchClause) {
                        return catchClause.getParameter().toString();
                    }

                    private boolean isInflaterOrDeflater(JavaType type) {
                        return TypeUtils.isAssignableTo(JAVA_UTIL_ZIP_DEFLATER, type) ||
                               TypeUtils.isAssignableTo(JAVA_UTIL_ZIP_INFLATER, type);
                    }

                    private boolean isInflaterOrDeflaterDeclaration(J.VariableDeclarations varDecl) {
                        return isInflaterOrDeflater(varDecl.getType());
                    }

                    // Inner class to collect resource usage information
                    private class ResourceUsageCollector extends JavaIsoVisitor<ExecutionContext> {
                        Map<String, ResourceUsage> resourceUsages = new HashMap<>();
                        int statementIndex = 0;

                        @Override
                        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                            if (isInflaterOrDeflaterDeclaration(multiVariable)) {
                                for (J.VariableDeclarations.NamedVariable var : multiVariable.getVariables()) {
                                    resourceUsages.put(var.getSimpleName(), new ResourceUsage(var.getSimpleName(), statementIndex));
                                }
                            }
                            statementIndex++;
                            return super.visitVariableDeclarations(multiVariable, ctx);
                        }

                        @Override
                        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                            if (END_METHOD_MATCHER.matches(method) && method.getSelect() instanceof J.Identifier) {
                                String varName = ((J.Identifier) method.getSelect()).getSimpleName();
                                ResourceUsage usage = resourceUsages.get(varName);
                                if (usage != null) {
                                    usage.hasEndCall = true;
                                    usage.endCallIndex = statementIndex;
                                }
                            } else if (method.getSelect() instanceof J.Identifier) {
                                String varName = ((J.Identifier) method.getSelect()).getSimpleName();
                                ResourceUsage usage = resourceUsages.get(varName);
                                if (usage != null) {
                                    usage.lastUsageIndex = statementIndex;
                                }
                            }
                            return super.visitMethodInvocation(method, ctx);
                        }

                        @Override
                        public Statement visitStatement(Statement statement, ExecutionContext ctx) {
                            statementIndex++;
                            return super.visitStatement(statement, ctx);
                        }
                    }

                    // Data class to hold resource usage information
                    private class ResourceUsage {
                        final String variableName;
                        final int declarationIndex;
                        boolean hasEndCall = false;
                        int endCallIndex = -1;
                        int lastUsageIndex = -1;
                        List<J.Try.Catch> existingCatches = new ArrayList<>();
                        J.Block nonEmptyFinally = null;

                        ResourceUsage(String variableName, int declarationIndex) {
                            this.variableName = variableName;
                            this.declarationIndex = declarationIndex;
                        }
                    }
                });
    }
}
