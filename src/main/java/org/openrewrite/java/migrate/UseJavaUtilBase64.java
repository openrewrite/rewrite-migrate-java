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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.template.Semantics;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markup;
import org.openrewrite.staticanalysis.UnnecessaryCatch;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

public class UseJavaUtilBase64 extends Recipe {
    private final String sunPackage;

    @Getter
    @Option(displayName = "Use Mime Coder", description = "Use `Base64.getMimeEncoder()/getMimeDecoder()` instead of `Base64.getEncoder()/getDecoder()`.", required = false, example = "false")
    boolean useMimeCoder;

    @Getter
    final String displayName = "Prefer `java.util.Base64` instead of `sun.misc`";

    @Getter
    final String description = "Prefer `java.util.Base64` instead of using `sun.misc` in Java 8 or higher. `sun.misc` is not exported " +
            "by the Java module system and accessing this class will result in a warning in Java 11 and an error in Java 17.";

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

        MethodMatcher anyEncoderMethod = new MethodMatcher(sunPackage + ".CharacterEncoder *(..)", true);
        MethodMatcher anyDecoderMethod = new MethodMatcher(sunPackage + ".CharacterDecoder *(..)", true);

        MethodMatcher newBase64Encoder = new MethodMatcher(sunPackage + ".BASE64Encoder <constructor>()");
        MethodMatcher newBase64Decoder = new MethodMatcher(sunPackage + ".BASE64Decoder <constructor>()");

        return Preconditions.check(check, new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                if (alreadyUsingIncompatibleBase64(cu)) {
                    return Markup.warn(cu, new IllegalStateException(
                            "Already using a class named Base64 other than java.util.Base64. Manual intervention required."));
                }
                if (usesLegacyTypeUntranslatably(cu)) {
                    // A legacy coder type appears somewhere this recipe cannot retype or rewrite. Migrating
                    // only part of the file would either not compile or leave a sun.misc reference behind, so
                    // leave the whole file for a human.
                    return cu;
                }
                J.CompilationUnit c = (J.CompilationUnit) super.visitCompilationUnit(cu, ctx);

                c = (J.CompilationUnit) new ChangeType(sunPackage + ".BASE64Encoder", "java.util.Base64$Encoder", true)
                        .getVisitor().visitNonNull(c, ctx);
                return (J.CompilationUnit) new ChangeType(sunPackage + ".BASE64Decoder", "java.util.Base64$Decoder", true)
                        .getVisitor().visitNonNull(c, ctx);
            }

