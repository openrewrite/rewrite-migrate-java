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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.nCopies;
import static org.openrewrite.java.migrate.fastjson.RemoveObsoleteFastjsonFeatures.SERIALIZER_FEATURE;

/**
 * fastjson2 writes JSON to an {@code OutputStream} with {@code JSON.writeTo}, always as UTF-8, and has no method that
 * writes to a {@code Writer}.
 */
public class MigrateFastjsonWriteJSONString extends Recipe {

    private static final String JSON = "com.alibaba.fastjson.JSON";

    private static final MethodMatcher WRITE = new MethodMatcher(
            JSON + " writeJSONString(java.io.OutputStream, Object, " + SERIALIZER_FEATURE + "[])");
    private static final MethodMatcher WRITE_WITH_CHARSET = new MethodMatcher(
            JSON + " writeJSONString(java.io.OutputStream, java.nio.charset.Charset, Object, " + SERIALIZER_FEATURE + "[])");
    private static final MethodMatcher WRITE_TO_WRITER = new MethodMatcher(
            JSON + " writeJSONString(java.io.Writer, Object, " + SERIALIZER_FEATURE + "[])");
    private static final List<String> UTF8_CONSTANT_OWNERS = asList(
            "java.nio.charset.StandardCharsets", "com.google.common.base.Charsets", "com.alibaba.fastjson.util.IOUtils");

    private static final MethodMatcher CHARSET_FOR_NAME = new MethodMatcher("java.nio.charset.Charset forName(String)");

    /**
     * Fastjson 1.x as seen by the templates, with the {@code writeTo} method fastjson2 has once the types are changed.
     */
    private static final String[] STUBS = {
            "package com.alibaba.fastjson.serializer;\n" +
            "public enum SerializerFeature {}",
            "package com.alibaba.fastjson;\n" +
            "import com.alibaba.fastjson.serializer.SerializerFeature;\n" +
            "public abstract class JSON {\n" +
            "    public static int writeTo(java.io.OutputStream out, Object object, SerializerFeature... features) { return 0; }\n" +
            "    public static String toJSONString(Object object, SerializerFeature... features) { return null; }\n" +
            "}"
    };

    @Getter
    final String displayName = "Migrate Fastjson `JSON.writeJSONString`";

