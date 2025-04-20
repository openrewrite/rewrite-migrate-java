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
package org.openrewrite.java.migrate.lombok;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@Value
@EqualsAndHashCode(callSuper = false)
public class NormalizeGetter extends ScanningRecipe<List<NormalizeGetter.RenameRecord>> {

    private final static String DO_NOT_RENAME = "DO_NOT_RENAME";

    @Override
    public String getDisplayName() {
        return "Rename getter methods to fit Lombok";
    }

    @Override
    public String getDescription() {
        return "Rename methods that are effectively getter to the name Lombok would give them.\n\n" +
                "Limitations:\n" +
                " - If two methods in a class are effectively the same getter then one's name will be corrected and the others name will be left as it is.\n" +
                " - If the correct name for a method is already taken by another method then the name will not be corrected.\n" +
                " - Method name swaps or circular renaming within a class cannot be performed because the names block each other.\n" +
                "E.g. `int getFoo() { return ba; } int getBa() { return foo; }` stays as it is."
                ;
    }

    @Value
    public static class RenameRecord {
        String methodPattern;
        String newMethodName;
    }

    @Override
    public List<RenameRecord> getInitialValue(ExecutionContext ctx) {
        return new ArrayList<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(List<RenameRecord> renameRecords) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                // Cheaply collect all declared methods; this also means we do not support clashing nested class methods
                Set<JavaType.Method> declaredMethods = cu.getTypesInUse().getDeclaredMethods();
                List<String> existingMethodNames = new ArrayList<>();
                for (JavaType.Method method : declaredMethods) {
                    existingMethodNames.add(method.getName());
                }
                getCursor().putMessage(DO_NOT_RENAME, existingMethodNames);
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                if (method.getMethodType() == null || method.getBody() == null ||
                        !LombokUtils.isEffectivelyGetter(method) ||
                        TypeUtils.isOverride(method.getMethodType())) {
                    return method;
                }

                String simpleName;
                Expression returnExpression = ((J.Return) method.getBody().getStatements().get(0)).getExpression();
                if (returnExpression instanceof J.Identifier) {
                    simpleName = ((J.Identifier) returnExpression).getSimpleName();
                } else if (returnExpression instanceof J.FieldAccess) {
                    simpleName = ((J.FieldAccess) returnExpression).getSimpleName();
                } else {
                    return method;
                }

                // If method already has the name it should have, then nothing to be done
                String expectedMethodName = LombokUtils.deriveGetterMethodName(returnExpression.getType(), simpleName);
                if (expectedMethodName.equals(method.getSimpleName())) {
                    return method;
                }

                // If the desired method name is already taken by an existing method, the current method cannot be renamed
                List<String> doNotRename = getCursor().getNearestMessage(DO_NOT_RENAME);
                assert doNotRename != null;
                if (doNotRename.contains(expectedMethodName)) {
                    return method;
                }

                renameRecords.add(new RenameRecord(MethodMatcher.methodPattern(method), expectedMethodName));
                doNotRename.remove(method.getSimpleName()); //actual method name becomes available again
                doNotRename.add(expectedMethodName); //expected method name now blocked
                return method;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(List<RenameRecord> renameRecords) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                for (RenameRecord rr : renameRecords) {
                    tree = new ChangeMethodName(rr.methodPattern, rr.newMethodName, true, null)
                            .getVisitor().visit(tree, ctx);
                }
                return tree;
            }
        };
    }
}
