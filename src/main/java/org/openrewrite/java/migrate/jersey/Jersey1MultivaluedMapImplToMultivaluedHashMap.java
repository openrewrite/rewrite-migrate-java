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
package org.openrewrite.java.migrate.jersey;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

import java.util.concurrent.atomic.AtomicBoolean;

@Value
@EqualsAndHashCode(callSuper = false)
public class Jersey1MultivaluedMapImplToMultivaluedHashMap extends Recipe {

    private static final String MULTIVALUED_MAP_IMPL = "com.sun.jersey.core.util.MultivaluedMapImpl";
    private static final String MULTIVALUED_HASH_MAP = "javax.ws.rs.core.MultivaluedHashMap";

    private static final MethodMatcher ADD = new MethodMatcher(MULTIVALUED_MAP_IMPL + " add(java.lang.String, ..)");
    private static final MethodMatcher PUT_SINGLE = new MethodMatcher(MULTIVALUED_MAP_IMPL + " putSingle(java.lang.String, ..)");

    String displayName = "Use JAX-RS `MultivaluedHashMap` instead of the Jersey 1 `MultivaluedMapImpl`";

    String description = "Replaces `com.sun.jersey.core.util.MultivaluedMapImpl`, a `MultivaluedMap<String, String>` that " +
            "is not generic itself, with `javax.ws.rs.core.MultivaluedHashMap<String, String>`. Constructor calls, " +
            "including the copy constructor, and declared types of fields, variables, parameters, return values, casts and " +
            "type arguments get the type arguments, so that loops over `entrySet()` keep compiling; `instanceof` and class " +
            "literals use the raw type. Values that are not strings, which `MultivaluedMapImpl.add` and `putSingle` turned " +
            "into strings with `toString()`, or into an empty string when `null`, are turned into strings the same way. " +
            "Files that extend `MultivaluedMapImpl`, anonymously or not, are left alone, as are calls to the conversions " +
            "`getFirst(String, Class)`, `getFirst(String, Object)` and `get(String, Class)`, which have no counterpart.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(MULTIVALUED_MAP_IMPL, false), new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                if (extendsMultivaluedMapImpl(cu)) {
                    return cu;
                }
                J c = super.visitCompilationUnit(cu, ctx);
                // what is left: instanceof, class literals, imports and the types of expressions
                return (J) new ChangeType(MULTIVALUED_MAP_IMPL, MULTIVALUED_HASH_MAP, true).getVisitor().visitNonNull(c, ctx);
            }

            @Override
            public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                J.VariableDeclarations v = (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, ctx);
                if (isMultivaluedMapImpl(v.getTypeExpression())) {
                    v = v.withTypeExpression(parameterized(v.getTypeExpression(), ctx));
                    maybeAddImport(MULTIVALUED_HASH_MAP);
                }
                return v;
            }

            @Override
            public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = (J.MethodDeclaration) super.visitMethodDeclaration(method, ctx);
                if (isMultivaluedMapImpl(m.getReturnTypeExpression())) {
                    m = m.withReturnTypeExpression(parameterized(m.getReturnTypeExpression(), ctx));
                    maybeAddImport(MULTIVALUED_HASH_MAP);
                }
                return m;
            }

            @Override
            public J visitTypeCast(J.TypeCast typeCast, ExecutionContext ctx) {
                J.TypeCast t = (J.TypeCast) super.visitTypeCast(typeCast, ctx);
                if (isMultivaluedMapImpl(t.getClazz().getTree())) {
                    t = t.withClazz(t.getClazz().withTree(parameterized(t.getClazz().getTree(), ctx)));
                    maybeAddImport(MULTIVALUED_HASH_MAP);
                }
                return t;
            }

            @Override
            public J visitParameterizedType(J.ParameterizedType type, ExecutionContext ctx) {
                J.ParameterizedType p = (J.ParameterizedType) super.visitParameterizedType(type, ctx);
                if (p.getTypeParameters() == null) {
                    return p;
                }
                return p.withTypeParameters(ListUtils.map(p.getTypeParameters(), t -> {
                    if (isMultivaluedMapImpl(t)) {
                        maybeAddImport(MULTIVALUED_HASH_MAP);
                        return parameterized((TypeTree) t, ctx);
                    }
                    return t;
                }));
            }

            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass n = (J.NewClass) super.visitNewClass(newClass, ctx);
                if (n.getBody() == null && isMultivaluedMapImpl(n.getClazz())) {
                    n = n.withClazz(parameterized(n.getClazz(), ctx));
                    maybeAddImport(MULTIVALUED_HASH_MAP);
                }
                return n;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if ((ADD.matches(m) || PUT_SINGLE.matches(m)) && m.getArguments().size() == 2) {
                    Expression value = m.getArguments().get(1);
                    JavaType type = value.getType();
                    if (type == null || TypeUtils.isString(type) || type == JavaType.Primitive.Null ||
                        type instanceof JavaType.Unknown) {
                        return m;
                    }
                    String code;
                    if (type instanceof JavaType.Primitive) {
                        code = "String.valueOf(#{any()})";
                    } else {
                        maybeAddImport("java.util.Objects");
                        code = "Objects.toString(#{any()}, \"\")";
                    }
                    Expression converted = JavaTemplate.builder(code)
                            .imports("java.util.Objects")
                            .build()
                            .apply(new Cursor(getCursor(), value), value.getCoordinates().replace(), value);
                    m = m.withArguments(ListUtils.map(m.getArguments(), (i, a) -> i == 1 ? converted : a));
                }
                return m;
            }
        });
    }

    private static boolean isMultivaluedMapImpl(@Nullable Tree tree) {
        return (tree instanceof J.Identifier || tree instanceof J.FieldAccess) &&
               TypeUtils.isOfClassType(((TypeTree) tree).getType(), MULTIVALUED_MAP_IMPL);
    }

    private static boolean extendsMultivaluedMapImpl(JavaSourceFile sourceFile) {
        AtomicBoolean found = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, AtomicBoolean found) {
                if (classDecl.getExtends() != null &&
                    TypeUtils.isOfClassType(classDecl.getExtends().getType(), MULTIVALUED_MAP_IMPL)) {
                    found.set(true);
                }
                return super.visitClassDeclaration(classDecl, found);
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, AtomicBoolean found) {
                if (newClass.getBody() != null && newClass.getClazz() != null &&
                    TypeUtils.isOfClassType(newClass.getClazz().getType(), MULTIVALUED_MAP_IMPL)) {
                    found.set(true);
                }
                return super.visitNewClass(newClass, found);
            }
        }.visit(sourceFile, found);
        return found.get();
    }

    /**
     * @return {@code MultivaluedHashMap<String, String>}, in the place of the type tree.
     */
    private static J.ParameterizedType parameterized(@Nullable TypeTree replaced, ExecutionContext ctx) {
        J.ParameterizedType prototype = ctx.computeMessageIfAbsent(
                Jersey1MultivaluedMapImplToMultivaluedHashMap.class.getName() + ".prototype", k -> parsePrototype(ctx));
        return prototype
                .withId(Tree.randomId())
                .withPrefix(replaced == null ? Space.EMPTY : replaced.getPrefix())
                .withClazz(prototype.getClazz().withId(Tree.randomId()))
                .withTypeParameters(ListUtils.map(prototype.getTypeParameters(), t -> t.withId(Tree.randomId())));
    }

    private static J.ParameterizedType parsePrototype(ExecutionContext ctx) {
        SourceFile stub = Jersey2Templates.parser(ctx).build()
                .parse(ctx, "import javax.ws.rs.core.MultivaluedHashMap;\n" +
                            "class Stub { MultivaluedHashMap<String, String> map; }")
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Could not parse MultivaluedHashMap<String, String>"));
        J.ClassDeclaration classDeclaration = ((J.CompilationUnit) stub).getClasses().get(0);
        J.VariableDeclarations field = (J.VariableDeclarations) classDeclaration.getBody().getStatements().get(0);
        return (J.ParameterizedType) requireNonNull(field.getTypeExpression()).withPrefix(Space.EMPTY);
    }

    private static <T> T requireNonNull(@Nullable T t) {
        if (t == null) {
            throw new IllegalStateException("Could not parse MultivaluedHashMap<String, String>");
        }
        return t;
    }
}
