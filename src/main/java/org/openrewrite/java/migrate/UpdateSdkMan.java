/*
 * Copyright 2024 the original author or authors.
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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.binary.Binary;
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.Remote;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateSdkMan extends Recipe {

    @Option(displayName = "Java version",
            description = "The Java version to update to.",
            required = false,
            example = "17")
    @Nullable
    String newVersion;

    @Option(displayName = "Distribution",
            description = "The JVM distribution to use.",
            required = false,
            example = "tem")
    @Nullable
    String newDistribution;

    @Override
    public String getDisplayName() {
        return "Update SDKMan Java version";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Update the SDKMAN JDK version in the `.sdkmanrc` file. Given a major release (e.g., 17), the recipe " +
                "will update the current distribution to the current default SDKMAN version of the specified major " +
                "release. The distribution option can be used to specify a specific JVM distribution. " +
                "Note that these must correspond to valid SDKMAN distributions.";
    }

    @Override
    public Validated<Object> validate(ExecutionContext ctx) {
        return super.validate(ctx)
                .and(Validated.required("newVersion", newVersion)
                        .or(Validated.required("newDistribution", newDistribution)));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> visitor = new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                SourceFile sourceFile = (SourceFile) requireNonNull(tree);
                if (sourceFile instanceof Quark || sourceFile instanceof Remote || sourceFile instanceof Binary) {
                    return sourceFile;
                }
                PlainText plainText = PlainTextParser.convert(sourceFile);

                // Define a regex pattern to extract the version and distribution
                Pattern pattern = Pattern.compile("java=(.*?)([.a-z]*-.*)");
                Matcher matcher = pattern.matcher(plainText.getText());
                if (matcher.find()) {
                    String ver = newVersion == null ? matcher.group(1) : newVersion;
                    String dist = newDistribution == null ? matcher.group(2) : newDistribution;
                    for (String candidate : readSdkmanJavaCandidates()) {
                        if (candidate.startsWith(ver) && candidate.endsWith(dist)) {
                            return plainText.withText(matcher.replaceFirst("java=" + candidate));
                        }
                    }
                }
                return sourceFile;
            }

            private List<String> readSdkmanJavaCandidates() {
                try (InputStream resourceAsStream = UpdateSdkMan.class.getResourceAsStream("/sdkman-java.csv");
                     InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream, StandardCharsets.UTF_8);
                     BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                    return bufferedReader.lines().collect(toList());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        };
        return Preconditions.check(new FindSourceFiles(".sdkmanrc"), visitor);
    }
}
