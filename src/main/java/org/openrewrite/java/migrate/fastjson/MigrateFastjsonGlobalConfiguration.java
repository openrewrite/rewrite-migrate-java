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
package org.openrewrite.java.migrate.fastjson;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.openrewrite.java.migrate.fastjson.RemoveObsoleteFastjsonFeatures.PARSER_FEATURE;
import static org.openrewrite.java.migrate.fastjson.RemoveObsoleteFastjsonFeatures.SERIALIZER_FEATURE;
import static org.openrewrite.java.migrate.fastjson.RemoveObsoleteFastjsonFeatures.isObsoleteConstant;

/**
 * Migrates the global defaults of Fastjson 1.x, which are static fields and the global {@code ParserConfig} and
 * {@code SerializeConfig}, to the configuration methods of fastjson2.
 */
public class MigrateFastjsonGlobalConfiguration extends Recipe {

    private static final String JSON = "com.alibaba.fastjson.JSON";
    private static final String PARSER_CONFIG = "com.alibaba.fastjson.parser.ParserConfig";
    private static final String SERIALIZE_CONFIG = "com.alibaba.fastjson.serializer.SerializeConfig";
    private static final String FAST_JSON_CONFIG = "com.alibaba.fastjson.support.config.FastJsonConfig";

    private static final MethodMatcher GET_MASK = new MethodMatcher("com.alibaba.fastjson..* getMask()");
    private static final MethodMatcher SERIALIZER_FEATURE_CONFIG = new MethodMatcher(
            SERIALIZER_FEATURE + " config(int, " + SERIALIZER_FEATURE + ", boolean)");
    private static final MethodMatcher PARSER_FEATURE_CONFIG = new MethodMatcher(
            PARSER_FEATURE + " config(int, " + PARSER_FEATURE + ", boolean)");
    private static final MethodMatcher SET_AUTO_TYPE_SUPPORT = new MethodMatcher(PARSER_CONFIG + " setAutoTypeSupport(boolean)");
    private static final MethodMatcher PARSER_CONFIG_GLOBAL_INSTANCE = new MethodMatcher(PARSER_CONFIG + " getGlobalInstance()");
    private static final MethodMatcher SERIALIZE_CONFIG_GLOBAL_INSTANCE = new MethodMatcher(SERIALIZE_CONFIG + " getGlobalInstance()");
    private static final MethodMatcher SET_SERIALIZE_CONFIG = new MethodMatcher(FAST_JSON_CONFIG + " setSerializeConfig(" + SERIALIZE_CONFIG + ")");
    private static final MethodMatcher SET_PARSER_CONFIG = new MethodMatcher(FAST_JSON_CONFIG + " setParserConfig(" + PARSER_CONFIG + ")");

    /**
     * Fastjson 1.x as seen by the templates, with the fastjson2 {@code JSON} configuration methods, which take the
     * fastjson2 features the fastjson 1.x ones are changed to afterward.
     */
    private static final String[] STUBS = {
            "package com.alibaba.fastjson.serializer;\n" +
            "public enum SerializerFeature {}",
            "package com.alibaba.fastjson.parser;\n" +
            "public enum Feature { SupportAutoType }",
            "package com.alibaba.fastjson;\n" +
            "import com.alibaba.fastjson.parser.Feature;\n" +
            "import com.alibaba.fastjson.serializer.SerializerFeature;\n" +
            "public abstract class JSON {\n" +
            "    public static void config(SerializerFeature... features) {}\n" +
            "    public static void config(SerializerFeature feature, boolean state) {}\n" +
            "    public static void config(Feature... features) {}\n" +
            "    public static void config(Feature feature, boolean state) {}\n" +
            "    public static void configWriterDateFormat(String format) {}\n" +
            "}"
    };

    @Getter
    final String displayName = "Migrate Fastjson global configuration to fastjson2";

