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

class UpgradePlexusComponentMetadataForJava25Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradePlexusComponentMetadataForJava25());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "9.6", "9.7.1"})
    void addsOrUpgradesAffectedBytecodeReader(String asm) {
        rewriteRun(pomXml(pom("2.2.0", asm, false), spec -> spec.after(after -> {
            assertThat(after).contains("<artifactId>asm</artifactId>").contains("<version>9.8</version>");
            return after;
        })));
    }

    @Test
    void actualVersionlessDeclarationFromExternalMojoParent87() {
        rewriteRun(pomXml(pom(null, "", true), spec -> spec.after(after -> {
            assertThat(after).contains("<artifactId>mojo-parent</artifactId><version>87</version>")
              .contains("<artifactId>asm</artifactId>").contains("<version>9.8</version>");
            assertThat(after).doesNotContain("<artifactId>plexus-component-metadata</artifactId><version>");
            return after;
        })));
    }

    @ParameterizedTest
    @ValueSource(strings = {"9.8", "9.9", "10.0.0-beta-1"})
    void preservesCompatibleAndNewerAsm(String asm) {
        rewriteRun(pomXml(pom("2.2.0", asm, false)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.1.1", "2.2.1", "2.3.0", "3.0.0"})
    void preservesOtherPluginVersions(String plugin) {
        rewriteRun(pomXml(pom(plugin, "", false)));
    }

    @Test
    void preservesUnknownPluginVersion() {
        rewriteRun(pomXml(pom("${external.plugin.version}", "", false)));
    }

    @Test
    void preservesUnmanagedVersionlessPlugin() {
        rewriteRun(pomXml(pom(null, "", false)));
    }

    @Test
    void preservesOpaqueAsmVersion() {
        rewriteRun(pomXml(pom("2.2.0", "${external.asm.version}", false)));
    }

    @Test
    void upgradesPropertyReferenceLocally() {
        String before = pom("2.2.0", "${asm.version}", false)
          .replace("<build>", "<properties><asm.version>9.6</asm.version></properties><build>");
        rewriteRun(pomXml(before, spec -> spec.after(after -> {
            assertThat(after).contains("<asm.version>9.6</asm.version>").contains("<version>9.8</version>");
            return after;
        })));
    }

    @Test
    void preservesNewerInheritedAsmDependency() {
        inheritedParser("9.9");
    }

    @Test
    void preservesOpaqueInheritedAsmDependency() {
        inheritedParser("${external.asm.version}");
    }

    @Test
    void preservesNewerInheritedPluginVersion() {
        String parent = parentPom(pom("3.0.0", "", false));
        String child = childPom(pom(null, "", false));
        rewriteRun(mavenProject("parent", pomXml(parent), mavenProject("child", pomXml(child))));
    }

    @Test
    void preservesProfileManagedFutureVersion() {
        String future = "<profiles><profile><id>future</id><build><pluginManagement><plugins><plugin>" +
          "<groupId>org.codehaus.plexus</groupId><artifactId>plexus-component-metadata</artifactId><version>3.0.0</version>" +
          "</plugin></plugins></pluginManagement><plugins><plugin><groupId>org.codehaus.plexus</groupId>" +
          "<artifactId>plexus-component-metadata</artifactId></plugin></plugins></build></profile></profiles>";
        String before = pom(null, "", true).replace("</project>", future + "</project>");
        rewriteRun(pomXml(before, spec -> spec.after(after -> {
            assertThat(after).contains("<version>3.0.0</version>").contains("<version>9.8</version>");
            assertThat(after.split("<artifactId>asm</artifactId>", -1)).hasSize(2);
            return after;
        })));
    }

    @Test
    void preservesUnrelatedPlugins() {
        rewriteRun(pomXml(pom("2.2.0", "", false).replace("plexus-component-metadata", "other-plugin")));
    }

    @Test
    void doesNotIntroducePlugin() {
        rewriteRun(pomXml("<project><modelVersion>4.0.0</modelVersion><groupId>peer</groupId><artifactId>plain</artifactId><version>1</version></project>"));
    }

    @Test
    void unsupportedSourceKind() {
        rewriteRun(text("plexus-component-metadata 2.2.0", spec -> spec.path("notes.txt")));
    }

    private void inheritedParser(String version) {
        String parent = parentPom(pom("2.2.0", version, false));
        String child = childPom(pom("2.2.0", "", false));
        rewriteRun(mavenProject("parent", pomXml(parent), mavenProject("child", pomXml(child))));
    }

    private static String parentPom(String pom) {
        return pom.replace("<artifactId>metadata</artifactId>", "<artifactId>parent</artifactId><packaging>pom</packaging>")
          .replace("<build>", "<build><pluginManagement>").replace("</build>", "</pluginManagement></build>");
    }

    private static String childPom(String pom) {
        return pom.replace("<groupId>peer</groupId>", "<parent><groupId>peer</groupId><artifactId>parent</artifactId><version>1</version></parent>");
    }

    private static String pom(String pluginVersion, String asmVersion, boolean externalParent) {
        String parent = externalParent ? "<parent><groupId>org.codehaus.mojo</groupId><artifactId>mojo-parent</artifactId><version>87</version></parent>" : "";
        String version = pluginVersion == null ? "" : "<version>" + pluginVersion + "</version>";
        String dependency = asmVersion.isEmpty() ? "" : "<dependencies><dependency><groupId>org.ow2.asm</groupId><artifactId>asm</artifactId><version>" + asmVersion + "</version></dependency></dependencies>";
        return "<project><modelVersion>4.0.0</modelVersion>" + parent + "<groupId>peer</groupId><artifactId>metadata</artifactId><version>1</version>" +
          "<build><plugins><plugin><groupId>org.codehaus.plexus</groupId><artifactId>plexus-component-metadata</artifactId>" + version +
          dependency + "</plugin></plugins></build></project>";
    }
}
