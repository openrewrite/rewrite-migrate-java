/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.migrate.lombok.log;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.TreeVisitor;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseJBossLog extends UseLogRecipeTemplate {

    @Override
    public String getDisplayName() {
        return getDisplayName("@JBossLog");
    }

    @Override
    public String getDescription() {
        return getDescription("@JBossLog", "org.jboss.logging.Logger");
    }

    @Option(displayName = "Name of the log field",
            description = FIELD_NAME_DESCRIPTION,
            example = "LOGGER",
            required = false)
    @Nullable
    String fieldName;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new LogVisitor(
                "org.jboss.logging.Logger",
                "org.jboss.logging.Logger getLogger(..)",
                "lombok.extern.jbosslog.JBossLog",
                fieldName);
    }

}
