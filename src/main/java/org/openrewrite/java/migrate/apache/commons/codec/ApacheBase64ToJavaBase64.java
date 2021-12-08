/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.migrate.apache.commons.codec;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

public class ApacheBase64ToJavaBase64 extends Recipe {
    @Override
    public String getDisplayName() {
        return "Migrate apache.commons.codec.binary.Base64 to java.util.Base64";
    }

    @Override
    public String getDescription() {
        return "Migrate `apache.commons.codec.binary.Base64#encodeBase64` to `java.util.Base64.Encoder#encodeBase64`, `apache.commons.codec.binary.Base64#encodeBase64String` to `java.util.Base64.Encoder#encodeToString`, and `apache.commons.codec.binary.Base64#decodeBase64` to `java.util.Base64.Decoder#decode`";
    }

    @Override
    protected UsesType<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>("org.apache.commons.codec.binary.Base64");
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final MethodMatcher apacheEncodeToString = new MethodMatcher("org.apache.commons.codec.binary.Base64 encodeBase64String(byte[])");
            private final MethodMatcher apacheEncode64 = new MethodMatcher("org.apache.commons.codec.binary.Base64 encodeBase64(byte[])");
            private final MethodMatcher apacheDecode = new MethodMatcher("org.apache.commons.codec.binary.Base64 decodeBase64(..)");

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
                String templatePrefix = null;
                if (apacheEncodeToString.matches(mi)) {
                    String argType = mi.getArguments().get(0).getType() instanceof JavaType.Array ? "#{anyArray()}" : "#{any(String)}";
                    templatePrefix = "Base64.getEncoder().encodeToString(" + argType + ")";
                } else if (apacheEncode64.matches(mi)) {
                    templatePrefix = "Base64.getEncoder().encode(#{anyArray()})";
                } else if (apacheDecode.matches(mi)) {
                    templatePrefix = "Base64.getDecoder().decode(#{any(String)})";
                }
                if (templatePrefix != null) {
                    JavaTemplate t = JavaTemplate.builder(this::getCursor, templatePrefix).imports("java.util.Base64").build();
                    maybeRemoveImport("org.apache.commons.codec.binary.Base64");
                    maybeAddImport("java.util.Base64");
                    mi = mi.withTemplate(t, mi.getCoordinates().replace(), mi.getArguments().get(0));
                }
                return mi;
            }
        };
    }
}
