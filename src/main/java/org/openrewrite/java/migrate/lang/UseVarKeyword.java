/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.migrate.lang;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.migrate.lang.var.DeclarationCheck;
import org.openrewrite.java.search.HasJavaVersion;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.time.Duration;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseVarKeyword extends Recipe {
    public String getDisplayName() {
        return "Use local variable type-inference (var) where possible";
    }

    @Override
    public String getDescription() {
        return "Local variable type-inference reduce the noise produces by repeating the type definitions in Java 10 or higher.";
    }

    @Override
    public @Nullable Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new HasJavaVersion("10", true),
                new UseVarKeywordVisitor());
    }

    static final class UseVarKeywordVisitor extends JavaVisitor<ExecutionContext> {
        private final JavaTemplate template = JavaTemplate.builder("var #{} = #{any()}")
                .javaParser(JavaParser.fromJavaVersion()).build();

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
            J.VariableDeclarations vd = (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, executionContext);

            boolean isGeneralApplicable = DeclarationCheck.isVarApplicable(getCursor(), vd);
            if (!isGeneralApplicable) return vd;

            //todo move this block to UseVarForPrimitives
            boolean isPrimitive = DeclarationCheck.isPrimitive(vd);
            boolean isGeneric = DeclarationCheck.useGenerics(vd);
            boolean useTernary = DeclarationCheck.initializedByTernary(vd);
            if (isPrimitive || isGeneric || useTernary) return vd;

            return transformToVar(vd);
        }

        @NotNull
        private J.VariableDeclarations transformToVar(@NotNull J.VariableDeclarations vd) {
            Expression initializer = vd.getVariables().get(0).getInitializer();
            String simpleName = vd.getVariables().get(0).getSimpleName();

            return template.apply(this.getCursor(), vd.getCoordinates().replace(), simpleName, initializer);
        }
    }
}
