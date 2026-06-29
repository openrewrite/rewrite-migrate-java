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
package org.openrewrite.java.migrate.jakarta;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

/**
 * Sets the {@code <ejbVersion>} configuration of the {@code maven-ejb-plugin} to {@code 4.0},
 * handling both literal values (e.g. {@code 3.2}) and Maven property references
 * (e.g. {@code ${jee.ejb.api}}) whose resolved value matches the EJB 3.x range.
 * <p>
 * This complements {@code UpgradePluginVersion} (which already handles property-referenced
 * plugin versions via {@code ChangePropertyValue}) and replaces the YAML-only
 * {@code ChangeTagValue} step that could not resolve property expressions.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeMavenEjbPluginConfiguration extends Recipe {


    @Override
    public String getDisplayName() {
        return "Set `maven-ejb-plugin` ejbVersion to 4.0";
    }

    @Override
    public String getDescription() {
        return "Updates the `<ejbVersion>` configuration of `maven-ejb-plugin` to `4.0` when the current value " +
               "(or its resolved Maven property) indicates EJB 3.x. Handles the common pattern where `<ejbVersion>` " +
               "is coupled to the `javax.ejb-api` dependency version via a shared property, decoupling them after migration.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                // isPluginTag() relies on cursor state — must be checked before super.visitTag()
                if (!isPluginTag("org.apache.maven.plugins", "maven-ejb-plugin")) {
                    return super.visitTag(tag, ctx);
                }
                Xml.Tag ejbVersionTag = tag.getChild("configuration")
                        .flatMap(config -> config.getChild("ejbVersion"))
                        .orElse(null);
                if (ejbVersionTag != null) {
                    String rawValue = ejbVersionTag.getValue().orElse(null);
                    if (rawValue != null && !"4.0".equals(rawValue.trim())) {
                        // Resolve property references (e.g. "${jee.ejb.api}") to their actual value.
                        // Note: by the time this recipe runs the property may already have been bumped to 4.x
                        // by an earlier step (ChangeDependency / UpgradeDependencyVersion), so we resolve
                        // against the *original* pom values by checking the rawValue pattern too.
                        String rawTrimmed = rawValue.trim();
                        boolean isPropertyRef = rawTrimmed.startsWith("${");
                        String resolvedValue = isPropertyRef ?
                                getResolutionResult().getPom().getValue(rawTrimmed) : rawTrimmed;
                        boolean currentlyEjb3 = resolvedValue != null && resolvedValue.startsWith("3.");
                        // A property reference means the ejbVersion was coupled to the javax.ejb-api version.
                        // Even if the property was already updated to 4.x by an earlier recipe step, the
                        // ejbVersion tag still holds a property reference that must be replaced with "4.0".
                        if (isPropertyRef || currentlyEjb3) {
                            doAfterVisit(new ChangeTagValueVisitor<>(ejbVersionTag, "4.0"));
                        }
                    }
                }
                return super.visitTag(tag, ctx);
            }
        };
    }
}
