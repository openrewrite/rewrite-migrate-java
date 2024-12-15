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
import lombok.Setter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.Set;

import static org.openrewrite.java.tree.JavaType.Variable;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseLombokSetter extends Recipe {

    @Override
    public String getDisplayName() {
        return "Convert setter methods to annotations";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Convert trivial setter methods to `@Setter` annotations on their respective fields.\n\n" +
                "Limitations:\n\n" +
                " - Does not add a dependency to Lombok, users need to do that manually\n" +
                " - Ignores fields that are declared on the same line as others, e.g. `private int foo, bar; " +
                "Users who have such fields are advised to separate them beforehand with [org.openrewrite.staticanalysis.MultipleVariableDeclaration](https://docs.openrewrite.org/recipes/staticanalysis/multiplevariabledeclarations).\n" +
                " - Does not offer any of the configuration keys listed in https://projectlombok.org/features/GetterSetter.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("lombok");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MethodRemover();
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class MethodRemover extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.@Nullable MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            if (LombokUtils.isEffectivelySetter(method)) {
                J.Assignment assignment_ = (J.Assignment) method.getBody().getStatements().get(0);
                J.FieldAccess fieldAccess = (J.FieldAccess) assignment_.getVariable();

                Variable fieldType = fieldAccess.getName().getFieldType();
                boolean nameMatch = method.getSimpleName().equals(LombokUtils.deriveSetterMethodName(fieldType));
                if (nameMatch) {
                    doAfterVisit(new FieldAnnotator(
                            Setter.class,
                            fieldType,
                            LombokUtils.getAccessLevel(method)
                    ));
                    return null; //delete
                }
            }
            return method;
        }
    }
}
