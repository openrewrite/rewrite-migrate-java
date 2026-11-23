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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.nCopies;
import static org.openrewrite.java.migrate.fastjson.RemoveObsoleteFastjsonFeatures.SERIALIZER_FEATURE;

/**
 * Rewrites the arguments of the Fastjson 1.x {@code JSON.toJSONString} overloads that fastjson2 has no counterpart for,
 * so that the type changes that follow leave a call fastjson2 has.
 */
public class MigrateFastjsonToJSONStringArguments extends Recipe {

    private static final String JSON = "com.alibaba.fastjson.JSON";
    private static final String SERIALIZE_CONFIG = "com.alibaba.fastjson.serializer.SerializeConfig";

    private static final MethodMatcher TO_JSON_STRING_PRETTY = new MethodMatcher(JSON + " toJSONString(Object, boolean)");
    private static final MethodMatcher TO_JSON_STRING_CONFIG = new MethodMatcher(
            JSON + " toJSONString(Object, " + SERIALIZE_CONFIG + ", " + SERIALIZER_FEATURE + "[])");
    private static final MethodMatcher TO_JSON_STRING_FEATURES = new MethodMatcher(
            JSON + " toJSONString(Object, " + SERIALIZER_FEATURE + "[])");
    private static final MethodMatcher TO_JSON_STRING_DEFAULT_FEATURES = new MethodMatcher(
            JSON + " toJSONString(Object, int, " + SERIALIZER_FEATURE + "[])");

    /**
     * The features {@code SerializerFeature.WRITE_MAP_NULL_FEATURES} is the mask of.
     */
    private static final List<String> WRITE_MAP_NULL_FEATURES = Arrays.asList(
            "WriteMapNullValue", "WriteNullBooleanAsFalse", "WriteNullListAsEmpty", "WriteNullNumberAsZero",
            "WriteNullStringAsEmpty");

    /**
     * Fastjson 1.x as seen by the templates, with the methods the rewritten calls use. fastjson2 has each of them once
     * the types are changed, where {@code toJSONStringWithDateFormat} is renamed to {@code toJSONString}.
     */
    private static final String[] STUBS = {
            "package com.alibaba.fastjson.serializer;\n" +
            "public enum SerializerFeature { PrettyFormat, WriteMapNullValue, WriteNullBooleanAsFalse, " +
            "WriteNullListAsEmpty, WriteNullNumberAsZero, WriteNullStringAsEmpty }",
            "package com.alibaba.fastjson.serializer;\n" +
            "public class SerializeConfig {}",
            "package com.alibaba.fastjson;\n" +
            "import com.alibaba.fastjson.serializer.SerializerFeature;\n" +
            "public abstract class JSON {\n" +
            "    public static String toJSONString(Object object, SerializerFeature... features) { return null; }\n" +
            "    public static String toJSONString(Object object, com.alibaba.fastjson2.JSONWriter.Context context) { return null; }\n" +
            "    public static String toJSONStringWithDateFormat(Object object, String dateFormat, SerializerFeature... features) { return null; }\n" +
            "}",
            "package com.alibaba.fastjson2;\n" +
            "import com.alibaba.fastjson.serializer.SerializeConfig;\n" +
            "import com.alibaba.fastjson.serializer.SerializerFeature;\n" +
            "public interface JSONWriter {\n" +
            "    class Context { public Context(SerializeConfig provider, SerializerFeature... features) {} }\n" +
            "}"
    };

    @Getter
    final String displayName = "Migrate Fastjson `JSON.toJSONString` arguments that fastjson2 does not take";

