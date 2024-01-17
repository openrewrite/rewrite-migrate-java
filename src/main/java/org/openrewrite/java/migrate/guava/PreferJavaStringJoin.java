/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.guava;

import java.util.Collections;
import java.util.Set;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;

public class PreferJavaStringJoin extends Recipe {

    static final MethodMatcher JOIN_METHOD_MATCHER = new MethodMatcher("com.google.common.base.Joiner join(..)");

    @Override
    public String getDisplayName() {
        return "Prefer `String#join()` over Guava `Joiner#join()`";
    }

    @Override
    public String getDescription() {
        return "Replaces supported calls to `com.google.common.base.Joiner#join()` with `java.lang.String#join()`.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("guava");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(JOIN_METHOD_MATCHER), new PreferJavaStringJoinVisitor());
    }
}
