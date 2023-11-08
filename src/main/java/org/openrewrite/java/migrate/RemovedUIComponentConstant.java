/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemovedUIComponentConstant extends Recipe {

    @Override
    public String getDisplayName() {
        return "Replaces removed UIComponent Constants";
    }

    @Override
    public String getDescription() {
        return "Replaces removed jakarta.faces.component.UIComponent Constants with the new methods added in JSF 2.0.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("jakarta.faces.component.UIComponent", false), new RemovedUIComponentConstant.ReplaceUIComponentConstantsVisitor());
    }

    private static class ReplaceUIComponentConstantsVisitor extends JavaVisitor<ExecutionContext> {

        private final String existingOwningType = "jakarta.faces.component.UIComponent";
        private final String currentComponent = "CURRENT_COMPONENT";
        private final String currentCompositeComponent = "CURRENT_COMPOSITE_COMPONENT";
        public String methodTemplate = null;

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            JavaType.Variable fieldType = fieldAccess.getName().getFieldType();
            if (isConstant(fieldType)) {
                return JavaTemplate.builder(methodTemplate)
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), fieldAccess.getCoordinates().replace());
            }
            return super.visitFieldAccess(fieldAccess, ctx);
        }

        @Override
        public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
            JavaType.Variable fieldType = ident.getFieldType();
            if (isConstant(fieldType) && !isVariableDeclaration()) {
                return JavaTemplate.builder(methodTemplate)
                        .contextSensitive()
                        .build()
                        .apply(getCursor(), ident.getCoordinates().replace());
            }
            return super.visitIdentifier(ident, ctx);
        }

        private boolean isConstant(@Nullable JavaType.Variable varType) {
            boolean equalsConstant = false;
            if (varType != null) {
                if (varType.getName().equals(currentComponent)) {
                    methodTemplate = "jakarta.faces.component.UIComponent.getCurrentComponent()";
                    equalsConstant = true;
                } else if (varType.getName().equals(currentCompositeComponent)) {
                    methodTemplate = "jakarta.faces.component.UIComponent.getCurrentCompositeComponent()";
                    equalsConstant = true;
                }
            }

            return TypeUtils.isOfClassType(varType.getOwner(), existingOwningType) && equalsConstant;
        }

        private boolean isVariableDeclaration() {
            Cursor maybeVariable = getCursor().dropParentUntil(is -> is instanceof J.VariableDeclarations || is instanceof J.CompilationUnit);
            if (!(maybeVariable.getValue() instanceof J.VariableDeclarations)) {
                return false;
            }
            JavaType.Variable variableType = ((J.VariableDeclarations) maybeVariable.getValue()).getVariables().get(0).getVariableType();
            if (variableType == null) {
                return true;
            }

            JavaType.FullyQualified ownerFqn = TypeUtils.asFullyQualified(variableType.getOwner());
            if (ownerFqn == null) {
                return true;
            }

            boolean equalsConstant = false;
            if (currentComponent.equals(((J.VariableDeclarations) maybeVariable.getValue()).getVariables().get(0).getSimpleName())) {
                methodTemplate = "jakarta.faces.component.UIComponent.getCurrentComponent()";
                equalsConstant = true;
            } else if (currentCompositeComponent.equals(((J.VariableDeclarations) maybeVariable.getValue()).getVariables().get(0).getSimpleName())) {
                methodTemplate = "jakarta.faces.component.UIComponent.getCurrentCompositeComponent()";
                equalsConstant = true;
            }

            return equalsConstant && existingOwningType.equals(ownerFqn.getFullyQualifiedName());
        }
    }
}
