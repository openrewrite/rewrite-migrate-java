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
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class NormalizeSetter extends ScanningRecipe<NormalizeSetter.MethodAcc> {

    @Override
    public String getDisplayName() {
        return "Rename setter methods to fit lombok";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Rename methods that are effectively setter to the name lombok would give them.\n" +
               "Limitations:\n" +
               " - If two methods in a class are effectively the same setter then one's name will be corrected and the others name will be left as it is." +
               " - If the correct name for a method is already taken by another method then the name will not be corrected." +
               " - Method name swaps or circular renaming within a class cannot be performed because the names block each other. " +
               "E.g. `int getFoo() { return ba; } int getBa() { return foo; }` stays as it is.";
    }

    public static class MethodAcc  {
        List<RenameRecord> renameRecords = new ArrayList<>();
    }

    @Override
    public MethodAcc getInitialValue(ExecutionContext ctx) {
        return new MethodAcc();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(MethodAcc acc) {
        return new MethodRecorder(acc);
    }

    @Value
    private static class RenameRecord {
        String pathToClass_;
        String methodName_;
        String parameterType_;
        String newMethodName_;
    }


    @RequiredArgsConstructor
    private static class MethodRecorder extends JavaIsoVisitor<ExecutionContext> {

        private final static String METHOD_BLACKLIST = "METHOD_BLACKLIST";

        private final MethodAcc acc;

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {

            List<String> blackList = classDecl.getBody().getStatements().stream()
                    .filter(s -> s instanceof J.MethodDeclaration)
                    .map(s -> (J.MethodDeclaration) s)
                    .map(J.MethodDeclaration::getSimpleName)
                    .collect(Collectors.toList());

            getCursor().putMessage(METHOD_BLACKLIST, blackList);

            super.visitClassDeclaration(classDecl, ctx);

            return classDecl;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            assert method.getMethodType() != null;

            if (!LombokUtils.isEffectivelySetter(method)) {
                return method;
            }

            //return early if the method overrides another
            //if the project defined both the original and the overridden method,
            // then the renaming of the "original" in the base class will cover the override
            if (method.getLeadingAnnotations().stream().anyMatch(a -> "Override".equals(a.getSimpleName()))) {
                return method;
            }

            J.Assignment assignment_ = (J.Assignment) method.getBody().getStatements().get(0);
            J.FieldAccess fieldAccess = (J.FieldAccess) assignment_.getVariable();
            JavaType.Variable fieldType = fieldAccess.getName().getFieldType();

            String expectedMethodName = LombokUtils.deriveSetterMethodName(fieldType);
            String parameterType =  fieldType.getType().toString();
            String actualMethodName = method.getSimpleName();

            //if method already has the name it should have, then nothing to be done
            if (expectedMethodName.equals(actualMethodName)) {
                return method;
            }

            //If the desired method name is already taken by an existing method, the current method cannot be renamed
            List<String> blackList = getCursor().getNearestMessage(METHOD_BLACKLIST);
            assert blackList != null;
            if (blackList.contains(expectedMethodName)) {
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
                            pathToClass,
                            actualMethodName,
                            parameterType,
                            expectedMethodName
                    )
            );
            blackList.remove(actualMethodName);//actual method name becomes available again
            blackList.add(expectedMethodName);//expected method name now blocked
            return method;
        }
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(MethodAcc acc) {

        return new TreeVisitor<Tree, ExecutionContext>() {

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {

                for (RenameRecord rr : acc.renameRecords) {
                    String methodPattern = String.format("%s %s(%s)", rr.pathToClass_, rr.methodName_, rr.parameterType_);
                    tree = new ChangeMethodName(methodPattern, rr.newMethodName_, true, null)
                            .getVisitor().visit(tree, ctx);
                }
                return tree;
            }
        };
    }
}
