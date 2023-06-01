/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.logging;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

public class MigrateLoggerGlobalToGetGlobal extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use `Logger#getGlobal()`";
    }

    @Override
    public String getDescription() {
        return "The preferred way to get the global logger object is via the call `Logger#getGlobal()` over direct field access to `java.util.logging.Logger.global`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("java.util.logging.Logger", false), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                J.FieldAccess fa = (J.FieldAccess) super.visitFieldAccess(fieldAccess, ctx);
                if (TypeUtils.isOfClassType(fa.getTarget().getType(), "java.util.logging.Logger") && "global".equals(fa.getSimpleName())) {
                    return JavaTemplate.builder("Logger.getGlobal();")
                            .imports("java.util.logging.Logger")
                            .build()
                            .apply(updateCursor(fa), fa.getCoordinates().replace());
                }
                return fa;
            }
        });
    }
}
