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
package org.openrewrite.java.migrate.jakarta;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.AddDependency;
import org.openrewrite.maven.search.DoesNotIncludeDependency;

import java.util.Collections;
import java.util.List;

public class MaybeAddJakartaServletApi extends Recipe {

    @Override
    public String getDisplayName() {
        return "Maybe add `jakarta.servlet-api` dependency";
    }

    @Override
    public String getDescription() {
        return "Adds the `jakarta.servlet-api` dependency, unless the project already uses `spring-boot-starter-web`, which transitively includes a compatible implementation under a different GAV.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new DoesNotIncludeDependency("org.springframework.boot", "spring-boot-starter-web", null, null), TreeVisitor.noop());
    }

    @Override
    public List<Recipe> getRecipeList() {
        return Collections.singletonList(new AddDependency(
                "jakarta.servlet",
                "jakarta.servlet-api",
                "6.x",
                null,
                null,
                null,
                "javax.servlet.*",
                null,
                null,
                null,
                null,
                true
        ));
    }
}
