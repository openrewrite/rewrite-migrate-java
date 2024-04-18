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
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.Collections;

import static org.openrewrite.Tree.randomId;

public class RemovedSecurityManagerMethods extends Recipe {
    @Override
    public String getDisplayName() {
        return "Replace deprecated methods in`SecurityManager`";
    }

    @Override
    public String getDescription() {
        return "This recipe replaces the methods deprecated in the SecurityManager class in Java SE 11" +
                "The methods `checkAwtEventQueueAccess()`,`checkSystemClipboardAccess()`,`checkMemberAccess()`,`checkTopLevelWindow()`" +
                " are replaced by `checkPermission(new java.security.AllPermission())`," +
                " the methods `inClass()`, `inClassLoader()` and `getInCheck()` return false" +
                " and  when the  methods `classDepth()` and `classLoaderDepth()` are invoked they return 0." +
                " The recipe also replaces the boolean data field `inCheck` with false";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            private final MethodMatcher METHOD_PATTERN_QUE = new MethodMatcher("java.lang.SecurityManager checkAwtEventQueueAccess()", false);
            private final MethodMatcher METHOD_PATTERN_CLIP = new MethodMatcher("java.lang.SecurityManager checkSystemClipboardAccess()", false);
            private final MethodMatcher METHOD_PATTERN_MEMBER = new MethodMatcher("java.lang.SecurityManager checkMemberAccess(..)", false);
            private final MethodMatcher METHOD_PATTERN_WINDOW = new MethodMatcher("java.lang.SecurityManager checkTopLevelWindow(..)", false);
            private final MethodMatcher METHOD_PATTERN_IN_CLASS = new MethodMatcher("java.lang.SecurityManager inClass(..)", false);
            private final MethodMatcher METHOD_PATTERN_IN_CLASSLOADER = new MethodMatcher("java.lang.SecurityManager inClassLoader(..)", false);
            private final MethodMatcher METHOD_PATTERN_IN_CHECK = new MethodMatcher("java.lang.SecurityManager getInCheck()", false);

            private final MethodMatcher METHOD_PATTERN_CLASSDEPTH = new MethodMatcher("java.lang.SecurityManager classDepth(String)", false);

            private final MethodMatcher METHOD_PATTERN_CLASSLOADERDEPTH = new MethodMatcher("java.lang.SecurityManager classLoaderDepth()", false);

            private final String FIELD_IN_CHECK = "inCheck";
            private final String SECURITY_MGR_PACKAGE = "java.lang.SecurityManager";

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if ((METHOD_PATTERN_QUE.matches(method) || (METHOD_PATTERN_CLIP).matches(method) || (METHOD_PATTERN_MEMBER).matches(method)) || (METHOD_PATTERN_WINDOW).matches(method)) {
                    return JavaTemplate.builder("checkPermission(new java.security.AllPermission())")
                            .imports("java.security.AllPermission")
                            .build().apply(updateCursor(method),
                                    method.getCoordinates().replaceMethod());
                } else if ((METHOD_PATTERN_IN_CLASS).matches(method) || (METHOD_PATTERN_IN_CLASSLOADER.matches(method)) || (METHOD_PATTERN_IN_CHECK.matches(method))) {
                    return new J.Literal(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, Boolean.FALSE, "false", null, JavaType.Primitive.Boolean);
                } else if ((METHOD_PATTERN_CLASSDEPTH).matches(method) || (METHOD_PATTERN_CLASSLOADERDEPTH.matches(method))) {
                    return new J.Literal(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, Integer.valueOf(0), "0", null, JavaType.Primitive.Int);
                }
                return method;
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {

                if (FIELD_IN_CHECK.equals(identifier.getSimpleName())) {
                    if (identifier.getFieldType() != null) {
                        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(identifier.getFieldType().getOwner());
                        if (fq != null && SECURITY_MGR_PACKAGE.equals(fq.getFullyQualifiedName())) {
                            if (identifier.getComments().isEmpty()) {
                                identifier = getWithComment(identifier);
                            }
                            identifier = identifier.withSimpleName("false");
                        }
                    }
                    return identifier;
                }
                return identifier;
            }

            private J.Identifier getWithComment(J j) {
                String prefixWhitespace = j.getPrefix().getWhitespace();
                String commentText =
                        prefixWhitespace + "`inCheck` is deprecated now this value is `false`, it should not be used!!!";
                return j.withComments(Collections.singletonList(new TextComment(true, commentText, "", Markers.EMPTY)));
            }
        };
    }
}
