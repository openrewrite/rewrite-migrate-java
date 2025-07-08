/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.lombok;

import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.MaybeUsesImport;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.service.AnnotationService;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class LombokValToFinalVar extends Recipe {

    private static final String LOMBOK_VAL = "lombok.val";
    private static final String LOMBOK_VAR = "lombok.var";

    @Override
    public String getDisplayName() {
        return "Prefer `final var` over `lombok.val`";
    }

    @Override
    public String getDescription() {
        return "Prefer the Java standard library's `final var` and `var` over third-party usage of Lombok's `lombok.val` and `lombok.var` in Java 10 or higher.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("lombok");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> check = Preconditions.or(
                new UsesType<>(LOMBOK_VAL, false),
                // The parser does not recognize `var` as coming from Lombok, which happens to align with what we want
                new MaybeUsesImport<>(LOMBOK_VAR));
        return Preconditions.check(check, new LombokValToFinalVarVisitor());
    }

    private static class LombokValToFinalVarVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit compilationUnit, ExecutionContext ctx) {
            maybeRemoveImport(LOMBOK_VAR);
            return super.visitCompilationUnit(compilationUnit, ctx);
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations mv, ExecutionContext ctx) {
            J.VariableDeclarations varDecls = super.visitVariableDeclarations(mv, ctx);

            if (TypeUtils.isOfClassType(varDecls.getType(), LOMBOK_VAL) ||
                    (varDecls.getTypeExpression() instanceof J.Identifier && "val".equals(((J.Identifier) varDecls.getTypeExpression()).getSimpleName()))) {
                maybeRemoveImport(LOMBOK_VAL);

                J.VariableDeclarations.NamedVariable nv = mv.getVariables().get(0);
                if (nv.getInitializer() == null) {
                    // manually transform to var, as val in this case has no sufficient type information
                    // and the java template parsing would fail, see https://github.com/openrewrite/rewrite/pull/5637
                    TypeTree typeExpression = varDecls.getTypeExpression();
                    J.Identifier varType = new J.Identifier(Tree.randomId(),
                            typeExpression.getPrefix(),
                            typeExpression.getMarkers(),
                            service(AnnotationService.class).getAllAnnotations(getCursor()),
                            "var",
                            nv.getType(),
                            null);
                    return varDecls.withTypeExpression(varType);
                }

                varDecls = JavaTemplate.builder("final var #{} = #{any()};")
                        .contextSensitive()
                        .build()
                        .apply(updateCursor(varDecls), varDecls.getCoordinates().replace(), nv.getSimpleName(), nv.getInitializer());
                varDecls = varDecls.withVariables(ListUtils.map(varDecls.getVariables(), namedVar -> namedVar
                        .withInitializer(namedVar.getInitializer().withPrefix(nv.getInitializer().getPrefix()))));
            }
            return varDecls;
        }
    }
}
