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
package org.openrewrite.java.migrate.jakarta;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesField;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemovedUIComponentConstant extends Recipe {
    private static final String UI_COMPONENT = "jakarta.faces.component.UIComponent";
    private static final MethodMatcher GET_ATTRIBUTES = new MethodMatcher("jakarta.faces.context.FacesContext getAttributes()", true);
    private static final MethodMatcher MAP_GET = new MethodMatcher("java.util.Map get(java.lang.Object)", true);
    private static final MethodMatcher GET_CURRENT = new MethodMatcher(UI_COMPONENT + " getCurrent*Component(jakarta.faces.context.FacesContext)");

    String displayName = "Replace `CURRENT_COMPONENT` and `CURRENT_COMPOSITE_COMPONENT` with `getCurrentComponent()` and `getCurrentCompositeComponent()`";

    String description = "Replace lookups of the `jakarta.faces.component.UIComponent.CURRENT_COMPONENT` and `CURRENT_COMPOSITE_COMPONENT` " +
            "keys in the `FacesContext` attributes with `UIComponent.getCurrentComponent(FacesContext)` and " +
            "`UIComponent.getCurrentCompositeComponent(FacesContext)`, which were added in JSF 2.0.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesField<>(UI_COMPONENT, "CURRENT_*"), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitTypeCast(J.TypeCast typeCast, ExecutionContext ctx) {
                J j = super.visitTypeCast(typeCast, ctx);
                // the method returns a UIComponent, which the attribute lookup had to be cast to
                if (j instanceof J.TypeCast && ((J.TypeCast) j).getExpression() != typeCast.getExpression() &&
                    GET_CURRENT.matches(((J.TypeCast) j).getExpression()) &&
                    TypeUtils.isOfClassType(((J.TypeCast) j).getType(), UI_COMPONENT)) {
                    return ((J.TypeCast) j).getExpression().withPrefix(j.getPrefix());
                }
                return j;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J j = super.visitMethodInvocation(method, ctx);
                if (!(j instanceof J.MethodInvocation)) {
                    return j;
                }
                J.MethodInvocation m = (J.MethodInvocation) j;
                if (!MAP_GET.matches(m) || !(m.getSelect() instanceof J.MethodInvocation)) {
                    return m;
                }
                J.MethodInvocation attributes = (J.MethodInvocation) m.getSelect();
                String getter = getterFor(m.getArguments().get(0));
                if (getter == null || !GET_ATTRIBUTES.matches(attributes) || attributes.getSelect() == null) {
                    return m;
                }
                maybeAddImport(UI_COMPONENT);
                return JavaTemplate.builder("UIComponent." + getter + "(#{any(jakarta.faces.context.FacesContext)})")
                        .imports(UI_COMPONENT)
                        .javaParser(JavaParser.fromJavaVersion().dependsOn(
                                "package jakarta.faces.context; public abstract class FacesContext {}",
                                "package jakarta.faces.component; public abstract class UIComponent {" +
                                " public static UIComponent getCurrentComponent(jakarta.faces.context.FacesContext context) { return null; }" +
                                " public static UIComponent getCurrentCompositeComponent(jakarta.faces.context.FacesContext context) { return null; } }"))
                        .build()
                        .apply(updateCursor(m), m.getCoordinates().replace(), attributes.getSelect());
            }
        });
    }

    private static @Nullable String getterFor(Expression key) {
        JavaType.Variable field = key instanceof J.FieldAccess ? ((J.FieldAccess) key).getName().getFieldType() :
                key instanceof J.Identifier ? ((J.Identifier) key).getFieldType() : null;
        if (field != null && TypeUtils.isOfClassType(field.getOwner(), UI_COMPONENT)) {
            if ("CURRENT_COMPONENT".equals(field.getName())) {
                return "getCurrentComponent";
            } else if ("CURRENT_COMPOSITE_COMPONENT".equals(field.getName())) {
                return "getCurrentCompositeComponent";
            }
        }
        return null;
    }
}
