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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateSdkMan extends Recipe {
    private static final String SDKMAN_CONFIG_FILE_PATTERN = ".sdkmanrc";

    /**
     * Retrieve from https://api.sdkman.io/2/candidates/java/linuxx64/versions/all
     * TODO: Investifate the best way to get the latest versions from SDKMAN
     */
    private static final List<String> SDKMAN_CANDIDATES_LIST = Arrays.asList(
            "11.0.14.1-jbr,11.0.15-trava,11.0.25-albba,11.0.25-amzn,11.0.25-kona,11.0.25-librca,11.0.25-ms,11.0.25-sapmchn,11.0.25-sem,11.0.25-tem,11.0.25-zulu,11.0.25.fx-librca,11.0.25.fx-zulu,17.0.12-graal,17.0.12-jbr,17.0.12-oracle,17.0.13-albba,17.0.13-amzn,17.0.13-kona,17.0.13-librca,17.0.13-ms,17.0.13-sapmchn,17.0.13-sem,17.0.13-tem,17.0.13-zulu,17.0.13.crac-librca,17.0.13.crac-zulu,17.0.13.fx-librca,17.0.13.fx-zulu,17.0.9-graalce,21.0.2-graalce,21.0.2-open,21.0.5-amzn,21.0.5-graal,21.0.5-jbr,21.0.5-kona,21.0.5-librca,21.0.5-ms,21.0.5-oracle,21.0.5-sapmchn,21.0.5-sem,21.0.5-tem,21.0.5-zulu,21.0.5.crac-librca,21.0.5.crac-zulu,21.0.5.fx-librca,21.0.5.fx-zulu,22.0.2-oracle,22.1.0.1.r11-gln,22.1.0.1.r17-gln,22.3.5.r11-nik,22.3.5.r17-mandrel,22.3.5.r17-nik,23-open,23.0.1-amzn,23.0.1-graal,23.0.1-graalce,23.0.1-librca,23.0.1-oracle,23.0.1-sapmchn,23.0.1-tem,23.0.1-zulu,23.0.1.crac-zulu,23.0.1.fx-librca,23.0.1.fx-zulu,23.0.6.fx-nik,23.0.6.r17-mandrel,23.0.6.r17-nik,23.1.5.fx-nik,23.1.5.r21-mandrel,23.1.5.r21-nik,24.0.2.r22-mandrel,24.1.1.r23-mandrel,24.1.1.r23-nik,24.ea.22-graal,24.ea.23-graal,24.ea.24-graal,24.ea.26-open,24.ea.27-open,24.ea.28-open,24.ea.29-open,25.ea.1-graal,25.ea.1-open,25.ea.2-open,25.ea.3-open,6.0.119-zulu,7.0.352-zulu,8.0.282-trava,8.0.432-albba,8.0.432-amzn,8.0.432-kona,8.0.432-librca,8.0.432-sem,8.0.432-tem,8.0.432-zulu,8.0.432.fx-librca,8.0.432.fx-zulu"
                    .split(","));

    @Option(displayName = "Java version", description = "The Java version to update to.", example = "17")
    @Nullable
    String newVersion;

    @Option(displayName = "Distribution", description = "The JVM distribution to use.", example = "tem")
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
                    for (String candidate : SDKMAN_CANDIDATES_LIST) {
                        if (candidate.startsWith(ver) && candidate.endsWith(dist)) {
                            return plainText.withText(matcher.replaceFirst("java=" + candidate));
                        }
                    }
                }
                return sourceFile;
            }
        };
        return Preconditions.check(new FindSourceFiles(SDKMAN_CONFIG_FILE_PATTERN), visitor);
    }
}
