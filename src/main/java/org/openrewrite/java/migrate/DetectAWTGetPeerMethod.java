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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;

class DetectAWTGetPeerMethod extends Recipe {
    private final String methodPatternGetPeer ;
    private final String methodUpdateIsDisplayable;
    private final String className;
    private final String methodUpdateIsLightweight;
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
    @JsonCreator
    public DetectAWTGetPeerMethod() {
        this.methodPatternGetPeer = "java.awt.* getPeer()";
        this.methodUpdateIsDisplayable = "java.awt.* isDisplayable()";
        this.className = "java.awt.peer.LightweightPeer";
        this.methodUpdateIsLightweight = "java.awt.*  isLightweight()";
    }
    /**
     * Overload constructor to allow for custom method patterns used in tests only.
     */
    DetectAWTGetPeerMethod(String methodPatternGetPeer, String methodUpdatedIsDisplayable, String className, String methodUpdateIsLightweight ) {
        this.methodPatternGetPeer = methodPatternGetPeer;
        this.methodUpdateIsDisplayable = methodUpdatedIsDisplayable;
        this.className = className;
        this.methodUpdateIsLightweight = methodUpdateIsLightweight;
    }
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(methodPatternGetPeer), new JavaVisitor<ExecutionContext>() {
            @Override
            public <T extends J> J.ControlParentheses<T> visitControlParentheses(J.ControlParentheses<T> cp, ExecutionContext ctx) {
                Expression ifCExp = (Expression) cp.getTree();
                //Check if there is a Binary or an instanceOf inside the paranthesis
                if (!(ifCExp instanceof J.Binary) && !(ifCExp instanceof J.InstanceOf)) {
                    return cp;
                }
                if (ifCExp instanceof J.Binary) {
                    J.Binary binaryCondition = (J.Binary) ifCExp;
                    //check if(x.getPeer() != null)
                    if (binaryCondition.getLeft() instanceof J.MethodInvocation && binaryCondition.getRight() instanceof J.Literal && binaryCondition.getOperator().name().equals("NotEqual")) {
                        J.MethodInvocation mi = (J.MethodInvocation) binaryCondition.getLeft();
                        J.Literal lt = (J.Literal) binaryCondition.getRight();
                        if (mi.getName().getSimpleName().equals("getPeer") && lt.getValueSource().equals("null")) {
                            mi = (J.MethodInvocation) new ChangeMethodName(methodPatternGetPeer, "isDisplayable", true, null
                            ).getVisitor().visit(mi, ctx);
                            mi = (J.MethodInvocation) new ChangeMethodInvocationReturnType(methodUpdateIsDisplayable,"boolean").getVisitor().visit(mi, ctx);
                            return cp.withTree((T) mi);
                        }
                    }
                }
                else if (ifCExp instanceof J.InstanceOf){
                    J.InstanceOf instanceOfVar = (J.InstanceOf) ifCExp;
                    if((instanceOfVar.getExpression() instanceof J.MethodInvocation)) {
                        J.MethodInvocation mi = ((J.MethodInvocation) instanceOfVar.getExpression());
                        if (mi.getName().getSimpleName().equals("getPeer") && checkClassNameIsEqualToFQCN(instanceOfVar)) {
                            mi = (J.MethodInvocation) new ChangeMethodName(methodPatternGetPeer, "isLightweight", true, null
                            ).getVisitor().visit(mi, ctx);
                            mi = (J.MethodInvocation) new ChangeMethodInvocationReturnType(methodUpdateIsLightweight, "boolean").getVisitor().visit(mi, ctx);
                            return cp.withTree((T) mi);
                        }
                    }
                }
                return cp;
            }
          });
    }
    private boolean checkClassNameIsEqualToFQCN(J.InstanceOf instOf){
        if(instOf.getClazz() instanceof J.Identifier){
            J.Identifier id = (J.Identifier) instOf.getClazz();
            return ((JavaType.Class) id.getType()).getFullyQualifiedName().toString().equals(className);
        }
        else if (instOf.getClazz() instanceof J.FieldAccess){
            J.FieldAccess fid = (J.FieldAccess) instOf.getClazz();
            return ((JavaType.Class) fid.getType()).getFullyQualifiedName().toString().equals(className);
        }
        else{
            return false;
        }
    }
}
