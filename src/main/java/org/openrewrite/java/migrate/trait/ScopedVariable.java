package org.openrewrite.java.migrate.trait;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.java.tree.J;
import org.openrewrite.trait.SimpleTraitMatcher;
import org.openrewrite.trait.Trait;

@Value
public class ScopedVariable implements Trait<J.VariableDeclarations.NamedVariable> {
    Cursor cursor;
    Cursor scope;
    J.Identifier identifier;

    @RequiredArgsConstructor
    public static class Matcher extends SimpleTraitMatcher<ScopedVariable> {

        @Override
        protected @Nullable ScopedVariable test(Cursor cursor) {
            if (cursor.getValue() instanceof J.VariableDeclarations.NamedVariable) {
                J.VariableDeclarations.NamedVariable variable = cursor.getValue();
                return new ScopedVariable(cursor, variable.getDeclaringScope(cursor), variable.getName());
            }
            return null;
        }
    }
}
