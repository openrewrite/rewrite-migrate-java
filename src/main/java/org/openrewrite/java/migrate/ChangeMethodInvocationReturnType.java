package org.openrewrite.java.migrate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;

import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.*;
import org.openrewrite.java.*;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeMethodInvocationReturnType extends Recipe {

    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method declarations/invocations.",
            example = "org.mockito.Matchers anyVararg()")
    String methodPattern;

    @Option(displayName = "New method invocation return type",
            description = "The return return type of method invocation.",
            example = "long")
    String newReturnType;

    @Override
    public String getDisplayName() {
        return "Change method invocation return type";
    }

    @Override
    public String getDescription() {
        return "Changes the return type of a method invocation.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        JavaIsoVisitor<ExecutionContext> condition = new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J visit(@Nullable Tree tree, ExecutionContext ctx) {
                return super.visit(tree, ctx);
            }
        };

        return new JavaIsoVisitor<ExecutionContext>() {
            private final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, false);

            private boolean methodUpdated;

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                JavaType.Method type = m.getMethodType();
                if (methodMatcher.matches(method) && type != null && !type.getReturnType().toString().equals(newReturnType)) {
                    type = type.withReturnType(JavaType.buildType(newReturnType));
                    m = m.withMethodType(type);
                    methodUpdated = true;
                }
                return m;
            }

           @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
               methodUpdated = false;
                JavaType.FullyQualified typeAsClass = multiVariable.getTypeAsFullyQualified();
                J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, ctx);

                if (methodUpdated) {
                    JavaType newType = JavaType.buildType(newReturnType);
                    JavaType.FullyQualified newFieldType = TypeUtils.asFullyQualified(newType);

                    maybeAddImport(newFieldType);
                    maybeRemoveImport(typeAsClass);

                    mv = mv.withTypeExpression(mv.getTypeExpression() == null ?
                            null :
                            new J.Identifier(mv.getTypeExpression().getId(),
                                    mv.getTypeExpression().getPrefix(),
                                    Markers.EMPTY,
                                    emptyList(),
                                    newReturnType,
                                    newType,
                                    null
                            )
                    );

                    mv = mv.withVariables(ListUtils.map(mv.getVariables(), var -> {
                        JavaType.FullyQualified varType = TypeUtils.asFullyQualified(var.getType());
                        if (varType != null && !varType.equals(newType)) {
                            return var.withType(newType).withName(var.getName().withType(newType));
                        }
                        return var;
                    }));
                }

                return mv;
            }
        };
    }
}
