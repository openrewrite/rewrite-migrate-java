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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.SourceSpecs.text;

class UpgradeImpsortForJava25Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeImpsortForJava25());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "3.28.0", "3.27.1"})
    void addsOrUpgradesAffectedParser(String parserVersion) {
        rewriteRun(pomXml(pom("1.13.0", parserVersion), spec -> spec.after(after -> {
            assertThat(after).contains("<artifactId>javaparser-core</artifactId>").contains("<version>3.28.2</version>");
            return after;
        })));
    }

    @Test
    void pluginManagement() {
        String before = pom("1.13.0", "").replace("<build>", "<build><pluginManagement>").replace("</build>", "</pluginManagement></build>");
        rewriteRun(pomXml(before, spec -> spec.after(after -> {
            assertThat(after).contains("<artifactId>javaparser-core</artifactId>").contains("<version>3.28.2</version>");
            return after;
        })));
    }

    @Test
    void resolvesPluginVersionProperty() {
        String before = pom("${impsort.version}", "").replace("<build>", "<properties><impsort.version>1.13.0</impsort.version></properties><build>");
        rewriteRun(pomXml(before, spec -> spec.after(after -> {
            assertThat(after).contains("<version>${impsort.version}</version>").contains("<version>3.28.2</version>");
            return after;
        })));
    }

    @Test
    void changesOldParserPropertyReferenceLocally() {
        String before = pom("1.13.0", "${parser.version}").replace("<build>", "<properties><parser.version>3.28.0</parser.version></properties><build>");
        rewriteRun(pomXml(before, spec -> spec.after(after -> {
            assertThat(after).contains("<parser.version>3.28.0</parser.version>").contains("<version>3.28.2</version>");
            return after;
        })));
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.28.2", "3.29.0", "4.0.0-beta-1"})
    void preservesCompatibleAndNewerParser(String parserVersion) {
        rewriteRun(pomXml(pom("1.13.0", parserVersion)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.12.0", "1.13.1", "1.14.0", "2.0.0"})
    void leavesOtherImpsortVersionsAlone(String pluginVersion) {
        rewriteRun(pomXml(pom(pluginVersion, "")));
    }

    @Test
    void leavesUnrelatedPluginsAlone() {
        rewriteRun(pomXml(pom("1.13.0", "").replace("impsort-maven-plugin", "formatter-maven-plugin")));
    }

    @Test
    void preservesUnresolvedPluginVersion() {
        rewriteRun(pomXml(pom("${unknown.version}", "")));
    }

    @Test
    void preservesUnresolvedParserVersion() {
        rewriteRun(pomXml(pom("1.13.0", "${unknown.version}")));
    }

    @Test
    void respectsProfileVersionProperty() {
        String before = pom("${impsort.version}", "").replace("<build>", "<profiles><profile><id>future</id><properties><impsort.version>1.14.0</impsort.version></properties><build>")
          .replace("</build>", "</build></profile></profiles>")
          .replace("<profiles>", "<properties><impsort.version>1.13.0</impsort.version></properties><profiles>");
        rewriteRun(pomXml(before));
    }

    @Test
    void unsupportedSourceKind() {
        rewriteRun(text("impsort-maven-plugin 1.13.0", spec -> spec.path("notes.txt")));
    }

    @Test
    void preservesUnrelatedPluginDependencies() {
        String before = pom("1.13.0", "").replace("</plugin>", "<dependencies><dependency><groupId>org.ow2.asm</groupId><artifactId>asm</artifactId><version>9.8</version></dependency></dependencies></plugin>");
        rewriteRun(pomXml(before, spec -> spec.after(after -> {
            assertThat(after).contains("<artifactId>asm</artifactId>").contains("<version>9.8</version>")
              .contains("<artifactId>javaparser-core</artifactId>").contains("<version>3.28.2</version>");
            return after;
        })));
    }

    @Test
    void profileParserPropertyDoesNotChangeOtherDeclarations() {
        String before = pom("1.13.0", "${parser.version}")
          .replace("<build>", "<profiles><profile><id>legacy</id><properties><parser.version>3.28.0</parser.version></properties><build>")
          .replace("</build>", "</build></profile></profiles>")
          .replace("<profiles>", "<properties><parser.version>3.29.0</parser.version></properties><profiles>");
        rewriteRun(pomXml(before, spec -> spec.after(after -> {
            assertThat(after).contains("<parser.version>3.29.0</parser.version>").contains("<parser.version>3.28.0</parser.version>")
              .contains("<version>3.28.2</version>");
            return after;
        })));
    }

    @Test
    void preservesNewerInheritedPluginDependency() {
        String parent = pom("1.13.0", "3.29.0").replace("<artifactId>impsort</artifactId>", "<artifactId>parent</artifactId><packaging>pom</packaging>")
          .replace("<build>", "<build><pluginManagement>").replace("</build>", "</pluginManagement></build>");
        String child = pom("1.13.0", "").replace("<groupId>peer</groupId>", "<parent><groupId>peer</groupId><artifactId>parent</artifactId><version>1</version></parent>");
        rewriteRun(mavenProject("parent", pomXml(parent), mavenProject("child", pomXml(child))));
    }

    @Test
    void preservesUnresolvedInheritedParserDependency() {
        String parent = pom("1.13.0", "${external.parser.version}").replace("<artifactId>impsort</artifactId>", "<artifactId>parent</artifactId><packaging>pom</packaging>")
          .replace("<build>", "<build><pluginManagement>").replace("</build>", "</pluginManagement></build>");
        String child = pom("1.13.0", "").replace("<groupId>peer</groupId>", "<parent><groupId>peer</groupId><artifactId>parent</artifactId><version>1</version></parent>");
        rewriteRun(mavenProject("parent", pomXml(parent), mavenProject("child", pomXml(child))));
    }

    private static String pom(String pluginVersion, String parserVersion) {
        String dependency = parserVersion.isEmpty() ? "" : "<dependencies><dependency><groupId>com.github.javaparser</groupId>" +
          "<artifactId>javaparser-core</artifactId><version>" + parserVersion + "</version></dependency></dependencies>";
        return "<project><modelVersion>4.0.0</modelVersion><groupId>peer</groupId><artifactId>impsort</artifactId><version>1</version>" +
          "<build><plugins><plugin><groupId>net.revelc.code</groupId><artifactId>impsort-maven-plugin</artifactId><version>" + pluginVersion +
          "</version>" + dependency + "</plugin></plugins></build></project>";
    }
}
