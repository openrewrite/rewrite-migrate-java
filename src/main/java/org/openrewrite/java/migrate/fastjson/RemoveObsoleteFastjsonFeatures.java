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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.*;

import static java.util.Arrays.asList;

/**
 * Removes the Fastjson 1.x {@code SerializerFeature} and parser {@code Feature} constants that fastjson2 has no
 * counterpart for, because fastjson2 already behaves that way or the option no longer applies.
 */
public class RemoveObsoleteFastjsonFeatures extends Recipe {

    static final String SERIALIZER_FEATURE = "com.alibaba.fastjson.serializer.SerializerFeature";
    static final String PARSER_FEATURE = "com.alibaba.fastjson.parser.Feature";

    /**
     * {@code SerializerFeature} constants whose behavior is fastjson2's default, or that no longer do anything.
     */
    static final Set<String> OBSOLETE_SERIALIZER_FEATURES = new HashSet<>(asList(
            // fastjson2 does not detect references unless `JSONWriter.Feature.ReferenceDetection` is enabled
            "DisableCircularReferenceDetect",
            "DisableCheckSpecialChar",
            // fastjson2 quotes field names unless `JSONWriter.Feature.UnquoteFieldName` is enabled
            "QuoteFieldNames",
            "SkipTransientField",
            "SortField",
            // fastjson2 writes dates with the default date format, which fastjson 1.x only did with this feature
            "WriteDateUseDateFormat",
            "WriteSlashAsSpecial",
            "WriteTabAsSpecial"
    ));

    /**
     * Parser {@code Feature} constants whose behavior is fastjson2's default, or that no longer do anything.
     */
    static final Set<String> OBSOLETE_PARSER_FEATURES = new HashSet<>(asList(
            "AllowArbitraryCommas",
            "AllowComment",
            "AllowISO8601DateFormat",
            "AllowSingleQuotes",
            "AutoCloseSource",
            "CustomMapDeserializer",
            "DisableASM",
            // fastjson2 only matches field names smartly with `JSONReader.Feature.SupportSmartMatch`
            "DisableFieldSmartMatch",
            "DisableSpecialKeyDetect",
            "IgnoreAutoType",
            "IgnoreNotMatch",
            "InternFieldNames",
            // fastjson2's `JSONObject` is a `LinkedHashMap`, so it keeps the field order
            "OrderedField",
            "SafeMode",
            "SortFeidFastMatch",
            // fastjson2 reads decimals as `BigDecimal`
            "UseBigDecimal",
            "UseObjectArray"
    ));

    @Getter
    final String displayName = "Remove Fastjson features that fastjson2 does not need";

    @Getter
    final String description = "Removes the Fastjson 1.x `SerializerFeature` and parser `Feature` constants that fastjson2 " +
            "has no counterpart for, because fastjson2 already behaves that way or the option no longer applies, " +
            "such as `SerializerFeature.DisableCircularReferenceDetect`, `SerializerFeature.WriteDateUseDateFormat` " +
            "and `Feature.OrderedField`. They are removed from varargs arguments, array initializers and annotation " +
            "attributes, such as `JSON.toJSONString(o, SerializerFeature.DisableCircularReferenceDetect)`, which becomes " +
            "`JSON.toJSONString(o)`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(new UsesType<>(SERIALIZER_FEATURE, false), new UsesType<>(PARSER_FEATURE, false)),
                new JavaIsoVisitor<ExecutionContext>() {

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                        List<Expression> args = removeFromVarargs(m.getArguments(), m.getMethodType());
                        return args == m.getArguments() ? m : m.withArguments(args);
                    }

