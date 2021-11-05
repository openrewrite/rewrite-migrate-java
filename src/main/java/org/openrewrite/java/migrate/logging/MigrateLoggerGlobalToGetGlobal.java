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
        return "The preferred way to get the global logger object is via the call `Logger#getGlobal()`.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>("java.util.logging.Logger");
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MigrateLoggerGlobalToGetGlobalVisitor();
    }

    private static class MigrateLoggerGlobalToGetGlobalVisitor extends JavaVisitor<ExecutionContext> {
        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            J j = super.visitFieldAccess(fieldAccess, ctx);
            J.FieldAccess asFieldAccess = (J.FieldAccess) j;

            if (TypeUtils.isOfClassType(asFieldAccess.getTarget().getType(), "java.util.logging.Logger") && asFieldAccess.getSimpleName().equals("global")) {
                j = j.withTemplate(JavaTemplate.builder(this::getCursor, "Logger.getGlobal();").build(),
                        ((J.FieldAccess) j).getCoordinates().replace());
            }

            return j;
        }


    }

}
