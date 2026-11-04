/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.migrate.internal.MavenPluginDependencyCompatibility;

public class UpgradePlexusComponentMetadataForJava25 extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use a Java 25 compatible bytecode reader for Plexus metadata";
    }

    @Override
    public String getDescription() {
        return "Use ASM 9.8 with Plexus component metadata 2.2.0, including declarations managed by an external parent. " +
               "Preserve newer ASM versions, other plugin versions, and unresolved declarations.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return MavenPluginDependencyCompatibility.visitor("org.codehaus.plexus", "plexus-component-metadata", "2.2.0",
                "org.ow2.asm", "asm", "9.8", true);
    }
}
