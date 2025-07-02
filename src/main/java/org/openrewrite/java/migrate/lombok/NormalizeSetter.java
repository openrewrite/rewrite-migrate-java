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
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class NormalizeSetter extends ScanningRecipe<NormalizeSetter.MethodAcc> {

    private final static String DO_NOT_RENAME = "DO_NOT_RENAME";

    @Override
    public String getDisplayName() {
        return "Rename setter methods to fit Lombok";
    }

    @Override
    public String getDescription() {
        return "Rename methods that are effectively setter to the name Lombok would give them.\n" +
                "Limitations:\n" +
                " - If two methods in a class are effectively the same setter then one's name will be corrected and the others name will be left as it is.\n" +
                " - If the correct name for a method is already taken by another method then the name will not be corrected.\n" +
                " - Method name swaps or circular renaming within a class cannot be performed because the names block each other.\n " +
                "E.g. `int getFoo() { return ba; } int getBa() { return foo; }` stays as it is.";
    }

    public static class MethodAcc {
        List<RenameRecord> renameRecords = new ArrayList<>();
    }

    @Value
    private static class RenameRecord {
        String methodPattern;
        String parameterType_;
        String newMethodName_;
    }

    @Override
    public MethodAcc getInitialValue(ExecutionContext ctx) {
        return new MethodAcc();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(MethodAcc acc) {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {

                List<String> doNotRename = classDecl.getBody().getStatements().stream()
                        .filter(s -> s instanceof J.MethodDeclaration)
                        .map(s -> (J.MethodDeclaration) s)
                        .map(J.MethodDeclaration::getSimpleName)
                        .collect(Collectors.toList());

                getCursor().putMessage(DO_NOT_RENAME, doNotRename);

                super.visitClassDeclaration(classDecl, ctx);

                return classDecl;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                if (method.getMethodType() == null || method.getBody() == null ||
                        !LombokUtils.isEffectivelySetter(method) ||
                        TypeUtils.isOverride(method.getMethodType())) {
                    return method;
                }

                JavaType.Variable fieldType = extractVariable(method);

                String expectedMethodName = LombokUtils.deriveSetterMethodName(fieldType);
                String parameterType = fieldType.getType().toString();
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
                //todo write separate recipe for merging effective setters
                acc.renameRecords.add(
                        new RenameRecord(
                                MethodMatcher.methodPattern(method),
                                parameterType,
                                expectedMethodName
                        )
                );
                doNotRename.remove(actualMethodName);//actual method name becomes available again
                doNotRename.add(expectedMethodName);//expected method name now blocked
                return method;
            }

            private JavaType.@Nullable Variable extractVariable(J.MethodDeclaration method) {
                J.Assignment assignment_ = (J.Assignment) method.getBody().getStatements().get(0);

                JavaType.Variable fieldType;
                if (assignment_.getVariable() instanceof J.FieldAccess) {
                    J.FieldAccess fieldAccess = (J.FieldAccess) assignment_.getVariable();
                    fieldType = fieldAccess.getName().getFieldType();
                } else if (assignment_.getVariable() instanceof J.Identifier) {
                    J.Identifier fieldAccess = (J.Identifier) assignment_.getVariable();
                    fieldType = fieldAccess.getFieldType();
                } else {
                    //only those types above are possible, see LombokUtils::isEffectivelySetter
                    throw new IllegalStateException("Unexpected type for returned variable");
                }
                return fieldType;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(MethodAcc acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                for (RenameRecord rr : acc.renameRecords) {
                    tree = new ChangeMethodName(rr.methodPattern, rr.newMethodName_, true, null)
                            .getVisitor().visit(tree, ctx);
                }
                return tree;
            }
        };
    }
}
