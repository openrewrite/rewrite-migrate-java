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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;

@Value
public class NullCheck implements Trait<J.If> {
    Cursor cursor;
    Expression nullCheckedParameter;

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

    /**
     * Calculates few potential cases where the null checked variable gets reassigned and only returns false if these cases DO NOT match.
     * In any other case this returns true as we do not know that particular situation yet -> no harm -> assume it could be altered in the block.
     * @return false only if we are 100% sure the block does not reassigns/changes the null checked variable.
     */
    public boolean couldModifyNullCheckedValue() {
        Statement statement = whenNull();
        if (statement instanceof J.Block) {
            for (Statement s : ((J.Block) statement).getStatements()) {
                if (couldModifyNullCheckedValue(s)) {
                    return true;
                }
            }
            return false;
        }
        return couldModifyNullCheckedValue(statement);
    }

    private boolean couldModifyNullCheckedValue(Statement assignment) {
        Expression nullCheckedParameter = getNullCheckedParameter();
        if (assignment instanceof Expression) {
            return couldModifyNullCheckedValue((Expression) assignment, nullCheckedParameter);
        }
        return false;
    }

    private static <T extends Expression> boolean couldModifyNullCheckedValue(T assigns, T nullChecked) {
        if (assigns instanceof J.Identifier) {
            if (nullChecked instanceof J.Identifier) {
                return ((J.Identifier) assigns).getSimpleName().equals(((J.Identifier) nullChecked).getSimpleName());
            }
            if (nullChecked instanceof J.FieldAccess) {
                return false;
            }
            if (nullChecked instanceof J.MethodInvocation) {
                return ((J.MethodInvocation) nullChecked).getSelect() == null;
            }
        }
        if (assigns instanceof J.FieldAccess) {
            if (nullChecked instanceof J.Identifier) {
                return false;
            }
            if (nullChecked instanceof J.FieldAccess) {
                return couldModifyNullCheckedValue(((J.FieldAccess) assigns).getTarget(), ((J.FieldAccess) nullChecked).getTarget()) &&
                        couldModifyNullCheckedValue(((J.FieldAccess) assigns).getName(), ((J.FieldAccess) nullChecked).getName());
            }
            if (nullChecked instanceof J.MethodInvocation) {
                if (((J.MethodInvocation) nullChecked).getSelect() == null) {
                    return false;
                }
                return couldModifyNullCheckedValue(((J.FieldAccess) assigns).getTarget(), ((J.MethodInvocation) nullChecked).getSelect());
            }
        }
        if (assigns instanceof J.MethodInvocation) {
            if (nullChecked instanceof J.Identifier) {
                if (((J.MethodInvocation) assigns).getSelect() != null) {
                    return false;
                }
            }
            if (nullChecked instanceof J.FieldAccess) {
                if (((J.MethodInvocation) assigns).getSelect() == null) {
                    return false;
                }
                return couldModifyNullCheckedValue(((J.MethodInvocation) assigns).getSelect(), ((J.FieldAccess) nullChecked).getTarget());
            }
            if (nullChecked instanceof J.MethodInvocation) {
                if (((J.MethodInvocation) assigns).getSelect() == null && ((J.MethodInvocation) nullChecked).getSelect() == null) {
                    return true;
                }
                if (((J.MethodInvocation) assigns).getSelect() == null || ((J.MethodInvocation) nullChecked).getSelect() == null) {
                    return false;
                }
                return couldModifyNullCheckedValue(((J.MethodInvocation) assigns).getSelect(), ((J.MethodInvocation) nullChecked).getSelect());
            }
        }
        if (assigns instanceof J.Assignment) {
            return couldModifyNullCheckedValue(((J.Assignment) assigns).getVariable(), nullChecked);
        }

        return true;
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
                        if (J.Literal.isLiteralValue(binary.getLeft(), null)) {
                            return new NullCheck(cursor, binary.getRight());
                        } else if (J.Literal.isLiteralValue(binary.getRight(), null)) {
                            return new NullCheck(cursor, binary.getLeft());
                        }
                    }
                }
            }
            return null;
        }
    }
}
