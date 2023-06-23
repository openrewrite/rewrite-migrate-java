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

package org.openrewrite.java.migrate.util;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

import java.util.Objects;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveFinalizerFromZip extends Recipe {

    @Override
    public String getDisplayName() {
        return "Remove invocations of deprecated invocations from Deflater, Inflater, ZipFile ";
    }

    @Override
    public String getDescription() {
        return "Remove invocations of finalize() deprecated invocations from Deflater, Inflater, ZipFile.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(
                                new UsesType<>("java.util.zip.Deflater", false),
                                new UsesType<>("java.util.zip.Inflater", false),
                                new UsesType<>("java.util.zip.ZipFile", false)),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                        J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
                        if (Objects.nonNull(cd.getExtends()) &&
                                (String.valueOf(cd.getExtends().getType()).equals("java.util.zip.Deflater")
                                        || String.valueOf(cd.getExtends().getType()).equals("java.util.zip.Inflater")
                                        || String.valueOf(cd.getExtends().getType()).equals("java.util.zip.ZipFile"))
                        ) {
                            String currentClassName = cd.getSimpleName();
                            cd = cd.withBody(cd.getBody().withStatements(ListUtils.map(cd.getBody().getStatements(), mdStmt -> {
                                if (mdStmt instanceof J.MethodDeclaration) {
                                    J.MethodDeclaration md = (J.MethodDeclaration) mdStmt;
                                    mdStmt = md.withBody(md.getBody().withStatements(ListUtils.map(md.getBody().getStatements(), miStmt ->
                                    {
                                        if (miStmt instanceof J.MethodInvocation) {
                                            J.MethodInvocation mi = (J.MethodInvocation) miStmt;
                                            if (Objects.nonNull(mi.getSelect())
                                                    && String.valueOf(mi.getSelect().getType()).equals(currentClassName)
                                                    && String.valueOf(mi.getName()).contains("finalize")) {
                                                miStmt = null;
                                            }
                                        }
                                        return miStmt;
                                    })));
                                }
                                return mdStmt;
                            })));
                            return cd;
                        }
                        return cd;
                    }

                });
    }

}