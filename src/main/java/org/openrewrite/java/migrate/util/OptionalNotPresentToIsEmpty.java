package org.openrewrite.java.migrate.util;

import org.openrewrite.Applicability;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Unary.Type;
import org.openrewrite.java.tree.Statement;

import java.time.Duration;

public class OptionalNotPresentToIsEmpty extends Recipe {

    @Override
    public String getDisplayName() {
        return "Replace !optional.isPresent() with optional.isEmpty()";
    }

    @Override
    public String getDescription() {
        return "Replace negated Optional.isPresent() calls with Optional.isEmpty() in Java 11 and above.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return Applicability.and(
                new UsesJavaVersion<>(11),
                new UsesMethod<>("java.util.Optional isPresent()"));
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public Statement visitStatement(Statement s, ExecutionContext p) {
                Statement statement = (Statement) super.visitStatement(s, p);
                if (statement instanceof J.Unary) {
                    J.Unary unary = (J.Unary) statement;
                    if (unary.getOperator() == Type.Not) {
                        Expression expression = unary.getExpression();
                        if (expression instanceof J.MethodInvocation) {
                            J.MethodInvocation methodInvocation = (J.MethodInvocation) expression;
                            if (new MethodMatcher("java.util.Optional isPresent()").matches(methodInvocation)) {
                                JavaTemplate javaTemplate = JavaTemplate.builder(this::getCursor, "#{any()}.isEmpty()")
                                        .javaParser(() -> JavaParser.fromJavaVersion().build())
                                        .build();
                                return statement.withTemplate(
                                        javaTemplate,
                                        statement.getCoordinates().replace(),
                                        methodInvocation.getSelect());
                            }
                        }
                    }
                }
                return statement;
            }
        };
    }
}
