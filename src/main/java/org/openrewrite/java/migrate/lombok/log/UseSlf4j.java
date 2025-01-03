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
package org.openrewrite.java.migrate.lombok.log;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseSlf4j extends UseLogRecipeTemplate {

    @Override
    public String getDisplayName() {
        return getDisplayName("@Slf4");
    }

    @Override
    public String getDescription() {
        //language=markdown
        return getDescription("@Slf4", "org.slf4j.Logger");
    }

    @Option(displayName = "Name of the log field",
            description = FIELD_NAME_DESCRIPTION,
            example = "LOGGER",
            required = false)
    @Nullable
    String fieldName;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Slf4jVisitor(fieldName);
    }

    public static class Slf4jVisitor extends LogVisitor {

        Slf4jVisitor(String fieldName_) {
            super(fieldName_);
        }

        @Override
        protected void switchImports() {
            maybeAddImport("lombok.extern.slf4j.Slf4j");
            maybeRemoveImport("org.slf4j.Logger");
            maybeRemoveImport("org.slf4j.LoggerFactory");
        }

        @Override
        protected JavaTemplate getLombokTemplate() {
            return getLombokTemplate("Slf4j", "lombok.extern.slf4j.Slf4j");
        }

        @Override
        protected String expectedLoggerPath() {
            return "org.slf4j.Logger";
        }

        @Override
        protected boolean methodPath(String path) {
            return "org.slf4j.LoggerFactory.getLogger".equals(path);
        }
    }
}
