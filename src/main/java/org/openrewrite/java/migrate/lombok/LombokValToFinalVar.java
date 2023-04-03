/*
 * Copyright 2022 the original author or authors.
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

import org.openrewrite.Applicability;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class LombokValToFinalVar extends Recipe {

    private static final String LOMBOK_VAL = "lombok.val";

    @Override
    public String getDisplayName() {
        return "Prefer `final var`";
    }

    @Override
    public String getDescription() {
        return "Prefer the Java standard library's `final var` over third-party usage of Lombok's `lombok.val` in Java 10 or higher.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("lombok");
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return Applicability.and(
//                new UsesJavaVersion<>(11),
                new UsesType<>(LOMBOK_VAL, false));
    }

    @Override
    protected LombokValToFinalVarVisitor getVisitor() {
        return new LombokValToFinalVarVisitor();
    }

    private static class LombokValToFinalVarVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations mv, ExecutionContext ctx) {
            J.VariableDeclarations varDecls = super.visitVariableDeclarations(mv, ctx);
            if (TypeUtils.isOfClassType(varDecls.getType(), LOMBOK_VAL)) {
                maybeRemoveImport(LOMBOK_VAL);

                J.VariableDeclarations.NamedVariable nv = mv.getVariables().get(0);

                String finalVarVariableTemplateString;
                Object[] args;
                if (nv.getInitializer() == null) {
                    finalVarVariableTemplateString = "final var #{}";
                    args = new Object[]{nv.getSimpleName()};
                } else {
                    finalVarVariableTemplateString = "final var #{} = #{any()};";
                    args = new Object[]{nv.getSimpleName(), nv.getInitializer()};
                }
                varDecls = mv.withTemplate(
                        JavaTemplate.builder(this::getCursor, finalVarVariableTemplateString)
                                .javaParser(JavaParser.fromJavaVersion())
                                .build(),
                        varDecls.getCoordinates().replace(), args);

                if (nv.getInitializer() != null) {
                    varDecls = varDecls.withVariables(ListUtils.map(varDecls.getVariables(), namedVar -> namedVar.withInitializer(namedVar.getInitializer().withPrefix(nv.getInitializer().getPrefix()))));
                }


            }
            return varDecls;
        }
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

}
