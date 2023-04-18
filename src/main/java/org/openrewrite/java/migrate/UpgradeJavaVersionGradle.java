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
package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class UpgradeJavaVersionGradle extends Recipe {
    @Option(displayName = "Java version",
        description = "The Java version to upgrade to.",
        example = "17")
    @Nullable
    Integer version;

    @Override
    public String getDisplayName() {
        return "Upgrade Java version in gradle project";
    }

    @Override
    public String getDescription() {
        return "Upgrade Java version `java.toolchain.languageVersion` in `build.gradle(.kts)`." +
               " Will not downgrade if the current version is newer than the specified version.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new IsBuildGradle<>();
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GroovyIsoVisitor<ExecutionContext>() {
            MethodMatcher javaLanguageVersionMatcher = new MethodMatcher("org.gradle.jvm.toolchain.JavaLanguageVersion of(int)");

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method,
                                                            ExecutionContext executionContext) {
                method = super.visitMethodInvocation(method, executionContext);
                if (javaLanguageVersionMatcher.matches(method)) {
                    List<Expression> args = method.getArguments();
                    J.Literal versionNumber = (J.Literal) args.get(0);
                    if ( (Integer) versionNumber.getValue() < version) {
                        return method.withArguments(Collections.singletonList(versionNumber.withValue(version)
                            .withValueSource(version.toString())));
                    }
                }
                return method;
            }
        };
    }
}
