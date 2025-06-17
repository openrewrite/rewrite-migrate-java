/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.migrate.lang;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.tree.*;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import javax.lang.model.SourceVersion;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

@Value
@EqualsAndHashCode(callSuper = false)
public class PatternMatchingWithInstanceOf extends Recipe {

    private static final List<BiFunction<J.InstanceOf, Statement, @Nullable String>> patternVariableNameGenerators = Arrays.asList(
            PatternMatchingWithInstanceOf::primitiveFirstLetter,
            PatternMatchingWithInstanceOf::typeAsCamelCasedName,
            PatternMatchingWithInstanceOf::expressionNameAsClassType
    );

    @Override
    public String getDisplayName() {
        return "Pattern matching for `instanceof`";
    }

    @Override
    public String getDescription() {
        return "[JEP 394](https://openjdk.org/jeps/394) describes how some instanceof statements and their variable scopes can be improved with pattern matching.\n" +
                "This recipe will add patterns to the if statements that do instanceOf checks and replace all cast usages of the checked variable with it pattern equivalent.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.not(new KotlinFileChecker<>()), new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitIf(J.If if_, ExecutionContext ctx) {
                if (if_.getIfCondition().getTree() instanceof J.InstanceOf && needsPatternVariable(if_)) {
                    String patternVariableName = calculatePatternVariableName(if_);
                    if (patternVariableName != null) {
                        J.InstanceOf patternMatcher = ensurePatternVariable(if_, patternVariableName);
                        if (patternMatcher != if_.getIfCondition().getTree()) {
                            J pattern = patternMatcher.getPattern();
                            if_ = if_.withIfCondition(if_.getIfCondition().withTree(patternMatcher));
                            return new JavaVisitor<ExecutionContext>() {
                                @Override
                                public J visitTypeCast(J.TypeCast typeCast, ExecutionContext ctx) {
                                    if (pattern != null && SemanticallyEqual.areEqual(typeCast.getExpression(), patternMatcher.getExpression())) {
                                        return pattern.withPrefix(typeCast.getPrefix());
                                    }
                                    return super.visitTypeCast(typeCast, ctx);
                                }

                                @Override
                                public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                                    List<J.VariableDeclarations.NamedVariable> mappedVariables = ListUtils.map(multiVariable.getVariables(), variable -> {
                                        if (pattern != null && variable != null) {
                                            Expression initializer = variable.getInitializer();
                                            if (initializer instanceof J.TypeCast) {
                                                J.TypeCast typeCast = (J.TypeCast) initializer;
                                                if (SemanticallyEqual.areEqual(typeCast.getClazz().getTree(), patternMatcher.getClazz()) &&
                                                        SemanticallyEqual.areEqual(typeCast.getExpression(), patternMatcher.getExpression())) {
                                                    return null;
                                                }
                                            }
                                        }
                                        return variable;
                                    });
                                    if (mappedVariables.isEmpty()) {
                                        return null;
                                    }
                                    return super.visitVariableDeclarations(multiVariable.withVariables(mappedVariables), ctx);
                                }
                            }.visitNonNull(if_, ctx);
                        }
                    }
                }

