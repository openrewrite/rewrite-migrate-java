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
public class ConvertUtilLog extends ConvertLogRecipe {

    @Override
    public String getDisplayName() {
        return getDisplayName("@Log");
    }

    @Override
    public String getDescription() {
        //language=markdown
        return getDescription("@Log", "java.util.logging.Logger");
    }

    @Option(displayName = "Name of the log field",
            description = FIELD_NAME_DESCRIPTION,
            example = "LOGGER",
            required = false)
    @Nullable
    String fieldName;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new LogVanillaVisitor(fieldName);
    }

    public static class LogVanillaVisitor extends LogVisitor {

        LogVanillaVisitor(String fieldName_) {
            super(fieldName_);
        }

        @Override
        protected void switchImports() {
            maybeAddImport("lombok.extern.java.Log");
            maybeRemoveImport("java.util.logging.Logger");
        }

        @Override
        protected JavaTemplate getLombokTemplate() {
            return getLombokTemplate("Log", "lombok.extern.java.Log");
        }

        @Override
        protected String expectedLoggerPath() {
            return "java.util.logging.Logger";
        }

        @Override
        protected boolean methodPath(String path) {
            return "java.util.logging.Logger.getLogger".equals(path);
        }

        @Override
        protected String getFactoryParameter(String className) {
            return className + ".class.getName()";
        }
    }
}
