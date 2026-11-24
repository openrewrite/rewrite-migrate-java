/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.jersey;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveUnusedJersey1Imports extends Recipe {

    String displayName = "Remove unused Jersey 1 imports";

    String description = "Removes the imports of `com.sun.jersey` types and packages that the code does not use, which " +
            "no longer compile once the Jersey 1 dependencies are gone. An import of a Jersey 1 type that the code " +
            "still uses is kept, for the compiler to point out.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("com.sun.jersey..*", true), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                for (J.Import anImport : cu.getImports()) {
                    String packageName = anImport.getPackageName();
                    if (!"com.sun.jersey".equals(packageName) && !packageName.startsWith("com.sun.jersey.")) {
                        continue;
                    }
                    String name = anImport.getQualid().getSimpleName();
                    if (anImport.isStatic()) {
                        maybeRemoveImport(anImport.getTypeName() + "." + name);
                    } else if ("*".equals(name)) {
                        maybeRemoveImport(packageName + ".*");
                    } else {
                        maybeRemoveImport(anImport.getTypeName());
                    }
                }
                return cu;
            }
        });
    }
}
