package org.openrewrite.java.migrate.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.migrate.table.JavaVersionTable;
import org.openrewrite.java.tree.J;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindJavaVersion extends Recipe {

    transient JavaVersionTable table = new JavaVersionTable(this);
    private static Set<JavaVersion> seen = new HashSet<>();
    @Override
    public String getDisplayName() {
        return "Find Java versions in use";
    }

    @Override
    public String getDescription() {
        return "Finds Java versions in use.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                Optional<JavaVersion> maybeJv = cu.getMarkers().findFirst(JavaVersion.class);
                if(!maybeJv.isPresent()) {
                    return cu;
                }
                JavaVersion jv = maybeJv.get();
                if(!seen.add(jv)) {
                    return cu;
                }
                table.insertRow(ctx, new JavaVersionTable.Row(jv.getSourceCompatibility(), jv.getTargetCompatibility()));
                return cu;
            }
        };
    }
}
