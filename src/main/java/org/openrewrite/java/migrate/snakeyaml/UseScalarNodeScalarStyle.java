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
package org.openrewrite.java.migrate.snakeyaml;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

public class UseScalarNodeScalarStyle extends Recipe {

    private static final String SCALAR_NODE = "org.yaml.snakeyaml.nodes.ScalarNode";
    private static final MethodMatcher GET_STYLE = new MethodMatcher(SCALAR_NODE + " getStyle()");

    @Getter
    final String displayName = "Use `ScalarNode.getScalarStyle()`";

    @Getter
    final String description = "SnakeYAML 2.0 removed `ScalarNode.getStyle()`, which returned the style as a " +
            "`Character`, and the `ScalarNode` constructors that take one. A `getStyle()` passed to such a " +
            "constructor becomes `getScalarStyle()`, so that the constructor taking a `DumperOptions.ScalarStyle` " +
            "is called instead, and any other `getStyle()` becomes `getScalarStyle().getChar()`. SnakeYAML 1.22 " +
            "added `getScalarStyle()`, so code compiled against an older SnakeYAML is left alone.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(GET_STYLE), new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return sourceFile instanceof J.CompilationUnit;
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass n = newClass;
                if (n.getClazz() != null && TypeUtils.isOfClassType(n.getClazz().getType(), SCALAR_NODE)) {
                    n = passScalarStyle(n);
                }
                return super.visitNewClass(n, ctx);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = method;
                if (m.getSelect() == null && "super".equals(m.getSimpleName()) && m.getMethodType() != null &&
                    TypeUtils.isOfClassType(m.getMethodType().getDeclaringType(), SCALAR_NODE)) {
                    m = passScalarStyle(m);
                }
                m = super.visitMethodInvocation(m, ctx);
                J.MethodInvocation getScalarStyle;
                if (GET_STYLE.matches(m) && (getScalarStyle = getScalarStyle(m)) != null) {
                    // Any other use expects a Character
                    JavaType.Method getChar = getChar(getScalarStyle.getMethodType());
                    return new J.MethodInvocation(randomId(), m.getPrefix(), Markers.EMPTY,
                            JRightPadded.build(getScalarStyle.withPrefix(Space.EMPTY)), null,
                            new J.Identifier(randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), "getChar", getChar, null),
                            JContainer.build(Space.EMPTY, singletonList(JRightPadded.build(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY))), Markers.EMPTY),
                            getChar);
                }
                return m;
            }

            /**
             * Passes {@code getScalarStyle()} where a constructor that SnakeYAML 2 removed was passed {@code getStyle()}.
             */
            private <M extends MethodCall> M passScalarStyle(M call) {
                JavaType.Method constructor = call.getMethodType();
                List<Expression> args = call.getArguments();
                if (constructor == null || args.isEmpty() || constructor.getParameterTypes().size() != args.size()) {
                    return call;
                }
                int last = args.size() - 1;
                Expression style = args.get(last);
                if (!TypeUtils.isOfClassType(constructor.getParameterTypes().get(last), "java.lang.Character") ||
                    !(style instanceof J.MethodInvocation) || !GET_STYLE.matches((J.MethodInvocation) style)) {
                    return call;
                }
                J.MethodInvocation getScalarStyle = getScalarStyle((J.MethodInvocation) style);
                if (getScalarStyle == null || getScalarStyle.getMethodType() == null) {
                    return call;
                }
                JavaType scalarStyle = getScalarStyle.getMethodType().getReturnType();
                JavaType.Method withScalarStyle = constructor.withParameterTypes(
                        ListUtils.map(constructor.getParameterTypes(), (i, t) -> i == last ? scalarStyle : t));
                for (JavaType.Method method : constructor.getDeclaringType().getMethods()) {
                    if (method.isConstructor() && sameParameterTypes(method, withScalarStyle)) {
                        withScalarStyle = method;
                        break;
                    }
                }
                //noinspection unchecked
                M updated = (M) call
                        .withArguments(ListUtils.map(args, (i, a) -> i == last ? getScalarStyle : a))
                        .withMethodType(withScalarStyle);
                if (updated instanceof J.MethodInvocation) {
                    J.MethodInvocation m = (J.MethodInvocation) updated;
                    //noinspection unchecked
                    updated = (M) m.withName(m.getName().withType(withScalarStyle));
                }
                return updated;
            }
        });
    }

    private static boolean sameParameterTypes(JavaType.Method a, JavaType.Method b) {
        List<JavaType> aTypes = a.getParameterTypes();
        List<JavaType> bTypes = b.getParameterTypes();
        if (aTypes.size() != bTypes.size()) {
            return false;
        }
        for (int i = 0; i < aTypes.size(); i++) {
            if (!TypeUtils.isOfType(aTypes.get(i), bTypes.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@code getScalarStyle()} in place of {@code getStyle()}, if the SnakeYAML on the classpath has it, as 1.22 and
     * later do.
     */
    private static J.@Nullable MethodInvocation getScalarStyle(J.MethodInvocation getStyle) {
        JavaType.Method type = getStyle.getMethodType();
        if (type == null) {
            return null;
        }
        for (JavaType.Method method : type.getDeclaringType().getMethods()) {
            if ("getScalarStyle".equals(method.getName()) && method.getParameterTypes().isEmpty()) {
                return getStyle
                        .withName(getStyle.getName().withSimpleName("getScalarStyle").withType(method))
                        .withMethodType(method);
            }
        }
        return null;
    }

    private static JavaType.@Nullable Method getChar(JavaType.@Nullable Method getScalarStyle) {
        if (getScalarStyle == null) {
            return null;
        }
        JavaType.FullyQualified scalarStyle = TypeUtils.asFullyQualified(getScalarStyle.getReturnType());
        if (scalarStyle == null) {
            return null;
        }
        for (JavaType.Method method : scalarStyle.getMethods()) {
            if ("getChar".equals(method.getName()) && method.getParameterTypes().isEmpty()) {
                return method;
            }
        }
        return new JavaType.Method(null, Flag.Public.getBitMask(), scalarStyle, "getChar",
                JavaType.ShallowClass.build("java.lang.Character"), emptyList(), emptyList(), emptyList(), emptyList(), null, null);
    }
}
