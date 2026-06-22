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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EqualsAndHashCode(callSuper = false)
@Value
public class CommentJava24KotlinCap extends Recipe {

    private static final String KOTLIN_GROUP = "org.jetbrains.kotlin";

    // Stable prefix shared by every emitted comment; used to recognise (and remove) a previously added comment even
    // after the named `kotlin-stdlib` version has changed.
    private static final String COMMENT_PREFIX = " Capped at Java 24:";

    private static final Set<String> JAVA_VERSION_PROPERTIES = new HashSet<>(Arrays.asList(
            "maven.compiler.release",
            "maven.compiler.source",
            "maven.compiler.target",
            "java.version"));

    String displayName = "Explain why the Java version was capped at 24 for Kotlin modules";

    String description = "Adds an explanatory comment to Maven `pom.xml` files in modules that were held at Java 24 " +
            "because they compile Kotlin and depend on `kotlin-stdlib` older than 2.3, which cannot target Java 25 " +
            "bytecode. The comment names the `kotlin-stdlib` version found and the next step needed to reach Java 25. " +
            "Self-healing: the comment is added while the module is at Java 24 and removed again once the module " +
            "reaches a higher Java version (for instance after its Kotlin was upgraded to 2.3), so it only ever remains " +
            "on modules that truly stay at Java 24 — whether a Kotlin 1.x cap or a 2.0-2.2 module whose Kotlin upgrade " +
            "could not be applied. Intended to run last, scoped to modules that compile Kotlin.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Nullable
            String commentText;

            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if ("properties".equals(t.getName())) {
                    if (capsJavaAt24(t)) {
                        if (!hasCapComment(t)) {
                            return addCommentAsFirstChild(t, comment());
                        }
                    } else if (hasCapComment(t)) {
                        return removeCapComment(t);
                    }
                }
                return t;
            }

            private boolean capsJavaAt24(Xml.Tag properties) {
                List<? extends Content> content = properties.getContent();
                if (content == null) {
                    return false;
                }
                for (Content c : content) {
                    if (c instanceof Xml.Tag) {
                        Xml.Tag child = (Xml.Tag) c;
                        if (JAVA_VERSION_PROPERTIES.contains(child.getName()) &&
                                "24".equals(child.getValue().map(String::trim).orElse(""))) {
                            return true;
                        }
                    }
                }
                return false;
            }

            private String comment() {
                if (commentText == null) {
                    commentText = COMMENT_PREFIX + " this module compiles Kotlin and depends on " + kotlinStdlibCoordinate() +
                            ", and Kotlin before 2.3 cannot target Java 25 bytecode. " +
                            "Upgrade Kotlin (kotlin-stdlib and the Kotlin compiler) to 2.3 or later, " +
                            "then re-run \"Migrate to Java 25\" to move this module to Java 25. ";
                }
                return commentText;
            }

            private String kotlinStdlibCoordinate() {
                for (List<ResolvedDependency> deps : getResolutionResult().getDependencies().values()) {
                    for (ResolvedDependency dep : deps) {
                        if (KOTLIN_GROUP.equals(dep.getGroupId()) && dep.getArtifactId().startsWith("kotlin-stdlib")) {
                            return dep.getArtifactId() + ' ' + dep.getVersion();
                        }
                    }
                }
                return "kotlin-stdlib (older than 2.3)";
            }

            private boolean hasCapComment(Xml.Tag tag) {
                List<? extends Content> content = tag.getContent();
                if (content == null) {
                    return false;
                }
                for (Content c : content) {
                    if (c instanceof Xml.Comment && ((Xml.Comment) c).getText().startsWith(COMMENT_PREFIX)) {
                        return true;
                    }
                }
                return false;
            }

            private Xml.Tag addCommentAsFirstChild(Xml.Tag tag, String text) {
                List<Content> content = (List<Content>) tag.getContent();
                String prefix = content == null || content.isEmpty() ? "" : content.get(0).getPrefix();
                Xml.Comment comment = new Xml.Comment(Tree.randomId(), prefix, Markers.EMPTY, text);
                return tag.withContent(ListUtils.concat(comment, content));
            }

            @SuppressWarnings("unchecked")
            private Xml.Tag removeCapComment(Xml.Tag tag) {
                return tag.withContent(ListUtils.map((List<Content>) tag.getContent(), c ->
                        c instanceof Xml.Comment && ((Xml.Comment) c).getText().startsWith(COMMENT_PREFIX) ? null : c));
            }
        };
    }
}
