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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Value
@EqualsAndHashCode(callSuper = false)
public class NormalizeGetter extends ScanningRecipe<List<NormalizeGetter.RenameRecord>> {

    private final static String DO_NOT_RENAME = "DO_NOT_RENAME";

    @Override
    public String getDisplayName() {
        return "Rename getter methods to fit lombok";
    }

    @Override
    public String getDescription() {
        return "Rename methods that are effectively getter to the name lombok would give them.\n\n" +
                "Limitations:\n" +
                " - If two methods in a class are effectively the same getter then one's name will be corrected and the others name will be left as it is.\n" +
                " - If the correct name for a method is already taken by another method then the name will not be corrected.\n" +
                " - Method name swaps or circular renaming within a class cannot be performed because the names block each other. " +
                "E.g. `int getFoo() { return ba; } int getBa() { return foo; }` stays as it is."
                ;
    }

    @Value
    public static class RenameRecord {
        String pathToClass_;
        String methodName_;
        String newMethodName_;
    }

    @Override
    public List<RenameRecord> getInitialValue(ExecutionContext ctx) {
        return new ArrayList<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(List<RenameRecord> renameRecords) {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                List<String> doNotRename = classDecl.getBody().getStatements().stream()
                        .filter(s -> s instanceof J.MethodDeclaration)
                        .map(s -> (J.MethodDeclaration) s)
                        .map(J.MethodDeclaration::getSimpleName)
                        .collect(toList());
                getCursor().putMessage(DO_NOT_RENAME, doNotRename);

                return super.visitClassDeclaration(classDecl, ctx);
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                assert method.getMethodType() != null;

                if (!LombokUtils.isEffectivelyGetter(method)) {
                    return method;
                }

                //return early if the method overrides another
                //if the project defined both the original and the overridden method,
                // then the renaming of the "original" in the base class will cover the override
                if (method.getLeadingAnnotations().stream().anyMatch(a -> "Override".equals(a.getSimpleName()))) {
                    return method;
                }

                Expression returnExpression = ((J.Return) method.getBody().getStatements().get(0)).getExpression();

                String simpleName;
                if (returnExpression instanceof J.Identifier) {
                    simpleName = ((J.Identifier) returnExpression).getSimpleName();
                } else if (returnExpression instanceof J.FieldAccess) {
                    simpleName = ((J.FieldAccess) returnExpression).getSimpleName();
                } else {
                    //only those types above are possible, see LombokUtils::isEffectivelyGetter
                    throw new IllegalStateException("Unexpected type for returned variable");
                }

                String expectedMethodName = LombokUtils.deriveGetterMethodName(returnExpression.getType(), simpleName);
                String actualMethodName = method.getSimpleName();

                // If method already has the name it should have, then nothing to be done
                if (expectedMethodName.equals(actualMethodName)) {
                    return method;
                }

                // If the desired method name is already taken by an existing method, the current method cannot be renamed
                List<String> doNotRename = getCursor().getNearestMessage(DO_NOT_RENAME);
                assert doNotRename != null;
                if (doNotRename.contains(expectedMethodName)) {
                    return method;
                }
                //WON'T DO: there is a rare edge case, that is not addressed yet.
                // If `getFoo()` returns `ba` and `getBa()` returns `foo` then neither will be renamed.
                // This could be fixed by compiling a list of planned changes and doing a soundness check (and not renaming sequentially, or rather introducing temporary method names)
                // At this point I don't think it's worth the effort.


                String pathToClass = method.getMethodType().getDeclaringType().getFullyQualifiedName().replace('$', '.');
                //todo write separate recipe for merging effective getters
                renameRecords.add(new RenameRecord(pathToClass, actualMethodName, expectedMethodName));
                doNotRename.remove(actualMethodName); //actual method name becomes available again
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
                    String methodPattern = String.format("%s %s()", rr.pathToClass_, rr.methodName_);
                    tree = new ChangeMethodName(methodPattern, rr.newMethodName_, true, null)
                            .getVisitor().visit(tree, ctx);
                }
                return tree;
            }
        };
    }
}