    @Getter
    final String description = "Fastjson 1.x `JSON.writeJSONString` has no counterpart in fastjson2, which writes JSON to an " +
            "`OutputStream` with `JSON.writeTo`, always as UTF-8. `JSON.writeJSONString(out, object, features)` becomes " +
            "`JSON.writeTo(out, object, features)`, and so does " +
            "`JSON.writeJSONString(out, charset, object, features)` where the charset is UTF-8. A call with another charset " +
            "is left alone. fastjson2 has no method that writes to a `Writer`, so `JSON.writeJSONString(writer, object, features)` " +
            "becomes `writer.write(JSON.toJSONString(object, features))` where the `IOException` that `Writer.write` throws " +
            "is caught or declared.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(JSON + " writeJSONString(..)"), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (WRITE_TO_WRITER.matches(m) && isIOExceptionHandled()) {
                    return writeToWriter(m);
                }
                List<Expression> args;
                if (WRITE.matches(m)) {
                    args = m.getArguments();
                } else if (WRITE_WITH_CHARSET.matches(m) && isUtf8(m.getArguments().get(1))) {
                    args = new ArrayList<>(m.getArguments());
                    args.remove(1);
                    for (String type : UTF8_CONSTANT_OWNERS) {
                        maybeRemoveImport(type);
                        maybeRemoveImport(type + ".UTF_8");
                        maybeRemoveImport(type + ".UTF8");
                    }
                    maybeRemoveImport("java.nio.charset.Charset");
                } else {
                    return m;
                }
                List<Expression> parameters = withoutEmpty(args);
                J.MethodInvocation replaced = JavaTemplate.builder("JSON.writeTo(" + String.join(", ", nCopies(parameters.size(), "#{any()}")) + ")")
                        .imports(JSON)
                        .javaParser(JavaParser.fromJavaVersion().dependsOn(STUBS))
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), parameters.toArray());
                return withSelectOf(replaced, m);
            }

            private J.MethodInvocation writeToWriter(J.MethodInvocation m) {
                List<Expression> parameters = withoutEmpty(m.getArguments());
                J.MethodInvocation replaced = JavaTemplate.builder("#{any(java.io.Writer)}.write(JSON.toJSONString(" +
                                                                   String.join(", ", nCopies(parameters.size() - 1, "#{any()}")) + "))")
                        .imports(JSON)
                        .javaParser(JavaParser.fromJavaVersion().dependsOn(STUBS))
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), parameters.toArray());
                return replaced.withArguments(ListUtils.mapFirst(replaced.getArguments(),
                        toJSONString -> withSelectOf((J.MethodInvocation) toJSONString, m)));
            }

            /**
             * Calls {@code JSON} the way the original call did, or imports it for a call without a qualifier.
             */
            private J.MethodInvocation withSelectOf(J.MethodInvocation replaced, J.MethodInvocation original) {
                if (original.getSelect() == null) {
                    maybeAddImport(JSON);
                    return replaced;
                }
                return replaced.withSelect(original.getSelect());
            }

            /**
             * Whether an {@code IOException} thrown where the cursor is would be caught or declared.
             */
            private boolean isIOExceptionHandled() {
                Object inner = getCursor().getValue();
                for (Cursor c = getCursor().getParent(); c != null; c = c.getParent()) {
                    Object value = c.getValue();
                    if (value instanceof J.Try && ((J.Try) value).getBody() == inner) {
                        for (J.Try.Catch aCatch : ((J.Try) value).getCatches()) {
                            if (catchesIOException(aCatch.getParameter().getType())) {
                                return true;
                            }
                        }
                    } else if (value instanceof J.MethodDeclaration) {
                        JavaType.Method methodType = ((J.MethodDeclaration) value).getMethodType();
                        return methodType != null && methodType.getThrownExceptions().stream().anyMatch(this::catchesIOException);
                    } else if (value instanceof J.Lambda || value instanceof J.ClassDeclaration) {
                        return false;
                    }
                    if (value instanceof J) {
                        inner = value;
                    }
                }
                return false;
            }

            private boolean catchesIOException(@Nullable JavaType type) {
                if (type instanceof JavaType.MultiCatch) {
                    return ((JavaType.MultiCatch) type).getThrowableTypes().stream().anyMatch(this::catchesIOException);
                }
                return TypeUtils.isOfClassType(type, "java.io.IOException") ||
                       TypeUtils.isOfClassType(type, "java.lang.Exception") ||
                       TypeUtils.isOfClassType(type, "java.lang.Throwable");
            }

            private boolean isUtf8(Expression charset) {
                if (isUtf8Constant(charset)) {
                    return true;
                }
                // A constant of this class that holds UTF-8
                JavaType.Variable field = charset instanceof J.Identifier ? ((J.Identifier) charset).getFieldType() : null;
                J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (field == null || classDecl == null || !TypeUtils.isOfType(field.getOwner(), classDecl.getType())) {
                    return false;
                }
                for (Statement statement : classDecl.getBody().getStatements()) {
                    if (statement instanceof J.VariableDeclarations &&
                        ((J.VariableDeclarations) statement).hasModifier(J.Modifier.Type.Final)) {
                        for (J.VariableDeclarations.NamedVariable variable : ((J.VariableDeclarations) statement).getVariables()) {
                            if (field.equals(variable.getVariableType())) {
                                return variable.getInitializer() != null && isUtf8Constant(variable.getInitializer());
                            }
                        }
                    }
                }
                return false;
            }
        });
    }

    private static List<Expression> withoutEmpty(List<Expression> args) {
        List<Expression> parameters = new ArrayList<>();
        for (Expression arg : args) {
            if (!(arg instanceof J.Empty)) {
                parameters.add(arg);
            }
        }
        return parameters;
    }

    private static boolean isUtf8Constant(Expression charset) {
        if (CHARSET_FOR_NAME.matches(charset)) {
            Expression name = ((J.MethodInvocation) charset).getArguments().get(0);
            return name instanceof J.Literal && ((J.Literal) name).getValue() instanceof String &&
                   ((String) ((J.Literal) name).getValue()).replace("-", "").equalsIgnoreCase("UTF8");
        }
        JavaType.Variable field = null;
        if (charset instanceof J.FieldAccess) {
            field = ((J.FieldAccess) charset).getName().getFieldType();
        } else if (charset instanceof J.Identifier) {
            field = ((J.Identifier) charset).getFieldType();
        }
        if (field == null) {
            return false;
        }
        return "UTF_8".equals(field.getName()) && (TypeUtils.isOfClassType(field.getOwner(), "java.nio.charset.StandardCharsets") ||
                                                   TypeUtils.isOfClassType(field.getOwner(), "com.google.common.base.Charsets")) ||
               "UTF8".equals(field.getName()) && TypeUtils.isOfClassType(field.getOwner(), "com.alibaba.fastjson.util.IOUtils");
    }
}
