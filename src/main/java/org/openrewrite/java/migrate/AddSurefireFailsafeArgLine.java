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
            "merging with any existing argLine value without duplicating arguments.";

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
                    Xml.Tag newArgLine = Xml.Tag.build( "<argLine>" + argLine + "</argLine>" );
                    Xml.Tag updatedConfig = config.withContent(
                            ListUtils.concat( newArgLine, (List<Content>) config.getContent() ) );
                    t = t.withContent( ListUtils.map( (List<Content>) t.getContent(), c ->
                            c == config ? updatedConfig : c ) );
                    return autoFormat( t, ctx );
                }
                Xml.Tag newConfig = Xml.Tag.build(
                        "<configuration>\n<argLine>" + argLine + "</argLine>\n</configuration>" );
                t = t.withContent( ListUtils.concat( (List<Content>) t.getContent(), newConfig ) );
                return autoFormat( t, ctx );
            }

            private boolean isPluginTag(Xml.Tag tag) {
                return "plugin".equals(tag.getName()) &&
                       getCursor().getParentTreeCursor().getValue() instanceof Xml.Tag &&
                       "plugins".equals(((Xml.Tag) getCursor().getParentTreeCursor().getValue()).getName());
            }
        };
    }

    private static final Pattern ARG_PATTERN = Pattern.compile("(--add-opens\\s+\\S+|-\\S+(?:\\s+(?!-)\\S+)*)");

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
        if (argsToAdd.isEmpty()) {
            return existing;
        }
        StringBuilder result = new StringBuilder(existing);
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
