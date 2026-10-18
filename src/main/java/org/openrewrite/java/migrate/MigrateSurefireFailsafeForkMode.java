/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class MigrateSurefireFailsafeForkMode extends Recipe {

    @Getter
    final String displayName = "Migrate Surefire/Failsafe `forkMode` to `forkCount`/`reuseForks`";

    @Getter
    final String description = "The Surefire and Failsafe `forkMode` parameter (`once`, `always`, `never`, `perthread`) " +
            "was deprecated in Surefire 2.14 in favor of `forkCount`/`reuseForks`, and Surefire 3 silently ignores it " +
            "entirely, changing fork behaviour out from under any build that still sets it. This recipe migrates it to " +
            "the exact modern equivalent, per the [Surefire fork options documentation](https://maven.apache.org/surefire/maven-surefire-plugin/examples/fork-options-and-parallel-execution.html): " +
            "`never` to `forkCount=0`; `once` to `forkCount=1`, `reuseForks=true`; `always` to `forkCount=1`, " +
            "`reuseForks=false`; and `perthread` (which requires a paired `threadCount`) to `forkCount=<threadCount>`, " +
            "`reuseForks=false`. A `perthread` configuration with no `threadCount` present has nothing to migrate to " +
            "and is left untouched, as is any `forkMode` alongside an existing `forkCount` or `reuseForks`, since " +
            "Surefire 3 already ignores `forkMode` in that case.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (!isSurefireOrFailsafePlugin(t)) {
                    return t;
                }

                Optional<Xml.Tag> configTag = t.getChild("configuration");
                if (!configTag.isPresent()) {
                    return t;
                }
                Xml.Tag config = configTag.get();
                Optional<Xml.Tag> forkModeTag = config.getChild("forkMode");
                if (!forkModeTag.isPresent()) {
                    return t;
                }
                Xml.Tag forkMode = forkModeTag.get();

                // Surefire 3 ignores `forkMode` outright once `forkCount`/`reuseForks` are present;
                // only the now-dead `forkMode` tag needs removing.
                if (config.getChild("forkCount").isPresent() || config.getChild("reuseForks").isPresent()) {
                    return withConfig(t, config, removeTag(config, forkMode), ctx);
                }

                List<Xml.Tag> replacement = replacementFor(forkMode.getValue().orElse("").trim(), config);
                if (replacement == null) {
                    return t;
                }

                List<Content> configContents = new ArrayList<>();
                //noinspection unchecked
                for (Content c : (List<Content>) config.getContent()) {
                    if (c == forkMode) {
                        configContents.addAll(replacement);
                    } else {
                        configContents.add(c);
                    }
                }
                return withConfig(t, config, config.withContent(configContents), ctx);
            }

            private List<Xml.Tag> replacementFor(String mode, Xml.Tag config) {
                switch (mode.toLowerCase(Locale.ROOT)) {
                    case "never":
                        return singletonList(Xml.Tag.build("<forkCount>0</forkCount>"));
                    case "once":
                        return asList(Xml.Tag.build("<forkCount>1</forkCount>"), Xml.Tag.build("<reuseForks>true</reuseForks>"));
                    case "always":
                        return asList(Xml.Tag.build("<forkCount>1</forkCount>"), Xml.Tag.build("<reuseForks>false</reuseForks>"));
                    case "perthread": {
                        Optional<Xml.Tag> threadCount = config.getChild("threadCount");
                        if (!threadCount.isPresent()) {
                            // No `threadCount` to carry over as `forkCount`; nothing safe to migrate to.
                            return null;
                        }
                        String count = threadCount.get().getValue().orElse(null);
                        if (count == null) {
                            return null;
                        }
                        return asList(Xml.Tag.build("<forkCount>" + count + "</forkCount>"), Xml.Tag.build("<reuseForks>false</reuseForks>"));
                    }
                    default:
                        // Not one of the four historical values; nothing safe to assume.
                        return null;
                }
            }

            private Xml.Tag removeTag(Xml.Tag config, Xml.Tag toRemove) {
                return config.withContent(ListUtils.filter((List<Content>) config.getContent(), c -> c != toRemove));
            }

            private Xml.Tag withConfig(Xml.Tag plugin, Xml.Tag oldConfig, Xml.Tag newConfig, ExecutionContext ctx) {
                //noinspection unchecked
                Xml.Tag updated = plugin.withContent(ListUtils.map((List<Content>) plugin.getContent(),
                        c -> c == oldConfig ? newConfig : c));
                return autoFormat(updated, ctx);
            }

            private boolean isSurefireOrFailsafePlugin(Xml.Tag tag) {
                if (!"plugin".equals(tag.getName())) {
                    return false;
                }
                String artifactId = tag.getChildValue("artifactId").orElse("");
                if (!"maven-surefire-plugin".equals(artifactId) && !"maven-failsafe-plugin".equals(artifactId)) {
                    return false;
                }
                String groupId = tag.getChildValue("groupId").orElse("org.apache.maven.plugins");
                return "org.apache.maven.plugins".equals(groupId);
            }
        };
    }
}
