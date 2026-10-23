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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MigrateSurefireFailsafeForkMode extends Recipe {

    private static final XPathMatcher PLUGIN_CONFIGURATION = new XPathMatcher("//plugins/plugin/configuration");
    private static final XPathMatcher EXECUTION_CONFIGURATION = new XPathMatcher("//plugins/plugin/executions/execution/configuration");

    @Getter
    final String displayName = "Migrate Surefire/Failsafe `forkMode` to `forkCount`/`reuseForks`";

    @Getter
    final String description = "The `forkMode` parameter of the Maven Surefire and Failsafe plugins was deprecated in 2.14 " +
            "and removed in 3.0.0-M8, after which it is ignored, changing how tests are forked in any build that still sets it. " +
            "This recipe replaces it with the equivalent `forkCount` and `reuseForks`, per the " +
            "[Surefire fork options documentation](https://maven.apache.org/surefire/maven-surefire-plugin/examples/fork-options-and-parallel-execution.html): " +
            "`never` to `forkCount=0`; `once` to `forkCount=1`, `reuseForks=true`; `always` to `forkCount=1`, " +
            "`reuseForks=false`; and `perthread` to `forkCount=<threadCount>`, leaving `reuseForks` untouched. " +
            "The documented mapping sets `reuseForks=false` for `perthread` too, but Surefire's own " +
            "`convertDeprecatedForkMode`, from 2.14 up to 3.0.0-M7, left it at its default of `true` - what " +
            "most real builds actually ran. Following the documented mapping instead would silently make " +
            "those builds' tests noticeably slower (a fresh JVM per class) on upgrade, the exact kind of " +
            "change this recipe exists to avoid. " +
            "The legacy `none` and `pertest` values are treated as `never` and `always`. " +
            "As in Surefire before 3.0.0-M8, the values implied by `forkMode` override any `forkCount` or `reuseForks` already present. " +
            "A `perthread` without a `threadCount`, an unrecognized value, or a plugin with an explicit version below " +
            "2.14 (which predates `forkCount`/`reuseForks` and would silently ignore them while the `forkMode` it " +
            "understood is removed), is left untouched.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                Optional<Xml.Tag> forkMode = t.getChild("forkMode");
                if (!forkMode.isPresent() || !isSurefireOrFailsafeConfiguration()) {
                    return t;
                }
                Xml.Tag migrated = migrate(t, forkMode.get());
                return migrated == t ? t : autoFormat(migrated, ctx, getCursor().getParentOrThrow());
            }

            private boolean isSurefireOrFailsafeConfiguration() {
                if (PLUGIN_CONFIGURATION.matches(getCursor())) {
                    return isSurefireOrFailsafePlugin(getCursor().getParentTreeCursor());
                }
                return EXECUTION_CONFIGURATION.matches(getCursor()) &&
                       isSurefireOrFailsafePlugin(getCursor().getParentTreeCursor().getParentTreeCursor().getParentTreeCursor());
            }
        };
    }

    private static boolean isSurefireOrFailsafePlugin(Cursor cursor) {
        Xml.Tag plugin = cursor.getValue();
        String artifactId = plugin.getChildValue("artifactId").orElse("");
        return ("maven-surefire-plugin".equals(artifactId) || "maven-failsafe-plugin".equals(artifactId)) &&
               "org.apache.maven.plugins".equals(plugin.getChildValue("groupId").orElse("org.apache.maven.plugins")) &&
               isVersionSafeToMigrate(plugin);
    }

    /**
     * `forkCount`/`reuseForks` were introduced in Surefire 2.14; an explicit older version does not
     * recognize them and would silently ignore the replacement while the `forkMode` it did understand
     * is removed. An absent `<version>` is treated as safe: either nothing pins the plugin that old in
     * practice today, or - when this recipe runs as part of a composite that bumps the version first,
     * such as `UpgradePluginsForJava25` - the version is already current by the time this visitor runs.
     * A version that can't be confidently parsed (a property placeholder, for instance) is treated as
     * unsafe, since there is nothing to verify it against.
     */
    private static boolean isVersionSafeToMigrate(Xml.Tag plugin) {
        Optional<String> version = plugin.getChildValue("version");
        if (!version.isPresent()) {
            return true;
        }
        LatestRelease comparator = new LatestRelease(null);
        return comparator.isValid(null, version.get()) && comparator.compare(null, version.get(), "2.14") >= 0;
    }

    private static Xml.Tag migrate(Xml.Tag config, Xml.Tag forkMode) {
        Xml.Tag c;
        switch (forkMode.getValue().orElse("").trim().toLowerCase(Locale.ROOT)) {
            case "never":
            case "none":
                c = set(config, forkMode, "forkCount", "0");
                break;
            case "once":
                c = setIfAbsent(setIfAbsent(config, forkMode, "forkCount", "1"), forkMode, "reuseForks", "true");
                break;
            case "always":
            case "pertest":
                c = set(set(config, forkMode, "forkCount", "1"), forkMode, "reuseForks", "false");
                break;
            case "perthread":
                Optional<String> threadCount = config.getChildValue("threadCount");
                if (!threadCount.isPresent()) {
                    return config;
                }
                // Unlike the other modes, Surefire's own pre-3.0.0-M8 conversion left `reuseForks` at its
                // default of `true` for `perthread`, so it is left untouched here too, not set to `false`.
                c = set(config, forkMode, "forkCount", threadCount.get());
                break;
            default:
                return config;
        }
        return c.withContent(ListUtils.filter(c.getContent(), content -> content != forkMode));
    }

    private static Xml.Tag set(Xml.Tag config, Xml.Tag forkMode, String name, String value) {
        return config.getChild(name).isPresent() ?
                config.withChildValue(name, value) :
                insertBefore(config, forkMode, name, value);
    }

    private static Xml.Tag setIfAbsent(Xml.Tag config, Xml.Tag forkMode, String name, String value) {
        return config.getChild(name).isPresent() ? config : insertBefore(config, forkMode, name, value);
    }

    @SuppressWarnings("unchecked")
    private static Xml.Tag insertBefore(Xml.Tag config, Xml.Tag forkMode, String name, String value) {
        List<Content> content = (List<Content>) config.getContent();
        Xml.Tag tag = Xml.Tag.build("<" + name + ">" + value + "</" + name + ">");
        return config.withContent(ListUtils.insert(content, tag, content.indexOf(forkMode)));
    }
}
