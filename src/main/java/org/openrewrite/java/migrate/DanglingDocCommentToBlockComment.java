/*
 * Copyright 2025 the original author or authors.
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

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Comment;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TextComment;

public class DanglingDocCommentToBlockComment extends Recipe {

    @Getter
    final String displayName = "Turn dangling documentation comments into block comments";

    @Getter
    final String description = "A documentation comment that does not precede a declaration documents nothing, and " +
            "since Java 22 `-Xlint:dangling-doc-comments` warns about it, which fails any build using `-Werror`. " +
            "Changing `/**` to `/*` keeps the text and silences the warning. A documentation comment that is attached " +
            "to a declaration is left alone.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
                // A leading comment is conventionally the license header, and javac does not flag it.
                if (loc == Space.Location.COMPILATION_UNIT_PREFIX) {
                    return space;
                }
                return space.withComments(ListUtils.map(space.getComments(), DanglingDocCommentToBlockComment::toBlockComment));
            }
        };
    }

    /**
     * An attached documentation comment parses to {@link org.openrewrite.java.tree.Javadoc.DocComment}, so a
     * {@link TextComment} whose text opens with the extra asterisk of `/**` is one that documents nothing.
     */
    private static Comment toBlockComment(Comment comment) {
        if (comment instanceof TextComment) {
            TextComment text = (TextComment) comment;
            if (text.isMultiline() && text.getText().startsWith("*")) {
                return text.withText(text.getText().substring(1));
            }
        }
        return comment;
    }
}