            /**
             * True when a legacy coder type appears somewhere this recipe cannot translate. The recipe only
             * retypes {@code BASE64Encoder} and {@code BASE64Decoder} themselves and only rewrites the
             * overloads that have a {@code java.util.Base64} equivalent, so the file is left alone when:
             * <ul>
             * <li>an expression's static type is {@code CharacterEncoder} or {@code CharacterDecoder},
             * for example a receiver declared as the legacy supertype, which {@link ChangeType} never
             * retypes;</li>
             * <li>a class extends one of the legacy coder types, or instantiates one with an anonymous
             * class body, since {@code Base64.Encoder} and {@code Base64.Decoder} have no accessible
             * constructor;</li>
             * <li>a legacy coder method with no {@code java.util.Base64} equivalent is called, or a
             * supported overload is called on a receiver that is not typed as {@code BASE64Encoder} or
             * {@code BASE64Decoder}, for example a subclass declared in another compilation unit;</li>
             * <li>a legacy coder method or constructor is used as a method reference, which is never
             * rewritten;</li>
             * <li>a value is passed to a method that keeps a {@code CharacterEncoder} or
             * {@code CharacterDecoder} parameter.</li>
             * </ul>
             */
            private boolean usesLegacyTypeUntranslatably(J.CompilationUnit cu) {
                AtomicBoolean found = new AtomicBoolean(false);
                new JavaIsoVisitor<AtomicBoolean>() {
                    @Override
                    public J.Import visitImport(J.Import anImport, AtomicBoolean found) {
                        // Imports are rewritten by ChangeType and are not a place a value can flow through
                        return anImport;
                    }

                    @Override
                    public Expression visitExpression(Expression expression, AtomicBoolean found) {
                        if (isLegacySupertype(expression.getType())) {
                            found.set(true);
                        }
                        return super.visitExpression(expression, found);
                    }

                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, AtomicBoolean found) {
                        if (classDecl.getExtends() != null && isLegacyCoderType(classDecl.getExtends().getType())) {
                            found.set(true);
                        }
                        return super.visitClassDeclaration(classDecl, found);
                    }

                    @Override
                    public J.NewClass visitNewClass(J.NewClass newClass, AtomicBoolean found) {
                        // The type of an anonymous class creation is the anonymous class itself, so
                        // walk the supertype chain to find `new BASE64Encoder() { ... }` as well
                        if (newClass.getBody() != null &&
                            (TypeUtils.isAssignableTo(sunPackage + ".CharacterEncoder", newClass.getType()) ||
                             TypeUtils.isAssignableTo(sunPackage + ".CharacterDecoder", newClass.getType())) ||
                            takesLegacySupertypeParameter(newClass.getMethodType())) {
                            found.set(true);
                        }
                        return super.visitNewClass(newClass, found);
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                        if (isLegacyCoderMethod(method.getMethodType())) {
                            Expression select = method.getSelect();
                            if (!(encodeToString(method) || base64DecodeBuffer.matches(method)) ||
                                select == null || !isRetypedCoderClass(select.getType())) {
                                found.set(true);
                            }
                        } else if (takesLegacySupertypeParameter(method.getMethodType())) {
                            found.set(true);
                        }
                        return super.visitMethodInvocation(method, found);
                    }

                    @Override
                    public J.MemberReference visitMemberReference(J.MemberReference memberRef, AtomicBoolean found) {
                        JavaType.Method methodType = memberRef.getMethodType();
                        if (isLegacyCoderMethod(methodType) ||
                            methodType == null && isLegacyCoderType(memberRef.getContaining().getType()) ||
                            methodType != null && (isRetypedCoderClass(methodType.getDeclaringType()) ||
                                                   takesLegacySupertypeParameter(methodType))) {
                            found.set(true);
                        }
                        return super.visitMemberReference(memberRef, found);
                    }
                }.visit(cu, found);
                return found.get();
            }

            private boolean isLegacyCoderMethod(JavaType.@Nullable Method methodType) {
                return methodType != null && (anyEncoderMethod.matches(methodType) || anyDecoderMethod.matches(methodType));
            }

            private boolean isRetypedCoderClass(@Nullable JavaType type) {
                return TypeUtils.isOfClassType(type, sunPackage + ".BASE64Encoder") ||
                       TypeUtils.isOfClassType(type, sunPackage + ".BASE64Decoder");
            }

            private boolean isLegacySupertype(@Nullable JavaType type) {
                if (type instanceof JavaType.Array) {
                    return isLegacySupertype(((JavaType.Array) type).getElemType());
                }
                return TypeUtils.isOfClassType(type, sunPackage + ".CharacterEncoder") ||
                       TypeUtils.isOfClassType(type, sunPackage + ".CharacterDecoder");
            }

            private boolean isLegacyCoderType(@Nullable JavaType type) {
                if (type instanceof JavaType.Array) {
                    return isLegacyCoderType(((JavaType.Array) type).getElemType());
                }
                return isRetypedCoderClass(type) || isLegacySupertype(type);
            }

            private boolean takesLegacySupertypeParameter(JavaType.@Nullable Method methodType) {
                if (methodType != null) {
                    for (JavaType parameterType : methodType.getParameterTypes()) {
                        if (isLegacySupertype(parameterType)) {
                            return true;
                        }
                    }
                }
                return false;
            }

            private boolean encodeToString(J.MethodInvocation method) {
                return base64EncodeMethod.matches(method) &&
                       ("encode".equals(method.getSimpleName()) || "encodeBuffer".equals(method.getSimpleName()));
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (encodeToString(m)) {
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
