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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

public class RemovedSecurityManagerMethods extends Recipe {
    @Override
    public String getDisplayName() {
        return "Replace deprecated methods in`SecurityManager`";
    }

    @Override
    public String getDescription() {
        return "Replace `SecurityManager` methods `checkAwtEventQueueAccess()`, `checkSystemClipboardAccess()`, " +
               "`checkMemberAccess()` and `checkTopLevelWindow()` deprecated in Java SE 11 by " +
               "`checkPermission(new java.security.AllPermission())`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            private final MethodMatcher METHOD_PATTERN_QUE = new MethodMatcher("java.lang.SecurityManager checkAwtEventQueueAccess()", false);
            private final MethodMatcher METHOD_PATTERN_CLIP = new MethodMatcher("java.lang.SecurityManager checkSystemClipboardAccess()", false);
            private final MethodMatcher METHOD_PATTERN_MEMBER = new MethodMatcher("java.lang.SecurityManager checkMemberAccess(..)", false);
            private final MethodMatcher METHOD_PATTERN_WINDOW = new MethodMatcher("java.lang.SecurityManager checkTopLevelWindow(..)", false);

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (METHOD_PATTERN_QUE.matches(method) || METHOD_PATTERN_CLIP.matches(method) || METHOD_PATTERN_MEMBER.matches(method) || METHOD_PATTERN_WINDOW.matches(method)) {
                    return JavaTemplate.builder("checkPermission(new java.security.AllPermission())")
                            .imports("java.security.AllPermission")
                            .build().apply(updateCursor(method),
                                    method.getCoordinates().replaceMethod());
                }
                return method;
            }
        };
    }
}
