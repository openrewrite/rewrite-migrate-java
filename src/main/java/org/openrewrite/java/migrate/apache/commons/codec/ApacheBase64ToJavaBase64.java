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
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ApacheBase64ToJavaBase64 extends Recipe {
    @Override
    public String getDisplayName() {
        return "Prefer `java.util.Base64`";
    }

    @Override
    public String getDescription() {
        return "Prefer the Java standard library's `java.util.Base64` over third-party usage of apache's `apache.commons.codec.binary.Base64`.";
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("apache", "commons"));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("org.apache.commons.codec.binary.Base64", false), new JavaIsoVisitor<ExecutionContext>() {
            private final MethodMatcher apacheEncodeToString = new MethodMatcher("org.apache.commons.codec.binary.Base64 encodeBase64String(byte[])");
            private final MethodMatcher apacheEncode64 = new MethodMatcher("org.apache.commons.codec.binary.Base64 encodeBase64(byte[])");
            private final MethodMatcher apacheDecode = new MethodMatcher("org.apache.commons.codec.binary.Base64 decodeBase64(..)");
            private final MethodMatcher apacheEncode64UrlSafe = new MethodMatcher("org.apache.commons.codec.binary.Base64 encodeBase64URLSafe(..)");
            private final MethodMatcher apacheEncode64UrlSafeString = new MethodMatcher("org.apache.commons.codec.binary.Base64 encodeBase64URLSafeString(..)");

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                String templatePrefix = null;
                if (apacheEncodeToString.matches(mi)) {
                    String argType = mi.getArguments().get(0).getType() instanceof JavaType.Array ? "#{anyArray()}" : "#{any(String)}";
                    templatePrefix = "Base64.getEncoder().encodeToString(" + argType + ")";
                } else if (apacheEncode64.matches(mi)) {
                    templatePrefix = "Base64.getEncoder().encode(#{anyArray()})";
                } else if (apacheDecode.matches(mi)) {
                    templatePrefix = "Base64.getDecoder().decode(#{any(String)})";
                } else if (apacheEncode64UrlSafe.matches(mi)) {
                    templatePrefix = "Base64.getUrlEncoder().withoutPadding().encode(#{anyArray()})";
                } else if (apacheEncode64UrlSafeString.matches(mi)) {
                    templatePrefix = "Base64.getUrlEncoder().withoutPadding().encodeToString(#{anyArray()})";
                }
                if (templatePrefix != null) {
                    JavaTemplate t = JavaTemplate.builder(this::getCursor, templatePrefix).imports("java.util.Base64").build();
                    maybeRemoveImport("org.apache.commons.codec.binary.Base64");
                    maybeAddImport("java.util.Base64");
                    mi = mi.withTemplate(t, mi.getCoordinates().replace(), mi.getArguments().get(0));
                }
                return mi;
            }
        });
    }
}