    @Getter
    final String description = "Migrates the global defaults of Fastjson 1.x to the configuration methods of fastjson2. " +
            "`JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.WriteMapNullValue.getMask()` becomes " +
            "`JSON.config(SerializerFeature.WriteMapNullValue)`, and `&= ~` disables the feature, which works the same " +
            "for `JSON.DEFAULT_PARSER_FEATURE`, as does assigning `SerializerFeature.config(JSON.DEFAULT_GENERATE_FEATURE, feature, state)` " +
            "and `Feature.config(JSON.DEFAULT_PARSER_FEATURE, feature, state)`. Enabling a feature fastjson2 has no counterpart for is removed, since " +
            "fastjson2 behaves that way by default. `JSON.DEFFAULT_DATE_FORMAT = format` becomes " +
            "`JSON.configWriterDateFormat(format)`, and `ParserConfig.getGlobalInstance().setAutoTypeSupport(b)` becomes " +
            "`JSON.config(Feature.SupportAutoType, b)`. `FastJsonConfig.setSerializeConfig` and `setParserConfig` are " +
            "removed when they pass the global configuration, which fastjson2 uses anyway.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.or(
                new UsesType<>(JSON, false),
                new UsesType<>(PARSER_CONFIG, false),
                new UsesType<>(FAST_JSON_CONFIG, false)), new JavaVisitor<ExecutionContext>() {

            @Override
            public @Nullable J visitAssignmentOperation(J.AssignmentOperation assignOp, ExecutionContext ctx) {
                J j = super.visitAssignmentOperation(assignOp, ctx);
                if (!(j instanceof J.AssignmentOperation) || !isStatement()) {
                    return j;
                }
                J.AssignmentOperation a = (J.AssignmentOperation) j;
                boolean writer = isJsonField(a.getVariable(), "DEFAULT_GENERATE_FEATURE");
                if (!writer && !isJsonField(a.getVariable(), "DEFAULT_PARSER_FEATURE")) {
                    return a;
                }
                boolean enable = a.getOperator() == J.AssignmentOperation.Type.BitOr;
                Expression mask = a.getAssignment();
                if (!enable && a.getOperator() == J.AssignmentOperation.Type.BitAnd &&
                    mask instanceof J.Unary && ((J.Unary) mask).getOperator() == J.Unary.Type.Complement) {
                    mask = ((J.Unary) mask).getExpression();
                } else if (!enable) {
                    return a;
                }
                Expression feature = featureOfMask(mask, writer ? SERIALIZER_FEATURE : PARSER_FEATURE);
                if (feature == null) {
                    return a;
                }
                if (isObsoleteConstant(feature)) {
                    // fastjson2 does this by default. What turning it off means differs, so that is left alone.
                    return enable ? null : a;
                }
                maybeRemoveImport(JSON + ".DEFAULT_GENERATE_FEATURE");
                maybeRemoveImport(JSON + ".DEFAULT_PARSER_FEATURE");
                return configure(enable ? "JSON.config(#{any()});" : "JSON.config(#{any()}, false);",
                        a.getCoordinates().replace(), feature);
            }

            @Override
            public @Nullable J visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                J j = super.visitAssignment(assignment, ctx);
                if (!(j instanceof J.Assignment) || !isStatement()) {
                    return j;
                }
                J.Assignment a = (J.Assignment) j;
                if (isJsonField(a.getVariable(), "DEFFAULT_DATE_FORMAT")) {
                    maybeRemoveImport(JSON + ".DEFFAULT_DATE_FORMAT");
                    return configure("JSON.configWriterDateFormat(#{any(String)});", a.getCoordinates().replace(),
                            a.getAssignment());
                }
                return configFeature(a);
            }

            /**
             * {@code JSON.DEFAULT_GENERATE_FEATURE = SerializerFeature.config(JSON.DEFAULT_GENERATE_FEATURE, feature, state)},
             * and the same for the parser features.
             */
            private @Nullable J configFeature(J.Assignment a) {
                String field;
                String featureType;
                if (isJsonField(a.getVariable(), "DEFAULT_GENERATE_FEATURE") && SERIALIZER_FEATURE_CONFIG.matches(a.getAssignment())) {
                    field = "DEFAULT_GENERATE_FEATURE";
                    featureType = SERIALIZER_FEATURE;
                } else if (isJsonField(a.getVariable(), "DEFAULT_PARSER_FEATURE") && PARSER_FEATURE_CONFIG.matches(a.getAssignment())) {
                    field = "DEFAULT_PARSER_FEATURE";
                    featureType = PARSER_FEATURE;
                } else {
                    return a;
                }
                List<Expression> args = ((J.MethodInvocation) a.getAssignment()).getArguments();
                Expression feature = args.get(1);
                Expression state = args.get(2);
                if (!isJsonField(args.get(0), field) || !isStaticField(feature, featureType, null)) {
                    return a;
                }
                boolean enable = J.Literal.isLiteralValue(state, Boolean.TRUE);
                if (isObsoleteConstant(feature)) {
                    // fastjson2 does this by default. What turning it off means differs, so that is left alone.
                    return enable ? null : a;
                }
                maybeRemoveImport(JSON + "." + field);
                return enable ?
                        configure("JSON.config(#{any()});", a.getCoordinates().replace(), feature) :
                        configure("JSON.config(#{any()}, #{any(boolean)});", a.getCoordinates().replace(), feature, state);
            }

            @Override
            public @Nullable J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J j = super.visitMethodInvocation(method, ctx);
                if (!(j instanceof J.MethodInvocation) || !isStatement()) {
                    return j;
                }
                J.MethodInvocation m = (J.MethodInvocation) j;
                if (SET_AUTO_TYPE_SUPPORT.matches(m) && isGlobal(m.getSelect(), PARSER_CONFIG_GLOBAL_INSTANCE, PARSER_CONFIG, "global")) {
                    maybeAddImport(PARSER_FEATURE);
                    maybeRemoveImport(PARSER_CONFIG);
                    return configure("JSON.config(Feature.SupportAutoType, #{any(boolean)});",
                            m.getCoordinates().replace(), m.getArguments().get(0));
                }
                if (SET_SERIALIZE_CONFIG.matches(m) &&
                    isGlobal(m.getArguments().get(0), SERIALIZE_CONFIG_GLOBAL_INSTANCE, SERIALIZE_CONFIG, "globalInstance") ||
                    SET_PARSER_CONFIG.matches(m) &&
                    isGlobal(m.getArguments().get(0), PARSER_CONFIG_GLOBAL_INSTANCE, PARSER_CONFIG, "global")) {
                    maybeRemoveImport(SERIALIZE_CONFIG);
                    maybeRemoveImport(PARSER_CONFIG);
                    return null;
                }
                return m;
            }

            private J configure(String template, JavaCoordinates coordinates, Expression... arguments) {
                maybeAddImport(JSON);
                return JavaTemplate.builder(template)
                        .imports(JSON, SERIALIZER_FEATURE, PARSER_FEATURE)
                        .javaParser(JavaParser.fromJavaVersion().dependsOn(STUBS))
                        .build()
                        .apply(getCursor(), coordinates, (Object[]) arguments);
            }

            private boolean isStatement() {
                return getCursor().getParentTreeCursor().getValue() instanceof J.Block;
            }

            /**
             * Whether the expression is the global configuration, or a local variable that only ever holds it.
             */
            private boolean isGlobal(@Nullable Expression expression, MethodMatcher globalInstance, String type, String field) {
                if (expression == null) {
                    return false;
                }
                if (globalInstance.matches(expression) || isStaticField(expression, type, field)) {
                    return true;
                }
                if (!(expression instanceof J.Identifier) || ((J.Identifier) expression).getFieldType() == null ||
                    ((J.Identifier) expression).getFieldType().getOwner() instanceof JavaType.FullyQualified) {
                    return false;
                }
                J.Identifier variable = (J.Identifier) expression;
                Cursor body = getCursor().dropParentUntil(t -> t instanceof J.MethodDeclaration ||
                                                               t instanceof J.Lambda ||
                                                               t instanceof J.ClassDeclaration ||
                                                               t == Cursor.ROOT_VALUE);
                if (!(body.getValue() instanceof J.MethodDeclaration)) {
                    return false;
                }
                AtomicBoolean onlyGlobal = new AtomicBoolean(false);
                AtomicBoolean reassigned = new AtomicBoolean(false);
                new JavaIsoVisitor<AtomicBoolean>() {
                    @Override
                    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable v, AtomicBoolean found) {
                        if (v.getName().getSimpleName().equals(variable.getSimpleName()) &&
                            v.getVariableType() != null && v.getVariableType().equals(variable.getFieldType())) {
                            found.set(v.getInitializer() != null &&
                                      (globalInstance.matches(v.getInitializer()) ||
                                       isStaticField(v.getInitializer(), type, field)));
                        }
                        return super.visitVariable(v, found);
                    }

                    @Override
                    public J.Assignment visitAssignment(J.Assignment a, AtomicBoolean found) {
                        if (a.getVariable() instanceof J.Identifier &&
                            ((J.Identifier) a.getVariable()).getSimpleName().equals(variable.getSimpleName())) {
                            reassigned.set(true);
                        }
                        return super.visitAssignment(a, found);
                    }
                }.visit((J) body.getValue(), onlyGlobal);
                return onlyGlobal.get() && !reassigned.get();
            }
        });
    }

    /**
     * The feature constant of {@code Feature.X.getMask()}, or {@code null} if the mask is anything else.
     */
    private static @Nullable Expression featureOfMask(Expression mask, String featureType) {
        if (!(mask instanceof J.MethodInvocation) || !GET_MASK.matches((J.MethodInvocation) mask)) {
            return null;
        }
        Expression feature = ((J.MethodInvocation) mask).getSelect();
        return isStaticField(feature, featureType, null) ? feature : null;
    }

    private static boolean isJsonField(Expression variable, String name) {
        return isStaticField(variable, JSON, name);
    }

    private static boolean isStaticField(@Nullable Expression expression, String owner, @Nullable String name) {
        JavaType.Variable fieldType = null;
        if (expression instanceof J.FieldAccess) {
            fieldType = ((J.FieldAccess) expression).getName().getFieldType();
        } else if (expression instanceof J.Identifier) {
            fieldType = ((J.Identifier) expression).getFieldType();
        }
        return fieldType != null && (name == null || name.equals(fieldType.getName())) &&
               TypeUtils.isOfClassType(fieldType.getOwner(), owner);
    }
}
