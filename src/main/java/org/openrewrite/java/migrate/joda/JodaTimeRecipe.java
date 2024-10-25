package org.openrewrite.java.migrate.joda;

import org.openrewrite.ExecutionContext;
import org.openrewrite.NlsRewrite;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.java.tree.J.VariableDeclarations.NamedVariable;

import java.util.HashSet;
import java.util.Set;

public class JodaTimeRecipe extends ScanningRecipe<Set<NamedVariable>> {
    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Prefer java time over joda time";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Prefer the Java standard library over third-party usage of Joda Time.";
    }

    @Override
    public Set<NamedVariable> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    @Override
    public JodaTimeScanner getScanner(Set<NamedVariable> acc) {
        return new JodaTimeScanner(acc);
    }

    @Override
    public JodaTimeVisitor getVisitor(Set<NamedVariable> acc) {
        return new JodaTimeVisitor(acc);
    }
}
