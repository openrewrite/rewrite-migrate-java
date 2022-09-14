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
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;

public class UseJavaUtilBase64 extends Recipe {
    private String sunPackage;

    @Override
    public String getDisplayName() {
        return "Use `java.util.Base64` instead of `sun.misc`";
    }

    @Override
    public String getDescription() {
        return "The `sun.misc` package became is not intended for use beyond Java 9. " +
                "`java.util.Base64` was introduced in Java 8 for general use.";
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
        doNext(new ChangeType(sunPackage + ".BASE64Encoder", "java.util.Base64$Encoder", true));
        doNext(new ChangeType(sunPackage + ".BASE64Decoder", "java.util.Base64$Decoder", true));
    }

    @JsonCreator
    public UseJavaUtilBase64() {
        this("sun.misc");
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        MethodMatcher newBase64Encoder = new MethodMatcher(sunPackage + ".BASE64Encoder <constructor>()");
        MethodMatcher newBase64Decoder = new MethodMatcher(sunPackage + ".BASE64Decoder <constructor>()");

        return new JavaVisitor<ExecutionContext>() {
            final JavaTemplate getEncoderDecoder = JavaTemplate.builder(this::getCursor, "Base64.get#{}()")
                    .imports("java.util.Base64")
                    .build();

            @Override
            public J visitJavaSourceFile(JavaSourceFile cu, ExecutionContext ctx) {
                JavaSourceFile c = cu;
                c = (J.CompilationUnit) new ChangeMethodName(sunPackage + ".BASE64Encoder encode(byte[])", "encodeToString",
                        false, true).getVisitor().visitNonNull(c, ctx);
                c = (J.CompilationUnit) new ChangeMethodName(sunPackage + ".BASE64Decoder decodeBuffer(String)", "decode",
                        false, true).getVisitor().visitNonNull(c, ctx);
                return super.visitJavaSourceFile(c, ctx);
            }

            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                if (newBase64Encoder.matches(newClass)) {
                    return newClass.withTemplate(getEncoderDecoder, newClass.getCoordinates().replace(), "Encoder");
                }
                if (newBase64Decoder.matches(newClass)) {
                    return newClass.withTemplate(getEncoderDecoder, newClass.getCoordinates().replace(), "Decoder");
                }
                return super.visitNewClass(newClass, ctx);
            }
        };
    }
}