                return super.visitIf(if_, ctx);
            }

            private J.InstanceOf ensurePatternVariable(J.If if_, String patternVariableName) {
                if (if_.getIfCondition().getTree() instanceof J.InstanceOf) {
                    J.InstanceOf instanceOf = (J.InstanceOf) if_.getIfCondition().getTree();
                    if (instanceOf.getPattern() == null) {
                        J instanceOfClazz = instanceOf.getClazz();
                        if (instanceOfClazz instanceof J.Identifier) {
                            return instanceOf.withPattern(((J.InstanceOf) ((J.If) JavaTemplate.builder("if (#{} #{})")
                                    .contextSensitive()
                                    .build()
                                    .apply(getCursor(), if_.getCoordinates().replace(), instanceOf.toString(), patternVariableName))
                                    .getIfCondition()
                                    .getTree())
                                    .getPattern());
                        }
                    }
                    return instanceOf; // do not touch by default
                }

                throw new IllegalArgumentException("The if condition is not an instanceof-check and should not be passed to calculate a pattern variable.");
            }

            private @Nullable String calculatePatternVariableName(J.If if_) {
                if (if_.getIfCondition().getTree() instanceof J.InstanceOf) {
                    J.InstanceOf instanceOf = (J.InstanceOf) if_.getIfCondition().getTree();
                    if (instanceOf.getPattern() != null) {
                        return null; // already has a pattern variable
                    }
                    String castedVariableDeclared = variableDeclaredWithCastedValue(instanceOf, if_.getThenPart());
                    if (castedVariableDeclared != null) {
                        return castedVariableDeclared; // use the variable name that is already declared as that one will be removed
                    }

                    Set<String> identifiers = new HashSet<>();
                    J.Block variableScope = getCursor().firstEnclosing(J.Block.class);
                    if (variableScope == null) {
                        collectAllIdentifiersInThenPart(if_, identifiers);
                    } else {
                        collectAllIdentifiersInThenPart(variableScope, identifiers);
                    }

                    return patternVariableNameGenerators.stream()
                            .map(generator -> generator.apply(instanceOf, if_.getThenPart()))
                            .filter(Objects::nonNull)
                            .filter(candidate -> !identifiers.contains(candidate))
                            .filter(candidate -> JavaType.Primitive.fromKeyword(candidate) == null && SourceVersion.isIdentifier(candidate) && !SourceVersion.isKeyword(candidate))
                            .findFirst()
                            .orElse(null);
                }

                throw new IllegalArgumentException("The if condition is not an instanceof-check and should not be passed to calculate a pattern variable.");
            }

            private void collectAllIdentifiersInThenPart(J scope, Set<String> identifiers) {
                new JavaIsoVisitor<Set<String>>() {
                    @Override
                    public J.Identifier visitIdentifier(J.Identifier identifier, Set<String> identifiers) {
                        if (identifier.getFieldType() != null) {
                            identifiers.add(identifier.getSimpleName());
                        }
                        return identifier;
                    }
                }.visitNonNull(scope, identifiers);
            }

            private boolean needsPatternVariable(J.If if_) {
                if (if_.getIfCondition().getTree() instanceof J.InstanceOf) {
                    J.InstanceOf instanceOf = (J.InstanceOf) if_.getIfCondition().getTree();
                    if (instanceOf.getPattern() == null) {
                        J instanceOfClazz = instanceOf.getClazz();
                        AtomicBoolean hasCast = new AtomicBoolean(false);
                        new JavaIsoVisitor<AtomicBoolean>() {
                            @Override
                            public J.TypeCast visitTypeCast(J.TypeCast typeCast, AtomicBoolean hasCast) {
                                if (SemanticallyEqual.areEqual(typeCast.getClazz().getTree(), instanceOfClazz) &&
                                        SemanticallyEqual.areEqual(typeCast.getExpression(), instanceOf.getExpression())) {
                                    hasCast.set(true);
                                }
                                return super.visitTypeCast(typeCast, hasCast);
                            }
                        }.visitNonNull(if_.getThenPart(), hasCast);
                        return hasCast.get();
                    }
                }

                return false;
            }
        });
    }

    private static @Nullable String variableDeclaredWithCastedValue(J.InstanceOf instanceOf, Statement then) {
        AtomicReference<String> variableName = new AtomicReference<>();
        new JavaIsoVisitor<AtomicReference<String>>() {
            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, AtomicReference<String> stringAtomicReference) {
                J.VariableDeclarations declarations = super.visitVariableDeclarations(multiVariable, stringAtomicReference);
                declarations.getVariables().forEach(variable -> {
                    Expression initializer = variable.getInitializer();
                    if (initializer instanceof J.TypeCast) {
                        J.TypeCast typeCast = (J.TypeCast) initializer;
                        if (SemanticallyEqual.areEqual(typeCast.getClazz().getTree(), instanceOf.getClazz()) &&
                                SemanticallyEqual.areEqual(typeCast.getExpression(), instanceOf.getExpression())) {
                            stringAtomicReference.set(variable.getSimpleName());
                        }
                    }
                });
                return declarations;
            }
        }.visitNonNull(then, variableName);

        return variableName.get();
    }

    private static @Nullable String primitiveFirstLetter(J.InstanceOf instanceOf, Statement then) {
        if (instanceOf.getClazz() instanceof TypedTree) {
            JavaType.Primitive primitive = null;
            if (((TypedTree) instanceOf.getClazz()).getType() instanceof JavaType.FullyQualified) {
                primitive = JavaType.Primitive.fromClassName(((JavaType.FullyQualified) ((TypedTree) instanceOf.getClazz()).getType()).getFullyQualifiedName());
            } else if (((TypedTree) instanceOf.getClazz()).getType() instanceof JavaType.Primitive) {
                primitive = (JavaType.Primitive) ((TypedTree) instanceOf.getClazz()).getType();
            }
            if (primitive != null) {
                return primitive.getKeyword().toLowerCase().substring(0, 1);
            }
        }
        return null;
    }

    private static @Nullable String typeAsCamelCasedName(J.InstanceOf instanceOf, Statement then) {
        J clazz = instanceOf.getClazz();
        if (clazz instanceof J.Identifier) {
            return StringUtils.uncapitalize(((J.Identifier) clazz).getSimpleName());
        }
        if (clazz instanceof TypedTree) {
            JavaType type = ((TypedTree) clazz).getType();
            if (type != null) {
                return StringUtils.uncapitalize(((JavaType.Class) type).getClassName().replaceAll("\\.", ""));
            }
        }
        return null;
    }

    private static @Nullable String expressionNameAsClassType(J.InstanceOf instanceOf, Statement then) {
        String expressionName = instanceOf.getExpression() instanceof J.Identifier ? ((J.Identifier) instanceOf.getExpression()).getSimpleName() : null;
        String classType = typeAsCamelCasedName(instanceOf, then);

        StringBuilder result = new StringBuilder();
        if (expressionName != null) {
            result.append(expressionName);
        }
        if (expressionName != null && classType != null) {
            result.append("As");
        }
        if (classType != null) {
            result.append(StringUtils.capitalize(classType));
        }
        if (result.length() > 0) {
            return result.toString();
        }
        return null;
    }
}
