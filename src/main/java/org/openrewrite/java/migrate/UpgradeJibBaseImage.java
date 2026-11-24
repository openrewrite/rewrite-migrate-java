/*
 * Copyright 2026 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;

@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeJibBaseImage extends Recipe {

    private static final XPathMatcher JIB_FROM_IMAGE =
            new XPathMatcher("//plugin[artifactId='jib-maven-plugin']//configuration/from/image");
    private static final String IMAGE_PROPERTIES = "IMAGE_PROPERTIES";
    private static final Pattern PROPERTY_REFERENCE = Pattern.compile("\\$\\{([^}]+)}");
    /// Jib reads a base image from a registry by default, or from the local Docker daemon or a tarball
    /// when prefixed; a tarball is a file path rather than an image reference.
    private static final List<String> IMAGE_SOURCES = Arrays.asList("registry://", "docker://");

    @Option(displayName = "Java version",
            description = "The Java version to upgrade to.",
            example = "25")
    Integer version;

    String displayName = "Upgrade Jib base image Java version";
    String description = "Upgrade the Java version of the base image Jib builds container images on, as configured " +
            "by `from.image` of the `jib-maven-plugin`, or of the `jib` extension in Gradle build scripts written in " +
            "Groovy or the Kotlin DSL. A Maven `jib.from.image` property, or a property the plugin's base image refers " +
            "to, is upgraded as well. Images are upgraded as by `UpgradeDockerImageVersion`, which also moves " +
            "deprecated `openjdk` images to `eclipse-temurin`. Images that are not Java images are left untouched.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> maven = mavenVisitor();
        TreeVisitor<?, ExecutionContext> gradle = gradleVisitor();
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                if (maven.isAcceptable(sourceFile, ctx)) {
                    return maven.visit(tree, ctx);
                }
                return gradle.isAcceptable(sourceFile, ctx) ? gradle.visit(tree, ctx) : tree;
            }
        };
    }

    private MavenIsoVisitor<ExecutionContext> mavenVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Set<String> imageProperties = new MavenIsoVisitor<Set<String>>() {
                    @Override
                    public Xml.Tag visitTag(Xml.Tag tag, Set<String> properties) {
                        if (JIB_FROM_IMAGE.matches(getCursor())) {
                            tag.getValue().map(value -> PROPERTY_REFERENCE.matcher(value.trim()))
                                    .filter(Matcher::matches)
                                    .ifPresent(reference -> properties.add(reference.group(1)));
                        }
                        return super.visitTag(tag, properties);
                    }
                }.reduce(document, new HashSet<>(singletonList("jib.from.image")));
                getCursor().putMessage(IMAGE_PROPERTIES, imageProperties);
                return super.visitDocument(document, ctx);
            }

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (!JIB_FROM_IMAGE.matches(getCursor()) && !isImageProperty(t)) {
                    return t;
                }
                String upgraded = t.getValue().map(value -> upgrade(value.trim(), version)).orElse(null);
                if (upgraded == null) {
                    return t;
                }
                if (isImageProperty(t)) {
                    maybeUpdateModel();
                }
                return t.withValue(upgraded);
            }

            private boolean isImageProperty(Xml.Tag tag) {
                return (isPropertyTag() || isProfilePropertyTag()) &&
                       getCursor().<Set<String>>getNearestMessage(IMAGE_PROPERTIES, emptySet()).contains(tag.getName());
            }
        };
    }

    private TreeVisitor<?, ExecutionContext> gradleVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                J.Assignment a = super.visitAssignment(assignment, ctx);
                List<String> path = new ArrayList<>();
                addNames(a.getVariable(), path);
                return isJibFromImage(getCursor(), path) ? a.withAssignment(upgraded(a.getAssignment())) : a;
            }

            /// The Groovy DSL also sets a property by calling a method of the same name, as in `image 'eclipse-temurin:17'`.
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!"image".equals(m.getSimpleName()) || m.getArguments().size() != 1) {
                    return m;
                }
                List<String> path = new ArrayList<>(singletonList("image"));
                if (m.getSelect() != null) {
                    addNames(m.getSelect(), path);
                }
                if (!isJibFromImage(getCursor().getParentTreeCursor(), path)) {
                    return m;
                }
                Expression image = m.getArguments().get(0);
                Expression upgraded = upgraded(image);
                return upgraded == image ? m : m.withArguments(singletonList(upgraded));
            }
        });
    }

    private Expression upgraded(Expression image) {
        if (!(image instanceof J.Literal) || !(((J.Literal) image).getValue() instanceof String)) {
            return image;
        }
        J.Literal literal = (J.Literal) image;
        String reference = (String) literal.getValue();
        String upgraded = upgrade(reference, version);
        String valueSource = literal.getValueSource();
        if (upgraded == null || valueSource == null || !valueSource.contains(reference)) {
            return image;
        }
        return literal.withValue(upgraded).withValueSource(valueSource.replace(reference, upgraded));
    }

    /// Matches `jib.from.image`, whether written out or nested in `jib { from { image = ... } }` blocks, as well as
    /// `configure<JibExtension>` of the Kotlin DSL; the `jib` extension may itself be nested, as in `subprojects`.
    private static boolean isJibFromImage(Cursor cursor, List<String> path) {
        cursor.getPathAsStream()
                .filter(J.MethodInvocation.class::isInstance)
                .forEach(block -> addNames((J.MethodInvocation) block, path));
        return path.size() >= 3 && "image".equals(path.get(0)) && "from".equals(path.get(1)) && "jib".equals(path.get(2));
    }

    /// Appends the names of `expression` to `path`, innermost first, so that `jib.from` appends `from` and then `jib`.
    private static void addNames(Expression expression, List<String> path) {
        if (expression instanceof J.Identifier) {
            path.add(((J.Identifier) expression).getSimpleName());
        } else if (expression instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) expression;
            path.add(fieldAccess.getSimpleName());
            addNames(fieldAccess.getTarget(), path);
        } else if (expression instanceof J.MethodInvocation) {
            J.MethodInvocation method = (J.MethodInvocation) expression;
            path.add(isConfigureJibExtension(method) ? "jib" : method.getSimpleName());
            if (method.getSelect() != null) {
                addNames(method.getSelect(), path);
            }
        }
    }

    private static boolean isConfigureJibExtension(J.MethodInvocation method) {
        List<Expression> typeParameters = method.getTypeParameters();
        if (!"configure".equals(method.getSimpleName()) || typeParameters == null || typeParameters.size() != 1) {
            return false;
        }
        Expression type = typeParameters.get(0);
        String simpleName = type instanceof J.Identifier ? ((J.Identifier) type).getSimpleName() :
                type instanceof J.FieldAccess ? ((J.FieldAccess) type).getSimpleName() : null;
        return "JibExtension".equals(simpleName);
    }

    static @Nullable String upgrade(String reference, int version) {
        for (String source : IMAGE_SOURCES) {
            if (reference.startsWith(source)) {
                String upgraded = JavaImageReference.upgrade(reference.substring(source.length()), version);
                return upgraded == null ? null : source + upgraded;
            }
        }
        return JavaImageReference.upgrade(reference, version);
    }
}
