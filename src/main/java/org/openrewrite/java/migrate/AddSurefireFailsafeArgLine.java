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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Value
@EqualsAndHashCode(callSuper = false)
public class AddSurefireFailsafeArgLine extends Recipe {

    @Option(displayName = "Arg line",
            description = "The arguments to add to the surefire and failsafe plugin `argLine` configuration. " +
                          "Individual arguments are space-separated. Arguments already present in the existing argLine are not duplicated.",
            example = "--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED")
    String argLine;

    String displayName = "Add `argLine` to surefire and failsafe plugins";

    String description = "Adds the specified arguments to the `argLine` configuration of the Maven Surefire and Failsafe plugins, " +
            "merging with any existing argLine value without duplicating arguments. " +
            "The `@{argLine}` [late property reference](https://maven.apache.org/surefire/maven-surefire-plugin/faq.html) is prepended " +
            "so that an agent injected by another plugin during the build, such as the JaCoCo coverage agent from " +
            "`jacoco-maven-plugin:prepare-agent`, is preserved rather than overwritten. It is not added when the existing " +
            "`argLine` already references the `argLine` property.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (!isPluginTag(t)) {
                    return t;
                }
                String artifactId = t.getChildValue("artifactId").orElse("");
                if (!"maven-surefire-plugin".equals(artifactId) && !"maven-failsafe-plugin".equals(artifactId)) {
                    return t;
                }
                String groupId = t.getChildValue("groupId").orElse("org.apache.maven.plugins");
                if (!"org.apache.maven.plugins".equals(groupId)) {
                    return t;
                }

                Optional<Xml.Tag> configTag = t.getChild("configuration");
                if (configTag.isPresent()) {
                    Xml.Tag config = configTag.get();
                    Optional<Xml.Tag> argLineTag = config.getChild("argLine");
                    if (argLineTag.isPresent()) {
                        String existingValue = argLineTag.get().getValue().orElse( "" );
                        String merged = mergeArgLine( existingValue, argLine );
                        if (merged.equals( existingValue )) {
                            return t;
                        }
                        // Update argLine value in-place to preserve formatting
                        Xml.Tag updatedArgLine = argLineTag.get().withValue( merged );
                        Xml.Tag updatedConfig = config.withContent( ListUtils.map( (List<Content>) config.getContent(), c ->
                                c == argLineTag.get() ? updatedArgLine : c ) );
                        return t.withContent( ListUtils.map( (List<Content>) t.getContent(), c ->
                                c == config ? updatedConfig : c ) );
                    }
                    Xml.Tag newArgLine = Xml.Tag.build( "<argLine>" + newArgLineValue() + "</argLine>" );
                    Xml.Tag updatedConfig = config.withContent(
                            ListUtils.concat( newArgLine, (List<Content>) config.getContent() ) );
                    t = t.withContent( ListUtils.map( (List<Content>) t.getContent(), c ->
                            c == config ? updatedConfig : c ) );
                    return autoFormat( t, ctx );
                }
                Xml.Tag newConfig = Xml.Tag.build(
                        "<configuration>\n<argLine>" + newArgLineValue() + "</argLine>\n</configuration>" );
                t = t.withContent( ListUtils.concat( (List<Content>) t.getContent(), newConfig ) );
                return autoFormat( t, ctx );
            }

            // Value for a freshly created argLine: prepend the `@{argLine}` reference unless one is already present.
            private String newArgLineValue() {
                return argLine.contains("{argLine}") ? argLine : ARG_LINE_REFERENCE + " " + argLine;
            }

            private boolean isPluginTag(Xml.Tag tag) {
                return "plugin".equals(tag.getName()) &&
                       getCursor().getParentTreeCursor().getValue() instanceof Xml.Tag &&
                       "plugins".equals(((Xml.Tag) getCursor().getParentTreeCursor().getValue()).getName());
            }
        };
    }

    private static final Pattern ARG_PATTERN = Pattern.compile("(--add-opens\\s+\\S+|-\\S+(?:\\s+(?!-)\\S+)*)");

    /**
     * Surefire's late property reference. Prepended to a newly written or merged {@code argLine} so that an
     * {@code -javaagent} injected into the {@code argLine} property by an earlier plugin in the build (most notably
     * the JaCoCo coverage agent set by {@code jacoco-maven-plugin:prepare-agent}) is preserved instead of overwritten.
     * Unlike {@code ${argLine}}, the {@code @{...}} form is resolved when Surefire executes rather than at POM
     * interpolation, so it picks up the value set during the build and resolves to an empty string when unset.
     */
    static final String ARG_LINE_REFERENCE = "@{argLine}";

    static String mergeArgLine(String existing, String toAdd) {
        // Parse compound args like "--add-opens module/pkg=target" as single units
        Set<String> existingArgs = parseArgs(existing);
        List<String> argsToAdd = new ArrayList<>();
        Matcher m = ARG_PATTERN.matcher(toAdd);
        while (m.find()) {
            String arg = m.group(1).trim();
            // Normalize internal whitespace for comparison
            String normalized = arg.replaceAll("\\s+", " ");
            if (!existingArgs.contains(normalized)) {
                argsToAdd.add(normalized);
                existingArgs.add(normalized);
            }
        }
        // Prepend `@{argLine}` unless the existing value already references the `argLine` property (e.g. `${argLine}`)
        boolean needsArgLineReference = !existing.contains("{argLine}");
        if (argsToAdd.isEmpty() && !needsArgLineReference) {
            return existing;
        }
        StringBuilder result = new StringBuilder();
        if (needsArgLineReference) {
            result.append(ARG_LINE_REFERENCE);
            if (!existing.isEmpty()) {
                result.append(' ');
            }
        }
        result.append(existing);
        for (String arg : argsToAdd) {
            result.append(' ').append(arg);
        }
        return result.toString();
    }

    private static Set<String> parseArgs(String argLine) {
        Set<String> args = new LinkedHashSet<>();
        Matcher m = ARG_PATTERN.matcher(argLine);
        while (m.find()) {
            args.add(m.group(1).replaceAll("\\s+", " ").trim());
        }
        // Also add individual tokens for property references like ${argLine}
        for (String token : argLine.split("\\s+")) {
            if (!token.isEmpty() && !token.startsWith("-")) {
                args.add(token);
            }
        }
        return args;
    }
}
