/*
 * Copyright 2024 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

@Value
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor(access = AccessLevel.PACKAGE) // For tests
class ReplaceAWTGetPeerMethod extends Recipe {
    String methodPatternGetPeer;
    String methodUpdateIsDisplayable;
    String className;
    String methodUpdateIsLightweight;

    @JsonCreator
    public ReplaceAWTGetPeerMethod() {
        this.methodPatternGetPeer = "java.awt.* getPeer()";
        this.methodUpdateIsDisplayable = "java.awt.* isDisplayable()";
        this.className = "java.awt.peer.LightweightPeer";
        this.methodUpdateIsLightweight = "java.awt.*  isLightweight()";
    }

    @Override
    public String getDisplayName() {
        return "Replace `getPeer()` method";
    }

    @Override
    public String getDescription() {
        return " All methods that refer to types defined in the java.awt.peer package are removed in Java 11. "
                + "This recipe replaces the use of getPeer() method in the java.awt.Component, java.awt.Font, and java.awt.MenuComponent classes and direct known subclasse."
                + "The occurrence of  `(component.getPeer() != null) { .. }` is replaced with `(component.isDisplayable())` "
                + "and the occurrence of `(component.getPeer() instanceof LightweightPeer)` "
                + "is replaced with `(component.isLightweight())`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher methodMatcherGetPeer = new MethodMatcher(methodPatternGetPeer);
        return Preconditions.check(new UsesMethod<>(methodMatcherGetPeer), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitBinary(J.Binary binary, ExecutionContext ctx){
                J.Binary binaryCondition = (J.Binary) super.visitBinary(binary, ctx);

                //check if(x.getPeer() != null)
                //Not checking (null != getPeer())
                if (checkMethodInvocationNotEqualLiteralInBinary(binaryCondition)) {
                    J.MethodInvocation mi = (J.MethodInvocation) binaryCondition.getLeft();
                    J.Literal lt = (J.Literal) binaryCondition.getRight();
                    if (methodMatcherGetPeer.matches(mi.getMethodType())) {
                        assert lt.getValueSource() != null;
                        if (lt.getValueSource().equals("null")) {
                            mi = (J.MethodInvocation) new ChangeMethodName(methodPatternGetPeer, "isDisplayable", true, null
                            ).getVisitor().visit(mi, ctx);
                            mi = (J.MethodInvocation) new ChangeMethodInvocationReturnType(methodUpdateIsDisplayable, "boolean").getVisitor().visit(mi, ctx);
                            assert mi != null;
                            return mi.withPrefix(binaryCondition.getPrefix());
                        }
                    }
                }

                return binaryCondition;
            }
            @Override
            public J visitInstanceOf(J.InstanceOf instOf, ExecutionContext ctx){
                J.InstanceOf instanceOfVar = (J.InstanceOf) super.visitInstanceOf(instOf, ctx);

                if (instanceOfVar.getExpression() instanceof J.MethodInvocation) {
                    J.MethodInvocation mi = ((J.MethodInvocation) instanceOfVar.getExpression());
                    if (methodMatcherGetPeer.matches(mi.getMethodType()) && checkClassNameIsEqualToFQCN(instanceOfVar)) {
                        mi = (J.MethodInvocation) new ChangeMethodName(methodPatternGetPeer, "isLightweight", true, null
                        ).getVisitor().visit(mi, ctx);
                        mi = (J.MethodInvocation) new ChangeMethodInvocationReturnType(methodUpdateIsLightweight, "boolean").getVisitor().visit(mi, ctx);
                        assert mi != null;
                        maybeRemoveImport(className);
                        return mi.withPrefix(instanceOfVar.getPrefix());
                    }
                }

                return instanceOfVar;
            }
        });
    }

    private boolean checkClassNameIsEqualToFQCN(J.InstanceOf instOf) {
        if (instOf.getClazz() instanceof J.Identifier) {
            J.Identifier id = (J.Identifier) instOf.getClazz();
            assert id.getType() != null;
            return ((JavaType.Class) id.getType()).getFullyQualifiedName().equals(className);
        } else if (instOf.getClazz() instanceof J.FieldAccess) {
            J.FieldAccess fid = (J.FieldAccess) instOf.getClazz();
            assert fid.getType() != null;
            return ((JavaType.Class) fid.getType()).getFullyQualifiedName().equals(className);
        } else {
            return false;
        }
    }

    private boolean checkMethodInvocationNotEqualLiteralInBinary(J.Binary binaryCondition) {
        if (binaryCondition.getLeft() instanceof J.MethodInvocation) {
            return (binaryCondition.getRight() instanceof J.Literal && binaryCondition.getOperator() == J.Binary.Type.NotEqual);
        }
        if (binaryCondition.getRight() instanceof J.MethodInvocation) {
            return (binaryCondition.getLeft() instanceof J.Literal && binaryCondition.getOperator() == J.Binary.Type.NotEqual);
        }
        return false;
    }

}