    @Getter
    final String description = "Rewrites the Fastjson 1.x `JSON.toJSONString` calls whose arguments fastjson2 does not take. " +
            "`toJSONString(o, true)` passes `SerializerFeature.PrettyFormat` instead of the flag. " +
            "`toJSONString(o, serializeConfig, features)` passes a fastjson2 `JSONWriter.Context` that holds the " +
            "configuration and the features. `SerializerFeature.UseISO8601DateFormat`, which fastjson2 does not have, " +
            "becomes the `\"iso8601\"` date format of `toJSONStringWithDateFormat`. Of the `toJSONString(o, defaultFeatures, features)` " +
            "calls, those passing `JSON.DEFAULT_GENERATE_FEATURE` pass only the features, and those passing " +
            "`SerializerFeature.WRITE_MAP_NULL_FEATURES` pass the features that mask stands for.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(JSON + " toJSONString(..)"), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (TO_JSON_STRING_PRETTY.matches(m)) {
                    return prettyFormat(m, ctx);
                }
                if (TO_JSON_STRING_CONFIG.matches(m)) {
                    List<Expression> args = m.getArguments();
                    String context = "new JSONWriter.Context(" +
                                     String.join(", ", nCopies(args.size() - 1, "#{any()}")) + ")";
                    return replace(m, "JSON.toJSONString(#{any()}, " + context + ")", args.toArray(), ctx);
                }
                if (TO_JSON_STRING_FEATURES.matches(m)) {
                    return iso8601DateFormat(m, ctx);
                }
                if (TO_JSON_STRING_DEFAULT_FEATURES.matches(m)) {
                    return defaultFeatures(m, ctx);
                }
                return m;
            }

            /**
             * fastjson2 has no mask of default features, so only the masks that stand for features are rewritten.
             */
            private J.MethodInvocation defaultFeatures(J.MethodInvocation m, ExecutionContext ctx) {
                List<Expression> args = m.getArguments();
                Expression mask = args.get(1);
                String maskField;
                List<String> maskFeatures;
                if (isField(mask, JSON, "DEFAULT_GENERATE_FEATURE")) {
                    maskField = JSON + ".DEFAULT_GENERATE_FEATURE";
                    maskFeatures = emptyList();
                } else if (isField(mask, SERIALIZER_FEATURE, "WRITE_MAP_NULL_FEATURES")) {
                    maskField = SERIALIZER_FEATURE + ".WRITE_MAP_NULL_FEATURES";
                    maskFeatures = WRITE_MAP_NULL_FEATURES;
                } else {
                    return m;
                }
                StringBuilder template = new StringBuilder("JSON.toJSONString(#{any()}");
                for (String feature : maskFeatures) {
                    template.append(", SerializerFeature.").append(feature);
                }
                List<Object> parameters = new ArrayList<>();
                parameters.add(args.get(0));
                for (Expression feature : args.subList(2, args.size())) {
                    if (!(feature instanceof J.Empty)) {
                        template.append(", #{any()}");
                        parameters.add(feature);
                    }
                }
                template.append(")");
                // A statically imported mask is no longer used
                maybeRemoveImport(maskField);
                return replace(m, template.toString(), parameters.toArray(), ctx);
            }

            private J.MethodInvocation prettyFormat(J.MethodInvocation m, ExecutionContext ctx) {
                Expression object = m.getArguments().get(0);
                Expression pretty = m.getArguments().get(1);
                if (J.Literal.isLiteralValue(pretty, Boolean.TRUE)) {
                    return replace(m, "JSON.toJSONString(#{any()}, SerializerFeature.PrettyFormat)", new Object[]{object}, ctx);
                }
                if (J.Literal.isLiteralValue(pretty, Boolean.FALSE)) {
                    return replace(m, "JSON.toJSONString(#{any()})", new Object[]{object}, ctx);
                }
                return replace(m, "JSON.toJSONString(#{any()}, #{any(boolean)} ? " +
                                  "new SerializerFeature[]{SerializerFeature.PrettyFormat} : new SerializerFeature[0])",
                        new Object[]{object, pretty}, ctx);
            }

            private J.MethodInvocation iso8601DateFormat(J.MethodInvocation m, ExecutionContext ctx) {
                List<Expression> args = m.getArguments();
                List<Object> parameters = new ArrayList<>();
                parameters.add(args.get(0));
                boolean iso8601 = false;
                for (Expression feature : args.subList(1, args.size())) {
                    if (isUseISO8601DateFormat(feature)) {
                        iso8601 = true;
                    } else if (!(feature instanceof J.Empty)) {
                        parameters.add(feature);
                    }
                }
                if (!iso8601) {
                    return m;
                }
                String template = "JSON.toJSONStringWithDateFormat(#{any()}, \"iso8601\"" +
                                  String.join("", nCopies(parameters.size() - 1, ", #{any()}")) + ")";
                return replace(m, template, parameters.toArray(), ctx);
            }

            private J.MethodInvocation replace(J.MethodInvocation m, String template, Object[] parameters, ExecutionContext ctx) {
                J.MethodInvocation replaced = JavaTemplate.builder(template)
                        .imports(JSON, SERIALIZER_FEATURE, "com.alibaba.fastjson2.JSONWriter")
                        .javaParser(JavaParser.fromJavaVersion().dependsOn(STUBS))
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), parameters);
                if (template.contains("SerializerFeature")) {
                    maybeAddImport(SERIALIZER_FEATURE);
                }
                if (template.contains("JSONWriter")) {
                    maybeAddImport("com.alibaba.fastjson2.JSONWriter");
                }
                if (m.getSelect() == null) {
                    maybeAddImport(JSON);
                    return replaced;
                }
                // Keep the qualifier the call was written with
                return replaced.withSelect(m.getSelect());
            }
        });
    }

    private static boolean isUseISO8601DateFormat(Expression expression) {
        return isField(expression, SERIALIZER_FEATURE, "UseISO8601DateFormat");
    }

    private static boolean isField(Expression expression, String owner, String name) {
        JavaType.Variable fieldType = null;
        if (expression instanceof J.FieldAccess) {
            fieldType = ((J.FieldAccess) expression).getName().getFieldType();
        } else if (expression instanceof J.Identifier) {
            fieldType = ((J.Identifier) expression).getFieldType();
        }
        return fieldType != null && name.equals(fieldType.getName()) && TypeUtils.isOfClassType(fieldType.getOwner(), owner);
    }
}
