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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;

import java.util.concurrent.atomic.AtomicBoolean;

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
        if (statement instanceof J.Block || statement instanceof Expression) {
            return couldModifyNullCheckedValue(statement, nullCheckedParameter);
        }
        // Cautious by default
        return true;
    }
    private static boolean couldModifyNullCheckedValue(J expression, Expression nullChecked) {
        if (nullChecked instanceof J.FieldAccess && couldModifyNullCheckedValue(expression, ((J.FieldAccess) nullChecked).getTarget())) {
            return true;
        }
        if (nullChecked instanceof J.MethodInvocation &&
                ((J.MethodInvocation) nullChecked).getSelect() != null &&
                couldModifyNullCheckedValue(expression, ((J.MethodInvocation) nullChecked).getSelect())) {
            return true;
        }
        return new JavaIsoVisitor<AtomicBoolean>() {

            private final boolean isCertainlyImmutable = nullChecked.getType() != null && JavaType.Primitive.fromClassName(nullChecked.getType().toString()) != null;

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean atomicBoolean) {
                J.Identifier id = super.visitIdentifier(identifier, atomicBoolean);
                if (!isCertainlyImmutable && SemanticallyEqual.areEqual(id, nullChecked)) {
                    atomicBoolean.set(true);
                }
                return id;
            }
            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, AtomicBoolean atomicBoolean) {
                J.Assignment as = super.visitAssignment(assignment, atomicBoolean);
                if (SemanticallyEqual.areEqual(as.getVariable(), nullChecked)) {
                    atomicBoolean.set(true);
                }
                return as;
            }
            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, AtomicBoolean atomicBoolean) {
                J.FieldAccess fa = super.visitFieldAccess(fieldAccess, atomicBoolean);
                if (SemanticallyEqual.areEqual(fa, nullChecked) ||
                        SemanticallyEqual.areEqual(fa.getTarget(), nullChecked)) {
                    atomicBoolean.set(true);
                }
                return fa;
            }
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean atomicBoolean) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, atomicBoolean);
                if (SemanticallyEqual.areEqual(mi, nullChecked) ||
                        ((mi.getSelect() != null) && SemanticallyEqual.areEqual(mi.getSelect(), nullChecked))) {
                    atomicBoolean.set(true);
                }
                return mi;
            }
        }.reduce(expression, new AtomicBoolean(false)).get();
    }

    public static class Matcher extends SimpleTraitMatcher<NullCheck> {

        public static Matcher nullCheck() {
            return new Matcher();
        }

        @Override
        protected @Nullable NullCheck test(Cursor cursor) {
            if (cursor.getValue() instanceof J.If) {
                J.If if_ = cursor.getValue();
                if (if_.getIfCondition().getTree() instanceof J.Binary) {
                    J.Binary binary = (J.Binary) if_.getIfCondition().getTree();
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
