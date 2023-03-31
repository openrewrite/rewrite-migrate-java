/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.migrate.apache.commons.io;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

import java.util.*;
import java.util.function.Supplier;

@Value
@EqualsAndHashCode(callSuper = false)
public class ApacheIOUtilsUseExplicitCharset extends Recipe {

    private static final Map<MethodMatcher, String> MATCHER_TEMPLATES = new HashMap<>();
    // `IOUtils.toByteArray(java.lang.String)` is unique It's converted to String#getBytes()
    private static final MethodMatcher STRING_GET_BYTES = new MethodMatcher("org.apache.commons.io.IOUtils toByteArray(java.lang.String)");

    static {
        MATCHER_TEMPLATES.put(new MethodMatcher("org.apache.commons.io.IOUtils copy(java.io.InputStream, java.io.Writer)"), "copy(#{any(java.io.InputStream)}, #{any(java.io.Writer)}, StandardCharsets.#{})");
        MATCHER_TEMPLATES.put(new MethodMatcher("org.apache.commons.io.IOUtils copy(java.io.Reader, java.io.OutputStream)"), "copy(#{any(java.io.InputStream)}, #{any(java.io.OutputStream)}, StandardCharsets.#{})");
        MATCHER_TEMPLATES.put(new MethodMatcher("org.apache.commons.io.IOUtils readLines(java.io.InputStream)"), "readLines(#{any(java.io.InputStream)}, StandardCharsets.#{})");
        MATCHER_TEMPLATES.put(new MethodMatcher("org.apache.commons.io.IOUtils toByteArray(java.io.Reader)"), "toByteArray(#{any(java.io.Reader)}, StandardCharsets.#{})");
        MATCHER_TEMPLATES.put(new MethodMatcher("org.apache.commons.io.IOUtils toCharArray(java.io.InputStream)"), "toCharArray(#{any(java.io.InputStream)}, StandardCharsets.#{})");
        MATCHER_TEMPLATES.put(new MethodMatcher("org.apache.commons.io.IOUtils toInputStream(java.lang.CharSequence)"), "toInputStream(#{any(java.lang.CharSequence)}, StandardCharsets.#{})");
        MATCHER_TEMPLATES.put(new MethodMatcher("org.apache.commons.io.IOUtils toInputStream(java.lang.String)"), "toInputStream(#{any(java.lang.String)}, StandardCharsets.#{})");
        MATCHER_TEMPLATES.put(new MethodMatcher("org.apache.commons.io.IOUtils toString(byte[])"), "toString(#{anyArray(byte)}, StandardCharsets.#{})");
        MATCHER_TEMPLATES.put(new MethodMatcher("org.apache.commons.io.IOUtils toString(java.io.InputStream)"), "toString(#{any(java.io.InputStream)}, StandardCharsets.#{})");
        MATCHER_TEMPLATES.put(new MethodMatcher("org.apache.commons.io.IOUtils toString(java.net.URI)"), "toString(#{any(java.net.URI)}, StandardCharsets.#{})");
        MATCHER_TEMPLATES.put(new MethodMatcher("org.apache.commons.io.IOUtils toString(java.net.URL)"), "toString(#{any(java.net.URL)}, StandardCharsets.#{})");
        MATCHER_TEMPLATES.put(new MethodMatcher("org.apache.commons.io.IOUtils write(byte[], java.io.Writer)"), "write(#{anyArray(byte)}, #{any(java.io.Writer)}, StandardCharsets.#{})");
        MATCHER_TEMPLATES.put(new MethodMatcher("org.apache.commons.io.IOUtils write(char[], java.io.OutputStream)"), "write(#{anyArray(char)}, #{any(java.io.OutputStream)}, StandardCharsets.#{})");
        MATCHER_TEMPLATES.put(new MethodMatcher("org.apache.commons.io.IOUtils write(java.lang.CharSequence, java.io.OutputStream)"), "write(#{any(java.lang.CharSequence)}, #{any(java.io.OutputStream)}, StandardCharsets.#{})");
        MATCHER_TEMPLATES.put(new MethodMatcher("org.apache.commons.io.IOUtils write(java.lang.String, java.io.OutputStream)"), "write(#{any(java.lang.String)}, #{any(java.io.OutputStream)}, StandardCharsets.#{})");
        MATCHER_TEMPLATES.put(new MethodMatcher("org.apache.commons.io.IOUtils write(java.lang.StringBuffer, java.io.OutputStream)"), "write(#{any(java.lang.StringBuffer)}, #{any(java.io.OutputStream)}, StandardCharsets.#{})");
    }

    @Option(displayName = "Default encoding",
            description = "The default encoding to use, must be a standard charset.",
            example = "UTF_8",
            required = false)
    @Nullable
    String encoding;

    @Override
    public String getDisplayName() {
        return "Use IOUtils method that include  their charset encoding";
    }

    @Override
    public String getDescription() {
        return "Use `IOUtils` method invocations that include the charset encoding instead of using the deprecated versions that do not include a charset encoding. (e.g. converts `IOUtils.readLines(inputStream)` to `IOUtils.readLines(inputStream, StandardCharsets.UTF_8)`.";
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("apache", "commons"));
    }

    @Override
    public Validated validate() {
        return super.validate().and(Validated.test("encoding", "Invalid encoding must be a standard Charset",
                encoding, e -> e == null || e.matches("US_ASCII|ISO_8859_1|UTF_8|UTF_16BE|UTF_16LE|UTF_16")));
    }

    @Override
    protected UsesType<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>("org.apache.commons.io.IOUtils", false);
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final Supplier<JavaParser> parserSupplier = () -> JavaParser.fromJavaVersion().classpath("commons-io").build();

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
                if (STRING_GET_BYTES.matches(mi)) {
                    mi = mi.withSelect(method.getArguments().get(0));
                    //noinspection ConstantConditions
                    mi = mi.withMethodType(mi.getMethodType().withName("getBytes"));
                    mi = mi.withTemplate(JavaTemplate.builder(this::getCursor, "#{any(String)}.getBytes(StandardCharsets.#{})}")
                                    .javaParser(parserSupplier)
                                    .imports("java.nio.charset.StandardCharsets").build(),
                            mi.getCoordinates().replaceMethod(), mi.getArguments().get(0), encoding == null ? "UTF_8" : encoding);
                } else {
                    for (Map.Entry<MethodMatcher, String> entry : MATCHER_TEMPLATES.entrySet()) {
                        if (entry.getKey().matches(mi)) {
                            List<Object> args = new ArrayList<>(mi.getArguments());
                            args.add(encoding == null ? "UTF_8" : encoding);
                            mi = mi.withTemplate(JavaTemplate.builder(this::getCursor, entry.getValue())
                                            .javaParser(parserSupplier)
                                            .imports("java.nio.charset.StandardCharsets").build(),
                                    mi.getCoordinates().replaceMethod(), args.toArray());
                        }
                    }
                }
                if (method != mi) {
                    maybeAddImport("java.nio.charset.StandardCharsets");
                }
                return mi;
            }
        };
    }
}
