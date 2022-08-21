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
package org.openrewrite.java.migrate.javax;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.AddDependencyVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.RemoveDependency;
import org.openrewrite.xml.tree.Xml;

import java.time.Duration;
import java.util.Optional;

public class ReplaceJavaxJaxbWithJakarta extends Recipe {

    @Override
    public String getDisplayName() {
        return "Replace `javax.xml.bind:jaxb-api with `jakarta.xml.bind:jakarta.xml.bind-api`";
    }

    @Override
    public String getDescription() {
        return "This recipe will replace the legacy `javax-api` artifact with the Jakarta EE equivalent.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (isDependencyTag("javax.xml.bind", "jaxb-api")) {
                    Optional<Xml.Tag> scopeTag = tag.getChild("scope");
                    String scope = scopeTag.isPresent() && scopeTag.get().getValue().isPresent() ? scopeTag.get().getValue().get() : null;
                    doAfterVisit(new RemoveDependency("javax.xml.bind", "jaxb-api", scope));
                    doAfterVisit(new AddDependencyVisitor("jakarta.xml.bind", "jakarta.xml.bind-api", "2.3.2", null, scope, null, null, null, null, null));
                }
                return super.visitTag(tag, ctx);
            }
        };
    }
}
