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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import static org.openrewrite.Tree.randomId;


public class DetectAWTGetPeerMethod extends Recipe {
    @Override
    public String getDisplayName() {
        return "Replace `getPeer()` method";
    }

    @Override
    public String getDescription() {
        return " All methods that refer to types defined in the java.awt.peer package are removed in Java 11. "
                + "This recipe replaces the use of getPeer() method on the java.awt.Component, java.awt.Font, and java.awt.MenuComponent classes and direct known subclasse."
                + "The occurrence of  `if (component.getPeer() != null) { .. }` is replaced with `if (component.isDisplayable())` "
                + "and the occurence of `if (component.getPeer() instanceof LightweightPeer)` "
                + "is replaced with `if (component.isLightweight())`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>("com.test.Component1 getPeer()"), new JavaVisitor<ExecutionContext>() {
            public J.If visitIf(J.If iff, ExecutionContext ctx) {
                J.If ifJ = (J.If) super.visitIf(iff, ctx);
                Expression ifCExp = ifJ.getIfCondition().getTree();
                //J.ControlParentheses<Expression> ifCExp1 =  ifJ.getIfCondition();
                if (!(ifCExp instanceof J.Binary)) {
                    return ifJ;
                }
                J.Binary binaryCondition = (J.Binary) ifCExp;
                //check if(x.getPeer() != null)
                if (binaryCondition.getLeft() instanceof J.MethodInvocation && binaryCondition.getRight() instanceof J.Literal && binaryCondition.getOperator().name().equals("NotEqual")) {
                    J.MethodInvocation mi = (J.MethodInvocation) binaryCondition.getLeft();
                    J.Literal lt = (J.Literal) binaryCondition.getRight();
                    if (mi.getName().getSimpleName().equals("getPeer") && lt.getValueSource().equals("null")) {
                        String objectName = ((J.Identifier) mi.getSelect()).getSimpleName();
                        JavaTemplate newIfConditionTemplate = JavaTemplate.builder(objectName + ".isDisplayable()")
                                                                        .imports("java.awt.Component").build();
                        ifJ = newIfConditionTemplate.apply(getCursor(), ifCExp.getCoordinates().replace());
//                                       Expression newIfExpression = newIfConditionTemplate.apply(new Cursor(getCursor(),ifCExp),ifCExp.getCoordinates().replace());;
//                                       J.ControlParentheses<Expression> jNewCP = new J.ControlParentheses<>(randomId(),Space.EMPTY,Markers.EMPTY,JRightPadded.build(newIfExpression)).withPrefix(Space.SINGLE_SPACE);
//                                       ifJ = ifJ.withIfCondition(jNewCP);

                        return ifJ;
                    }
                }
                return ifJ;
            }

            public <T extends J> J.ControlParentheses<T> visitControlParentheses(J.ControlParentheses<T> cp, ExecutionContext ctx) {
                Expression ifCExp = (Expression) cp.getTree();
                if (!(ifCExp instanceof J.Binary)) {
                    return cp;
                }
                J.Binary binaryCondition = (J.Binary) ifCExp;
                //check if(x.getPeer() != null)
                if (binaryCondition.getLeft() instanceof J.MethodInvocation && binaryCondition.getRight() instanceof J.Literal && binaryCondition.getOperator().name().equals("NotEqual")) {
                    J.MethodInvocation mi = (J.MethodInvocation) binaryCondition.getLeft();
                    J.Literal lt = (J.Literal) binaryCondition.getRight();
                    if (mi.getName().getSimpleName().equals("getPeer") && lt.getValueSource().equals("null")) {
                        String objectName = ((J.Identifier) mi.getSelect()).getSimpleName();
                        JavaTemplate newIfConditionTemplate = JavaTemplate.builder(objectName + ".isDisplayable()")
                                                                .imports("java.awt.Component").build();
                        //cp = newIfConditionTemplate.apply(getCursor(),cp.getCoordinates().replace());
                        Expression newIfExpression = newIfConditionTemplate.apply(getCursor(), cp.getCoordinates().replace());
                        J.ControlParentheses<Expression> jNewCP = new J.ControlParentheses<>(randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(newIfExpression));
                        return (J.ControlParentheses<T>) jNewCP;
                    }
                }
                return cp;
            }
        });
    }
}
