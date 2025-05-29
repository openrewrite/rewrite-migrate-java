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
package org.openrewrite.java.migrate.lang;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;

@Value
public class NullCheck implements Trait<J.If> {
    Cursor cursor;
    J.Identifier nullCheckedParameter;

    public Statement whenNull() {
        return getTree().getThenPart();
    }

    public @Nullable Statement whenNotNull() {
        J.If.Else anElse = getTree().getElsePart();
        if (anElse == null) {
            return null;
        }
        return anElse.getBody();
    }

    public boolean returns() {
        Statement statement = whenNull();
        if (statement instanceof J.Block) {
            for (Statement s : ((J.Block) statement).getStatements()) {
                if (s instanceof J.Return) {
                    return true;
                }
            }
            return false;
        }
        return statement instanceof J.Return;
    }

    public boolean assigns(J.Identifier variable) {
        Statement statement = whenNull();
        if (statement instanceof J.Block) {
            for (Statement s : ((J.Block) statement).getStatements()) {
                if (s instanceof J.Assignment) {
                    return assigns((J.Assignment) s, variable);
                }
            }
        } else if (statement instanceof J.Assignment) {
            return assigns((J.Assignment) statement, variable);
        }
        return false;
    }

    private static boolean assigns(J.Assignment assignment, J.Identifier variable) {
        return assignment.getVariable() instanceof J.Identifier &&
                // TODO Slight worry here about say A.field vs. B.field, when only comparing the simple name
                ((J.Identifier) assignment.getVariable()).getSimpleName().equals(variable.getSimpleName());
    }

    public static class Matcher extends SimpleTraitMatcher<NullCheck> {

        public static Matcher nullCheck() {
            return new Matcher();
        }

        @Override
        protected @Nullable NullCheck test(Cursor cursor) {
            if (cursor.getValue() instanceof J.If) {
                J.If iff = cursor.getValue();
                if (iff.getIfCondition().getTree() instanceof J.Binary) {
                    J.Binary binary = (J.Binary) iff.getIfCondition().getTree();
                    if (J.Binary.Type.Equal == binary.getOperator()) {
                        if (binary.getLeft() instanceof J.Literal && ((J.Literal) binary.getLeft()).getValue() == null && binary.getRight() instanceof J.Identifier) {
                            return new NullCheck(cursor, (J.Identifier) binary.getRight());
                        } else if (binary.getRight() instanceof J.Literal && ((J.Literal) binary.getRight()).getValue() == null && binary.getLeft() instanceof J.Identifier) {
                            return new NullCheck(cursor, (J.Identifier) binary.getLeft());
                        }
                    }
                }
            }
            return null;
        }
    }
}
