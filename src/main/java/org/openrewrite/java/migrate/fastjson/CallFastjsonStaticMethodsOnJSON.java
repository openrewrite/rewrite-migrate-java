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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * Fastjson 1.x {@code JSONObject} and {@code JSONArray} extend {@code JSON}, so code calls {@code JSON}'s static
 * methods through them. Their fastjson2 counterparts do not extend {@code JSON}, and only declare some of those
 * methods themselves.
 */
public class CallFastjsonStaticMethodsOnJSON extends Recipe {

    private static final String JSON = "com.alibaba.fastjson.JSON";
    private static final String JSON_OBJECT = "com.alibaba.fastjson.JSONObject";
    private static final String JSON_ARRAY = "com.alibaba.fastjson.JSONArray";

    private static final MethodMatcher ANY_JSON_METHOD = new MethodMatcher(JSON + " *(..)");

    /**
     * The static methods of fastjson 1.x {@code JSON} that fastjson2 {@code JSONObject} also declares.
     */
    private static final List<MethodMatcher> DECLARED_BY_JSON_OBJECT = asList(
            new MethodMatcher(JSON + " parseObject(String)"),
            new MethodMatcher(JSON + " parseObject(String, Class)"),
            new MethodMatcher(JSON + " parseObject(String, Class, com.alibaba.fastjson.parser.Feature[])"),
            new MethodMatcher(JSON + " parseObject(String, java.lang.reflect.Type, com.alibaba.fastjson.parser.Feature[])"),
            new MethodMatcher(JSON + " parseObject(String, com.alibaba.fastjson.TypeReference, com.alibaba.fastjson.parser.Feature[])"),
            new MethodMatcher(JSON + " toJSONString(Object)"),
            new MethodMatcher(JSON + " toJSONString(Object, com.alibaba.fastjson.serializer.SerializerFeature[])")
    );

    /**
     * The static methods of fastjson 1.x {@code JSON} that fastjson2 {@code JSONArray} also declares.
     */
    private static final List<MethodMatcher> DECLARED_BY_JSON_ARRAY = asList(
            new MethodMatcher(JSON + " parseArray(String)"),
            new MethodMatcher(JSON + " parseArray(String, Class)"),
            new MethodMatcher(JSON + " toJSONString(Object)"),
            new MethodMatcher(JSON + " toJSONString(Object, com.alibaba.fastjson.serializer.SerializerFeature[])")
    );

    @Getter
    final String displayName = "Call Fastjson's static `JSON` methods on `JSON`";

    @Getter
    final String description = "Fastjson 1.x `JSONObject` and `JSONArray` extend `JSON`, so its static methods can be called " +
            "through them, as in `JSONObject.toJSON(o)` or `JSONObject.parseArray(text, Foo.class)`. In fastjson2 they no " +
            "longer extend `JSON` and only declare a few of those methods themselves, such as `JSONObject.parseObject(String)` " +
            "and `JSONArray.parseArray(String)`, which are left alone. The other calls are made on `JSON`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(ANY_JSON_METHOD), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                JavaType.Method methodType = m.getMethodType();
                Expression select = m.getSelect();
                if (methodType == null || select == null || !methodType.hasFlags(Flag.Static) ||
                    !TypeUtils.isOfClassType(methodType.getDeclaringType(), JSON) || !isTypeOrVariable(select)) {
                    return m;
                }
                JavaType.FullyQualified qualifier = TypeUtils.asFullyQualified(select.getType());
                if (qualifier == null || TypeUtils.isOfClassType(qualifier, JSON)) {
                    return m;
                }
                if (TypeUtils.isAssignableTo(JSON_OBJECT, qualifier) && matchesAny(DECLARED_BY_JSON_OBJECT, methodType) ||
                    TypeUtils.isAssignableTo(JSON_ARRAY, qualifier) && matchesAny(DECLARED_BY_JSON_ARRAY, methodType)) {
                    return m;
                }

                JavaType.FullyQualified jsonType = methodType.getDeclaringType();
                maybeRemoveImport(qualifier.getFullyQualifiedName());
                if (isJsonNameTaken()) {
                    return m.withSelect(TypeTree.<J.FieldAccess>build(JSON).withType(jsonType).withPrefix(select.getPrefix()));
                }
                maybeAddImport(JSON);
                return m.withSelect(new J.Identifier(Tree.randomId(), select.getPrefix(), Markers.EMPTY, emptyList(),
                        "JSON", jsonType, null));
            }

            /**
             * Whether the simple name {@code JSON} already names another type in this compilation unit.
             */
            private boolean isJsonNameTaken() {
                J.CompilationUnit cu = getCursor().firstEnclosing(J.CompilationUnit.class);
                if (cu == null) {
                    return false;
                }
                for (J.Import anImport : cu.getImports()) {
                    if (!anImport.isStatic() && "JSON".equals(anImport.getQualid().getSimpleName()) &&
                        !JSON.equals(anImport.getTypeName())) {
                        return true;
                    }
                }
                for (J.ClassDeclaration classDeclaration : cu.getClasses()) {
                    if ("JSON".equals(classDeclaration.getSimpleName())) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private static boolean matchesAny(List<MethodMatcher> matchers, JavaType.Method methodType) {
        for (MethodMatcher matcher : matchers) {
            if (matcher.matches(methodType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Only a type name or a plain variable can be dropped as the qualifier without losing an evaluation.
     */
    private static boolean isTypeOrVariable(Expression select) {
        return select instanceof J.Identifier ||
               select instanceof J.FieldAccess && ((J.FieldAccess) select).getName().getFieldType() == null;
    }
}
