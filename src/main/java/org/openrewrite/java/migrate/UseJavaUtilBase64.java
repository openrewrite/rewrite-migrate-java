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
import lombok.Getter;
import org.openrewrite.*;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.template.Semantics;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Markup;
import org.openrewrite.staticanalysis.UnnecessaryCatch;

import java.util.Base64;

public class UseJavaUtilBase64 extends Recipe {
    private final String sunPackage;

    @Getter
    @Option(displayName = "Use Mime Coder", description = "Use `Base64.getMimeEncoder()/getMimeDecoder()` instead of `Base64.getEncoder()/getDecoder()`.", required = false, example = "false")
    boolean useMimeCoder;

    @Override
    public String getDisplayName() {
        return "Prefer `java.util.Base64` instead of `sun.misc`";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Prefer `java.util.Base64` instead of using `sun.misc` in Java 8 or higher. `sun.misc` is not exported " +
               "by the Java module system and accessing this class will result in a warning in Java 11 and an error in Java 17.";
    }

    public UseJavaUtilBase64(String sunPackage, boolean useMimeCoder) {
        this.sunPackage = sunPackage;
        this.useMimeCoder = useMimeCoder;
    }

    @JsonCreator
    public UseJavaUtilBase64() {
        this("sun.misc", false);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> check = Preconditions.or(
                new UsesType<>(sunPackage + ".BASE64Encoder", false),
                new UsesType<>(sunPackage + ".BASE64Decoder", false)
        );
        MethodMatcher base64EncodeMethod = new MethodMatcher(sunPackage + ".CharacterEncoder *(byte[])");
        MethodMatcher base64DecodeBuffer = new MethodMatcher(sunPackage + ".CharacterDecoder decodeBuffer(String)");

        MethodMatcher newBase64Encoder = new MethodMatcher(sunPackage + ".BASE64Encoder <constructor>()");
        MethodMatcher newBase64Decoder = new MethodMatcher(sunPackage + ".BASE64Decoder <constructor>()");

        return Preconditions.check(check, new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                if (alreadyUsingIncompatibleBase64(cu)) {
                    return Markup.warn(cu, new IllegalStateException(
                            "Already using a class named Base64 other than java.util.Base64. Manual intervention required."));
                }
                J.CompilationUnit c = (J.CompilationUnit) super.visitCompilationUnit(cu, ctx);

                c = (J.CompilationUnit) new ChangeType(sunPackage + ".BASE64Encoder", "java.util.Base64$Encoder", true)
                        .getVisitor().visitNonNull(c, ctx);
                return (J.CompilationUnit) new ChangeType(sunPackage + ".BASE64Decoder", "java.util.Base64$Decoder", true)
                        .getVisitor().visitNonNull(c, ctx);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (base64EncodeMethod.matches(m) &&
                    ("encode".equals(method.getSimpleName()) || "encodeBuffer".equals(method.getSimpleName()))) {
                    m = JavaTemplate.builder(useMimeCoder ? "Base64.getMimeEncoder().encodeToString(#{anyArray(byte)})" : "Base64.getEncoder().encodeToString(#{anyArray(byte)})")
                            .imports("java.util.Base64")
                            .build()
                            .apply(updateCursor(m), m.getCoordinates().replace(), method.getArguments().get(0));
                    if (method.getSelect() instanceof J.Identifier) {
                        m = m.withSelect(method.getSelect());
                    }
                } else if (base64DecodeBuffer.matches(method)) {
                    m = JavaTemplate.builder(useMimeCoder ? "Base64.getMimeDecoder().decode(#{any(String)})" : "Base64.getDecoder().decode(#{any(String)})")
                            .imports("java.util.Base64")
                            .build()
                            .apply(updateCursor(m), m.getCoordinates().replace(), method.getArguments().get(0));
                    if (method.getSelect() instanceof J.Identifier) {
                        m = m.withSelect(method.getSelect());
                    }
                    // Note: The sun.misc.CharacterDecoder#decodeBuffer throws an IOException, whereas the java
                    // Base64Decoder.decode does not throw a checked exception. If this recipe converts decode, we
                    // may need to remove the catch or completely unwrap a try/catch.
                    doAfterVisit(new UnnecessaryCatch(false, false).getVisitor());
                }
                return m;
            }

            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass c = (J.NewClass) super.visitNewClass(newClass, ctx);
                if (newBase64Encoder.matches(c)) {
                    // noinspection Convert2MethodRef
                    JavaTemplate.Builder encoderTemplate = useMimeCoder ?
                            Semantics.expression(this, "getMimeEncoder", () -> Base64.getMimeEncoder()) :
                            Semantics.expression(this, "getEncoder", () -> Base64.getEncoder());
                    return encoderTemplate
                            .build()
                            .apply(updateCursor(c), c.getCoordinates().replace());

                }
                if (newBase64Decoder.matches(c)) {
                    return JavaTemplate.builder(useMimeCoder ? "Base64.getMimeDecoder()" : "Base64.getDecoder()")
                            .contextSensitive()
                            .imports("java.util.Base64")
                            .build()
                            .apply(updateCursor(c), c.getCoordinates().replace());
                }
                return c;
            }
        });
    }

    private boolean alreadyUsingIncompatibleBase64(JavaSourceFile cu) {
        return cu.getClasses().stream().anyMatch(it -> "Base64".equals(it.getSimpleName())) ||
               cu.getTypesInUse().getTypesInUse().stream()
                       .filter(org.openrewrite.java.tree.JavaType.FullyQualified.class::isInstance)
                       .map(JavaType.FullyQualified.class::cast)
                       .map(JavaType.FullyQualified::getFullyQualifiedName)
                       .filter(it -> !"java.util.Base64".equals(it))
                       .anyMatch(it -> it.endsWith(".Base64"));
    }
}
