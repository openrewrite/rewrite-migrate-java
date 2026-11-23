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
import org.openrewrite.java.ShortenFullyQualifiedTypeReferences;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

/**
 * fastjson2's {@code JSONObject} and {@code JSONArray} have no {@code getSqlDate} and {@code getTimestamp}, and
 * convert the value with {@code getObject} instead.
 */
public class ReplaceFastjsonSqlDateGetters extends Recipe {

    private static final String JSON_OBJECT = "com.alibaba.fastjson.JSONObject";
    private static final String JSON_ARRAY = "com.alibaba.fastjson.JSONArray";

    private static final MethodMatcher GETTERS = new MethodMatcher("com.alibaba.fastjson.JSON* get*(..)");
    private static final MethodMatcher OBJECT_SQL_DATE = new MethodMatcher(JSON_OBJECT + " getSqlDate(String)", true);
    private static final MethodMatcher OBJECT_TIMESTAMP = new MethodMatcher(JSON_OBJECT + " getTimestamp(String)", true);
    private static final MethodMatcher ARRAY_SQL_DATE = new MethodMatcher(JSON_ARRAY + " getSqlDate(int)", true);
    private static final MethodMatcher ARRAY_TIMESTAMP = new MethodMatcher(JSON_ARRAY + " getTimestamp(int)", true);

    /**
     * Fastjson 1.x as seen by the templates, with the {@code getObject} methods fastjson2 has too.
     */
    private static final String[] STUBS = {
            "package com.alibaba.fastjson;\n" +
            "public class JSONObject {\n" +
            "    public <T> T getObject(String key, Class<T> clazz) { return null; }\n" +
            "}",
            "package com.alibaba.fastjson;\n" +
            "public class JSONArray {\n" +
            "    public <T> T getObject(int index, Class<T> clazz) { return null; }\n" +
            "}"
    };

    @Getter
    final String displayName = "Replace Fastjson `getSqlDate` and `getTimestamp` with `getObject`";

    @Getter
    final String description = "Fastjson 1.x `JSONObject` and `JSONArray` have `getSqlDate` and `getTimestamp`, which " +
            "fastjson2 does not. They become `getObject` with `java.sql.Date` or `java.sql.Timestamp` as the type, which " +
            "converts the value the same way.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(GETTERS), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                // A call in a subclass, without a qualifier, is left alone
                if (m.getSelect() == null) {
                    return m;
                }
                if (OBJECT_SQL_DATE.matches(m)) {
                    return getObject(m, JSON_OBJECT, "String", "java.sql.Date", ctx);
                }
                if (OBJECT_TIMESTAMP.matches(m)) {
                    return getObject(m, JSON_OBJECT, "String", "java.sql.Timestamp", ctx);
                }
                if (ARRAY_SQL_DATE.matches(m)) {
                    return getObject(m, JSON_ARRAY, "int", "java.sql.Date", ctx);
                }
                if (ARRAY_TIMESTAMP.matches(m)) {
                    return getObject(m, JSON_ARRAY, "int", "java.sql.Timestamp", ctx);
                }
                return m;
            }

            private J.MethodInvocation getObject(J.MethodInvocation m, String owner, String keyType, String valueType,
                                                 ExecutionContext ctx) {
                J.MethodInvocation replaced = JavaTemplate.builder(
                                "#{any(" + owner + ")}.getObject(#{any(" + keyType + ")}, " + valueType + ".class)")
                        .javaParser(JavaParser.fromJavaVersion().dependsOn(STUBS))
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), m.getSelect(), m.getArguments().get(0));
                // `java.sql.Date` is written in full where another `Date`, such as `java.util.Date`, is in scope
                doAfterVisit(ShortenFullyQualifiedTypeReferences.modifyOnly(replaced));
                return replaced;
            }
        });
    }
}
