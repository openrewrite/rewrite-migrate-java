package org.openrewrite.java.migrate.maven;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

import static org.openrewrite.xml.AddOrUpdateChild.addOrUpdateChild;
import static org.openrewrite.xml.FilterTagChildrenVisitor.filterChildren;

@Value
@EqualsAndHashCode(callSuper = true)
public class UseMavenCompilerPluginReleaseConfiguration extends Recipe {
    private static final XPathMatcher PLUGINS_MATCHER = new XPathMatcher("/project/build/plugins");

    @Option(
            displayName = "Release Version",
            description = "The new value for the release configuration.",
            example = "11"
    )
    String releaseVersion;

    @Override
    public String getDisplayName() {
        return "Use Maven Compiler Plugin Release Configuration";
    }

    @Override
    public String getDescription() {
        return "Replaces any explicit `source` or `target` configuration (if present) on the maven-compiler-plugin with `release`, and updates the `release` value if needed.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                final Xml.Tag superVisit = super.visitTag(tag, ctx);
                if (!PLUGINS_MATCHER.matches(getCursor())) {
                    return superVisit;
                }
                Optional<Xml.Tag> compilerPluginConfig = superVisit.getChildren()
                        .stream()
                        .filter(plugin -> "plugin".equals(plugin.getName()) &&
                                "org.apache.maven.plugins".equals(plugin.getChildValue("groupId").orElse(null)) &&
                                "maven-compiler-plugin".equals(plugin.getChildValue("artifactId").orElse(null)))
                        .findAny()
                        .flatMap(it -> it.getChild("configuration"));
                if (!compilerPluginConfig.isPresent()) {
                    return superVisit;
                }
                Xml.Tag updated = filterChildren(superVisit, compilerPluginConfig.get(),
                        child -> !(hasName(child, "source") || hasName(child, "target")));
                if (updated == superVisit
                        && !compilerPluginConfig.flatMap(it -> it.getChild("release")).isPresent()) {
                    return superVisit;
                }
                updated = addOrUpdateChild(updated, compilerPluginConfig.get(),
                        Xml.Tag.build("<release>" + releaseVersion + "</release>"), getCursor().getParentOrThrow());
                return updated;
            }

            private boolean hasName(Content child, String name) {
                return child instanceof Xml.Tag && name.equals(((Xml.Tag) child).getName());
            }
        };
    }
}
