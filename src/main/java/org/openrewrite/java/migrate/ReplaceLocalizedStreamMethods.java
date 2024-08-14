package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceLocalizedStreamMethods extends Recipe {

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "java.lang.Runtime getLocalizedInputStream(java.io.InputStream)")
    String localizedInputStreamMethodMatcher = "java.lang.Runtime getLocalizedInputStream(java.io.InputStream)";

    @Option(displayName = "Method pattern to replace",
            description = "The method pattern to match and replace.",
            example = "java.lang.Runtime getLocalizedOutputStream(java.io.OutputStream)")
    String localizedOutputStreamMethodMatcher = "java.lang.Runtime getLocalizedOutputStream(java.io.OutputStream)";

    @Override
    public String getDisplayName() {
        return "Replace getLocalizedInputStream with direct assignment";
    }

    @Override
    public String getDescription() {
        return "Replaces `InputStream new = rt.getLocalizedInputStream(in);` with `InputStream new = in;`.";
    }

    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            private final MethodMatcher LocalizedInputStreamMethod = new MethodMatcher(localizedInputStreamMethodMatcher, false);
            private final MethodMatcher localizedOutputStreamMethod = new MethodMatcher(localizedOutputStreamMethodMatcher, false);

            @Override
            public J visitMethodInvocation(J.MethodInvocation mi, ExecutionContext ctx) {
                if (LocalizedInputStreamMethod.matches(mi) || localizedOutputStreamMethod.matches(mi)) {
                    Expression parameter = mi.getArguments().get(0);
                    parameter = parameter.withPrefix(Space.SINGLE_SPACE);
                    return parameter;
                }
                return super.visitMethodInvocation(mi, ctx);
            }
        };
    }
}
