/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.java.tree.TypedTree;

@Value
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
public class ReplaceAWTGetPeerMethod extends Recipe {

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "java.awt.* getPeer()",
            required = false)
    String getPeerMethodPattern;

    @Option(displayName = "The LightweightPeer interface FQCN",
            description = "The fully qualified class name of the LightweightPeer interface to replace in `instanceof`.",
            example = "java.awt.peer.LightweightPeer",
            required = false)
    String lightweightPeerFQCN;

    @JsonCreator
    public ReplaceAWTGetPeerMethod() {
        getPeerMethodPattern = "java.awt.* getPeer()";
        lightweightPeerFQCN = "java.awt.peer.LightweightPeer";
    }

    @Override
    public String getDisplayName() {
        return "Replace AWT `getPeer()` method";
    }

    @Override
    public String getDescription() {
        return "This recipe replaces the use of `getPeer()` method in `java.awt.*` classes. " +
                "`component.getPeer() != null` is replaced with `component.isDisplayable()` and " +
                "`component.getPeer() instanceof LightweightPeer` is replaced with `component.isLightweight()`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher methodMatcherGetPeer = new MethodMatcher(getPeerMethodPattern);
        return Preconditions.check(new UsesMethod<>(methodMatcherGetPeer), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                J.Binary bi = (J.Binary) super.visitBinary(binary, ctx);

                J.MethodInvocation mi = findMatchingMethodInvocation(bi);
                if (mi != null) {
                    mi = (J.MethodInvocation) new ChangeMethodName(
                            getPeerMethodPattern, "isDisplayable", true, null)
                            .getVisitor().visit(mi, ctx);
                    mi = (J.MethodInvocation) new ChangeMethodInvocationReturnType(
                            getPeerMethodPattern.split(" ")[0] + " isDisplayable()", "boolean")
                            .getVisitor().visit(mi, ctx);
                    assert mi != null;
                    return mi.withPrefix(bi.getPrefix());
                }

                return bi;
            }

            private J.@Nullable MethodInvocation findMatchingMethodInvocation(J.Binary binaryCondition) {
                J.MethodInvocation mi = null;
                if (binaryCondition.getOperator() == J.Binary.Type.NotEqual) {
                    if (binaryCondition.getLeft() instanceof J.MethodInvocation &&
                            binaryCondition.getRight() instanceof J.Literal) {
                        mi = (J.MethodInvocation) binaryCondition.getLeft();
                    } else if (binaryCondition.getLeft() instanceof J.Literal &&
                            binaryCondition.getRight() instanceof J.MethodInvocation) {
                        mi = (J.MethodInvocation) binaryCondition.getRight();
                    }
                }
                if (methodMatcherGetPeer.matches(mi)) {
                    return mi;
                }
                return null;
            }

            @Override
            public J visitInstanceOf(J.InstanceOf instOf, ExecutionContext ctx) {
                J.InstanceOf instanceOfVar = (J.InstanceOf) super.visitInstanceOf(instOf, ctx);

                if (instanceOfVar.getExpression() instanceof J.MethodInvocation) {
                    J.MethodInvocation mi = (J.MethodInvocation) instanceOfVar.getExpression();
                    if (methodMatcherGetPeer.matches(mi) && TypeUtils.isAssignableTo(lightweightPeerFQCN, ((TypedTree) instanceOfVar.getClazz()).getType())) {
                        mi = (J.MethodInvocation) new ChangeMethodName(getPeerMethodPattern, "isLightweight", true, null)
                                .getVisitor().visit(mi, ctx);
                        mi = (J.MethodInvocation) new ChangeMethodInvocationReturnType(
                                getPeerMethodPattern.split(" ")[0] + " isLightweight()", "boolean")
                                .getVisitor().visit(mi, ctx);
                        assert mi != null;
                        maybeRemoveImport(lightweightPeerFQCN);
                        return mi.withPrefix(instanceOfVar.getPrefix());
                    }
                }

                return instanceOfVar;
            }
        });
    }
}
