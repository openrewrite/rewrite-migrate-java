package org.openrewrite.java.migrate.apache.commons.lang;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class StringUtilsMethodToJdkTemplate extends Recipe {

    @Option(displayName = "Method",
            description = "The Apache Commons Lang `StringUtils` method to convert to a JDK equivalent.",
            example = "org.apache.commons.lang3.StringUtils isBlank(java.lang.String)")
    String methodPattern;

    @Option(displayName = "JDK template",
            description = "The JDK method to convert to.",
            example = "#{any(String)}.isBlank()")
    String jdkTemplate;

    @Option(displayName = "Imports",
            description = "The imports to add to the compilation unit.",
            example = "java.util.Objects",
            required = false)
    String[] imports;

    @Override
    public String getDisplayName() {
        return "Convert Apache Commons Lang `StringUtils` method to JDK equivalent";
    }

    @Override
    public String getDescription() {
        return "Convert Apache Commons Lang `StringUtils` method to JDK equivalent.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(methodPattern), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
                if (new MethodMatcher(methodPattern).matches(method)) {
                    String[] imports = StringUtilsMethodToJdkTemplate.this.imports == null ?
                            new String[0] : StringUtilsMethodToJdkTemplate.this.imports;
                    for (String imp : imports) {
                        maybeAddImport(imp);
                    }
                    maybeRemoveImport(methodPattern.split(" ")[0]);
                    return JavaTemplate.builder(jdkTemplate)
                            .imports(imports)
                            .build()
                            .apply(updateCursor(mi),
                                    mi.getCoordinates().replace(),
                                    ListUtils.map(mi.getArguments(), a -> a instanceof J.Empty ? null : a).toArray());
                }
                return mi;
            }
        });
    }
}
