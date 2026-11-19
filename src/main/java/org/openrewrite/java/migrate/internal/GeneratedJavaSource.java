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
package org.openrewrite.java.migrate.internal;

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Generated;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public final class GeneratedJavaSource {
    private static final Pattern ANTLR_HEADER = Pattern.compile(
            "\\s*(?:\\$ANTLR\\s+\\d|Generated (?:from .+ )?by ANTLR(?:\\s|$))");

    private GeneratedJavaSource() {
    }

    public static boolean isGeneratedSource(J.CompilationUnit cu) {
        if (cu.getMarkers().findFirst(Generated.class).isPresent()) {
            return true;
        }
        for (Path segment : cu.getSourcePath()) {
            String name = segment.toString();
            if ("src-gen".equals(name) || "xtend-gen".equals(name) || "generated-sources".equals(name) || "generated-test-sources".equals(name)) {
                return true;
            }
        }
        if (!cu.getClasses().isEmpty() && cu.getClasses().stream()
                .allMatch(type -> isGeneratedDeclaration(type.getLeadingAnnotations()))) {
            return true;
        }
        if (hasAntlrHeader(cu.getPrefix())) {
            return true;
        }
        // The parser can attach the file header to the first declaration's prefix.
        J.Package pkg = cu.getPackageDeclaration();
        if (pkg != null) {
            return hasAntlrHeader(pkg.getPrefix());
        }
        if (!cu.getImports().isEmpty()) {
            return hasAntlrHeader(cu.getImports().get(0).getPrefix());
        }
        return !cu.getClasses().isEmpty() && hasAntlrHeader(cu.getClasses().get(0).getPrefix());
    }

    private static boolean hasAntlrHeader(Space prefix) {
        return prefix.getComments().stream().anyMatch(comment ->
                comment instanceof TextComment && ANTLR_HEADER.matcher(((TextComment) comment).getText()).lookingAt());
    }

    public static boolean isGeneratedDeclaration(List<J.Annotation> annotations) {
        return annotations.stream().anyMatch(annotation ->
                TypeUtils.isOfClassType(annotation.getType(), "javax.annotation.processing.Generated") ||
                TypeUtils.isOfClassType(annotation.getType(), "javax.annotation.Generated") ||
                TypeUtils.isOfClassType(annotation.getType(), "jakarta.annotation.Generated") ||
                TypeUtils.isOfClassType(annotation.getType(), "lombok.Generated"));
    }
}
