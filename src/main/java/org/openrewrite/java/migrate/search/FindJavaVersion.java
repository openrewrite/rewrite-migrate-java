/*
 * Copyright 2023 the original author or authors.
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
@EqualsAndHashCode(callSuper = false)
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
