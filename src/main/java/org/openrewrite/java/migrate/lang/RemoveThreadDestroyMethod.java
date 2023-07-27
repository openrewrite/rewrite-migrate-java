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

package org.openrewrite.java.migrate.lang;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
public class RemoveThreadDestroyMethod extends Recipe {

    public static final String JAVA_LANG_THREAD = "java.lang.Thread";

    @Override
    public String getDisplayName() {
        return "Remove deprecated `Thread.destroy()`";
    }

    @Override
    public String getDescription() {
        return "Remove deprecated invocations of `Thread.destroy()` which have no alternatives needed.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("JDK-8204260");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return Preconditions.check(new UsesType<>(JAVA_LANG_THREAD, false),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                        if (Objects.nonNull(mi.getSelect())
                                && TypeUtils.isAssignableTo(JAVA_LANG_THREAD, mi.getSelect().getType())
                                && mi.getSimpleName().equals("destroy"))
                            return null;
                        return mi;
                    }
                });
    }

}
