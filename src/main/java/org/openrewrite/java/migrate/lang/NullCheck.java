package org.openrewrite.java.migrate.lang;

import lombok.RequiredArgsConstructor;
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
        } else return statement instanceof J.Return;
        return false;
    }

    public boolean assigns(J.Identifier variable) {
        Statement statement = whenNull();
        if (statement instanceof J.Block) {
            for (Statement s : ((J.Block) statement).getStatements()) {
                if (s instanceof J.Assignment) {
                    J.Assignment assign = (J.Assignment) s;
                    return assign.getVariable() instanceof J.Identifier && ((J.Identifier) assign.getVariable()).getSimpleName().equals(variable.getSimpleName());
                }
            }
        } else if (statement instanceof J.Assignment) {
            J.Assignment assign = (J.Assignment) statement;
            return assign.getVariable() instanceof J.Identifier && ((J.Identifier) assign.getVariable()).getSimpleName().equals(variable.getSimpleName());
        }
        return false;
    }

    @RequiredArgsConstructor
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
                    if (J.Binary.Type.Equal.equals(binary.getOperator())) {
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
