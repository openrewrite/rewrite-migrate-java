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
public class ConvertCommons extends ConvertLogRecipe {

    @Override
    public String getDisplayName() {
        return getDisplayName("@CommonsLog");
    }

    @Override
    public String getDescription() {
        //language=markdown
        return getDescription("@CommonsLog", "org.apache.commons.logging.Log");
    }

    @Option(displayName = "Name of the log field",
            description = FIELD_NAME_DESCRIPTION,
            example = "LOGGER",
            required = false)
    @Nullable
    String fieldName;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new CommonsLogVisitor(fieldName);
    }

    public static class CommonsLogVisitor extends LogVisitor {

        CommonsLogVisitor(String fieldName_) {
            super(fieldName_);
        }

        @Override
        protected void switchImports() {
            maybeAddImport("lombok.extern.apachecommons.CommonsLog");
            maybeRemoveImport("org.apache.commons.logging.Log");
            maybeRemoveImport("org.apache.commons.logging.LogFactory");
        }

        @Override
        protected JavaTemplate getLombokTemplate() {
            return getLombokTemplate("CommonsLog", "lombok.extern.apachecommons.CommonsLog");
        }

        @Override
        protected String expectedLoggerPath() {
            return "org.apache.commons.logging.Log";
        }

        @Override
        protected boolean methodPath(String path) {
            return "org.apache.commons.logging.LogFactory.getLog".equals(path);
        }
    }
}
