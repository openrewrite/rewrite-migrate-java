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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.J;

public class RemoveIllegalSemicolons extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove illegal semicolons";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Remove semicolons after package declarations and imports, no longer accepted in Java 21 as of " +
               "[JDK-8027682](https://bugs.openjdk.org/browse/JDK-8027682).";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesJavaVersion<>(21), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Import visitImport(J.Import _import, ExecutionContext ctx) {
                J.Import im = super.visitImport(_import, ctx);
                if (im.getPrefix().getWhitespace().contains(";")) {
                    im = im.withPrefix(im.getPrefix()
                            .withWhitespace(im.getPrefix().getWhitespace()
                                    .replaceAll("\\s*;(\\R*)\\s*", "$1")));
                }
                return im;
            }
        });
    }
}