                    @Override
                    public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                        J.NewClass n = super.visitNewClass(newClass, ctx);
                        List<Expression> args = removeFromVarargs(n.getArguments(), n.getConstructorType());
                        return args == n.getArguments() ? n : n.withArguments(args);
                    }

                    @Override
                    public J.NewArray visitNewArray(J.NewArray newArray, ExecutionContext ctx) {
                        J.NewArray n = super.visitNewArray(newArray, ctx);
                        // An array initializer of an annotation attribute may have no type of its own
                        if (n.getInitializer() == null || n.getType() != null && !isFeatureArray(n.getType())) {
                            return n;
                        }
                        List<Expression> initializer = removeObsolete(n.getInitializer(), 0);
                        if (initializer == n.getInitializer()) {
                            return n;
                        }
                        if (initializer.isEmpty()) {
                            initializer = Collections.singletonList(new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY));
                        }
                        return n.withInitializer(initializer);
                    }

                    @Override
                    public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                        J.Annotation a = super.visitAnnotation(annotation, ctx);
                        if (a.getArguments() == null || annotation.getArguments() == null ||
                            a.getArguments().size() != annotation.getArguments().size()) {
                            return a;
                        }
                        List<Expression> args = new ArrayList<>(a.getArguments().size());
                        for (int i = 0; i < a.getArguments().size(); i++) {
                            Expression arg = a.getArguments().get(i);
                            if (arg instanceof J.Assignment && annotation.getArguments().get(i) instanceof J.Assignment) {
                                Expression value = ((J.Assignment) arg).getAssignment();
                                Expression originalValue = ((J.Assignment) annotation.getArguments().get(i)).getAssignment();
                                if (isObsoleteConstant(value) ||
                                    // An array attribute that only held obsolete features is now empty
                                    value instanceof J.NewArray && originalValue instanceof J.NewArray &&
                                    isEmptyInitializer(((J.NewArray) value).getInitializer()) &&
                                    !isEmptyInitializer(((J.NewArray) originalValue).getInitializer())) {
                                    continue;
                                }
                            }
                            args.add(arg);
                        }
                        if (args.size() == a.getArguments().size()) {
                            return a;
                        }
                        if (args.isEmpty()) {
                            return a.withArguments(null);
                        }
                        if (!a.getArguments().get(0).equals(args.get(0))) {
                            args.set(0, args.get(0).withPrefix(a.getArguments().get(0).getPrefix()));
                        }
                        return a.withArguments(args);
                    }

                    @Override
                    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                        J.CompilationUnit c = super.visitCompilationUnit(cu, ctx);
                        if (c != cu) {
                            maybeRemoveImport(SERIALIZER_FEATURE);
                            maybeRemoveImport(PARSER_FEATURE);
                            for (String feature : OBSOLETE_SERIALIZER_FEATURES) {
                                maybeRemoveImport(SERIALIZER_FEATURE + "." + feature);
                            }
                            for (String feature : OBSOLETE_PARSER_FEATURES) {
                                maybeRemoveImport(PARSER_FEATURE + "." + feature);
                            }
                        }
                        return c;
                    }

                    private List<Expression> removeFromVarargs(List<Expression> args, JavaType.@Nullable Method methodType) {
                        if (methodType == null || !methodType.hasFlags(Flag.Varargs) || methodType.getParameterTypes().isEmpty()) {
                            return args;
                        }
                        List<JavaType> parameterTypes = methodType.getParameterTypes();
                        if (!isFeatureArray(parameterTypes.get(parameterTypes.size() - 1))) {
                            return args;
                        }
                        int varargsIndex = parameterTypes.size() - 1;
                        if (args.size() <= varargsIndex) {
                            return args;
                        }
                        List<Expression> kept = removeObsolete(args, varargsIndex);
                        if (kept != args && kept.isEmpty()) {
                            return Collections.singletonList(new J.Empty(Tree.randomId(), Space.EMPTY, Markers.EMPTY));
                        }
                        return kept;
                    }
                });
    }

    /**
     * Remove the obsolete feature constants at or after {@code from}, keeping the formatting of what is left.
     */
    private static List<Expression> removeObsolete(List<Expression> elements, int from) {
        List<Expression> kept = new ArrayList<>(elements.size());
        Space removedPrefix = null;
        for (int i = 0; i < elements.size(); i++) {
            Expression element = elements.get(i);
            if (i >= from && isObsoleteConstant(element)) {
                if (removedPrefix == null) {
                    removedPrefix = element.getPrefix();
                }
                continue;
            }
            if (removedPrefix != null && kept.size() == from) {
                // The first element that is left after the removed ones takes their place
                element = element.withPrefix(removedPrefix);
            }
            removedPrefix = kept.size() == from ? removedPrefix : null;
            kept.add(element);
        }
        return kept.size() == elements.size() ? elements : kept;
    }

    static boolean isObsoleteConstant(@Nullable Expression expression) {
        JavaType.Variable fieldType = null;
        if (expression instanceof J.FieldAccess) {
            fieldType = ((J.FieldAccess) expression).getName().getFieldType();
        } else if (expression instanceof J.Identifier) {
            fieldType = ((J.Identifier) expression).getFieldType();
        }
        if (fieldType == null) {
            return false;
        }
        if (TypeUtils.isOfClassType(fieldType.getOwner(), SERIALIZER_FEATURE)) {
            return OBSOLETE_SERIALIZER_FEATURES.contains(fieldType.getName());
        }
        if (TypeUtils.isOfClassType(fieldType.getOwner(), PARSER_FEATURE)) {
            return OBSOLETE_PARSER_FEATURES.contains(fieldType.getName());
        }
        return false;
    }

    private static boolean isFeatureArray(@Nullable JavaType type) {
        if (!(type instanceof JavaType.Array)) {
            return false;
        }
        JavaType elemType = ((JavaType.Array) type).getElemType();
        return TypeUtils.isOfClassType(elemType, SERIALIZER_FEATURE) || TypeUtils.isOfClassType(elemType, PARSER_FEATURE);
    }

    private static boolean isEmptyInitializer(@Nullable List<Expression> initializer) {
        return initializer != null && (initializer.isEmpty() ||
                                       initializer.size() == 1 && initializer.get(0) instanceof J.Empty);
    }
}
