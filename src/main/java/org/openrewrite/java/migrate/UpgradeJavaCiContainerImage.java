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
import org.openrewrite.yaml.JsonPathMatcher;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeJavaCiContainerImage extends Recipe {
    @Option(displayName = "Java version", description = "The minimum Java version for CI job containers.", example = "25")
    Integer version;

    String displayName = "Upgrade CircleCI OpenJDK job images";
    String description = "Upgrade cimg/openjdk job containers in GitHub Actions and CircleCI to the target Java version, " +
            "preserving newer versions and unrelated service images.";

    private static final Pattern IMAGE = Pattern.compile("cimg/openjdk:(\\d+)\\.\\d+(?:\\.\\d+)?(-[a-z0-9-]+)?");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new YamlIsoVisitor<ExecutionContext>() {
            private final JsonPathMatcher githubContainer = new JsonPathMatcher("$.jobs.*.container");
            private final JsonPathMatcher githubImage = new JsonPathMatcher("$.jobs.*.container.image");
            private final JsonPathMatcher circleImage = new JsonPathMatcher("$.jobs.*.docker[0].image");

            @Override
            public Yaml.Documents visitDocuments(Yaml.Documents documents, ExecutionContext ctx) {
                String path = documents.getSourcePath().toString().replace('\\', '/');
                if (!(path.startsWith(".github/workflows/") && (path.endsWith(".yml") || path.endsWith(".yaml"))) &&
                    !".circleci/config.yml".equals(path)) {
                    return documents;
                }
                return super.visitDocuments(documents, ctx);
            }

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                if (!(e.getValue() instanceof Yaml.Scalar) ||
                    !(githubContainer.matches(getCursor()) || githubImage.matches(getCursor()) || circleImage.matches(getCursor()))) {
                    return e;
                }
                Yaml.Scalar value = (Yaml.Scalar) e.getValue();
                Matcher image = IMAGE.matcher(value.getValue());
                if (!image.matches() || Integer.parseInt(image.group(1)) >= version) {
                    return e;
                }
                return e.withValue(value.withValue("cimg/openjdk:" + version + ".0" +
                        (image.group(2) == null ? "" : image.group(2))));
            }
        };
    }
}
