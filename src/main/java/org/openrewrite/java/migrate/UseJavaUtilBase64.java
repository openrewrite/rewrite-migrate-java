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
package org.openrewrite.java.migrate;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.*;
import org.openrewrite.java.*;
import org.openrewrite.java.cleanup.UnnecessaryCatch;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;

public class UseJavaUtilBase64 extends Recipe {
    private final String sunPackage;

    @Override
    public String getDisplayName() {
        return "Prefer `java.util.Base64` instead of `sun.misc`";
    }

    @Override
    public String getDescription() {
        return "Prefer `java.util.Base64` instead of using `sun.misc` in Java 8 or higher. `sun.misc` is no exported " +
               "by the Java module system and accessing this class will result in a warning in Java 11 and an error in Java 17.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return Applicability.or(
                new UsesType<>(sunPackage + ".BASE64Encoder"),
                new UsesType<>(sunPackage + ".BASE64Decoder")
        );
    }

    public UseJavaUtilBase64(String sunPackage) {
        this.sunPackage = sunPackage;
    }

    @JsonCreator
    public UseJavaUtilBase64() {
        this("sun.misc");
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {

        MethodMatcher base64EncodeMethod = new MethodMatcher(sunPackage + ".CharacterEncoder *(byte[])");
        MethodMatcher base64DecodeBuffer = new MethodMatcher(sunPackage + ".CharacterDecoder decodeBuffer(String)");

        MethodMatcher newBase64Encoder = new MethodMatcher(sunPackage + ".BASE64Encoder <constructor>()");
        MethodMatcher newBase64Decoder = new MethodMatcher(sunPackage + ".BASE64Decoder <constructor>()");

        return new JavaVisitor<ExecutionContext>() {
            final JavaTemplate getEncoderTemplate = JavaTemplate.builder(this::getCursor, "Base64.getEncoder()")
                    .imports("java.util.Base64")
                    .build();
            final JavaTemplate getDecoderTemplate = JavaTemplate.builder(this::getCursor, "Base64.getDecoder()")
                    .imports("java.util.Base64")
                    .build();

            final JavaTemplate encodeToString = JavaTemplate.builder(this::getCursor, "Base64.getEncoder().encodeToString(#{anyArray(byte)})")
                    .imports("java.util.Base64")
                    .build();

            final JavaTemplate decode = JavaTemplate.builder(this::getCursor, "Base64.getDecoder().decode(#{any(String)})")
                    .imports("java.util.Base64")
                    .build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {

                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
                if (base64EncodeMethod.matches(m) &&
                    ("encode".equals(method.getSimpleName()) || "encodeBuffer".equals(method.getSimpleName()))) {
                    m = m.withTemplate(encodeToString, m.getCoordinates().replace(), method.getArguments().get(0));
                    if (method.getSelect() instanceof J.Identifier) {
                        m = m.withSelect(method.getSelect());
                    }
                } else if (base64DecodeBuffer.matches(method)) {
                    m = m.withTemplate(decode, m.getCoordinates().replace(), method.getArguments().get(0));
                    if (method.getSelect() instanceof J.Identifier) {
                        m = m.withSelect(method.getSelect());
                    }
                    // Note: The sun.misc.CharacterDecoder#decodeBuffer throws an IOException, whereas the java
                    // Base64Decoder.decode does not throw a checked exception. If this recipe converts decode, we
                    // may need to remove the catch or completely unwrap a try/catch.
                    doAfterVisit(new UnnecessaryCatch());
                }
                return m;
            }

            @Override
            public J visitJavaSourceFile(JavaSourceFile cu, ExecutionContext ctx) {

                JavaSourceFile c = (JavaSourceFile) super.visitJavaSourceFile(cu, ctx);

                c = (J.CompilationUnit) new ChangeType(sunPackage + ".BASE64Encoder", "java.util.Base64$Encoder", true)
                        .getVisitor().visitNonNull(c, ctx);
                c = (J.CompilationUnit) new ChangeType(sunPackage + ".BASE64Decoder", "java.util.Base64$Decoder", true)
                        .getVisitor().visitNonNull(c, ctx);
                return c;
            }

            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass c = (J.NewClass) super.visitNewClass(newClass, ctx);
                if (newBase64Encoder.matches(c)) {
                    return c.withTemplate(getEncoderTemplate, c.getCoordinates().replace());
                }
                if (newBase64Decoder.matches(newClass)) {
                    return c.withTemplate(getDecoderTemplate, c.getCoordinates().replace());
                }
                return c;
            }
        };
    }
}
