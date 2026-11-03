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

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class AddLombokMapstructBindingMavenDependencyOnlyTest implements RewriteTest {
    private static final String DEPENDENCIES = """
          <dependencies>
              <dependency>
                  <groupId>org.projectlombok</groupId>
                  <artifactId>lombok</artifactId>
                  <version>1.18.36</version>
                  <scope>provided</scope>
              </dependency>
              <dependency>
                  <groupId>org.mapstruct</groupId>
                  <artifactId>mapstruct</artifactId>
                  <version>1.6.3</version>
              </dependency>
          </dependencies>
        """;
    private static final String BINDING = """
              <dependency>
                  <groupId>org.projectlombok</groupId>
                  <artifactId>lombok-mapstruct-binding</artifactId>
                  <version>0.2.0</version>
              </dependency>
        """;
    private static final String PROCESSOR = """
        <path>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok-mapstruct-binding</artifactId>
            <version>0.2.0</version>
        </path>
        """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddLombokMapstructBindingMavenDependencyOnly());
    }

    @Test
    void legacyConfigurationAndIdempotency() {
        rewriteRun(
          spec -> spec.cycles(3).expectedCyclesThatMakeChanges(1),
          pomXml(pom(DEPENDENCIES, ""), withBinding(pom(DEPENDENCIES, "")))
        );
    }

    @Test
    void bindingMissingFromCompilerPaths() {
        String before = pom(DEPENDENCIES, plugin("maven-compiler-plugin", PROCESSOR.replace("lombok-mapstruct-binding", "lombok")));
        rewriteRun(pomXml(before, withBinding(before)));
    }

    @Test
    void singleProcessorWithDefaultPluginGroup() {
        rewriteRun(pomXml(pom(DEPENDENCIES, plugin("maven-compiler-plugin", PROCESSOR))));
    }

    @Test
    void unrelatedPluginDoesNotSuppressDependency() {
        String before = pom(DEPENDENCIES, plugin("maven-surefire-plugin", PROCESSOR));
        rewriteRun(pomXml(before, withBinding(before)));
    }

    @Test
    void unrelatedProcessorGroupDoesNotSuppressDependency() {
        String before = pom(DEPENDENCIES, plugin("maven-compiler-plugin", PROCESSOR.replace("org.projectlombok", "com.example")));
        rewriteRun(pomXml(before, withBinding(before)));
    }

    @Test
    void existingDependencyKeepsScopeAndVersion() {
        String dependencies = DEPENDENCIES.replace("</dependencies>", """
                <dependency>
                    <groupId>org.projectlombok</groupId>
                    <artifactId>lombok-mapstruct-binding</artifactId>
                    <version>${binding.version}</version>
                    <scope>provided</scope>
                </dependency>
            </dependencies>""");
        rewriteRun(pomXml(pom(dependencies, "").replace("<dependencies>", """
            <properties>
                <binding.version>0.2.0</binding.version>
            </properties>
            <dependencies>""")));
    }

    @Test
    void unrelatedModuleDoesNotReceiveDependency() {
        String unrelated = """
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>unrelated</artifactId>
              <version>1</version>
              <dependencies>
                  <dependency>
                      <groupId>org.projectlombok</groupId>
                      <artifactId>lombok</artifactId>
                      <version>1.18.36</version>
                  </dependency>
              </dependencies>
          </project>
          """;
        rewriteRun(
          mavenProject("configured", pomXml(pom(DEPENDENCIES, plugin("maven-compiler-plugin", PROCESSOR)))),
          mavenProject("unrelated", pomXml(unrelated)),
          mavenProject("missing", pomXml(pom(DEPENDENCIES, ""), withBinding(pom(DEPENDENCIES, ""))))
        );
    }

    @Test
    void inheritedCompilerConfiguration() {
        String parent = pom(DEPENDENCIES, plugin("maven-compiler-plugin", PROCESSOR))
          .replace("<artifactId>app</artifactId>", "<artifactId>parent</artifactId>")
          .replace("<version>1</version>", "<version>1</version>\n    <packaging>pom</packaging>");
        rewriteRun(
          mavenProject("parent",
            pomXml(parent),
            mavenProject("child", pomXml("""
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1</version>
                  </parent>
                  <artifactId>child</artifactId>
              </project>
              """))
          )
        );
    }

    @Test
    void managedCompilerConfiguration() {
        String compiler = plugin("maven-compiler-plugin", PROCESSOR)
          .replace("<plugins>", "<pluginManagement>\n        <plugins>")
          .replace("</plugins>", "</plugins>\n    </pluginManagement>");
        rewriteRun(pomXml(pom(DEPENDENCIES, compiler)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"annotationProcessorPaths", "configuration"})
    void explicitOverrideContainingBinding(String overriddenElement) {
        rewriteRun(pomXml(pom(DEPENDENCIES, plugin("maven-compiler-plugin", PROCESSOR)
          .replace("<" + overriddenElement + ">", "<" + overriddenElement + " combine.self=\"override\">"))));
    }

    @Test
    void managedProcessorInheritedByChildWithoutProcessorPaths() {
        String managedCompiler = plugin("maven-compiler-plugin", PROCESSOR)
          .replace("<plugins>", "<pluginManagement>\n        <plugins>")
          .replace("</plugins>", "</plugins>\n    </pluginManagement>");
        String parent = pom("", managedCompiler)
          .replace("<artifactId>app</artifactId>", "<artifactId>parent</artifactId>")
          .replace("<version>1</version>", "<version>1</version>\n    <packaging>pom</packaging>");
        String child = pom(DEPENDENCIES, """
              <build>
                  <plugins>
                      <plugin>
                          <artifactId>maven-compiler-plugin</artifactId>
                          <configuration>
                              <source>17</source>
                          </configuration>
                      </plugin>
                  </plugins>
              </build>
            """)
          .replace("<groupId>com.example</groupId>", """
              <parent>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
              </parent>""");
        rewriteRun(
          mavenProject("parent",
            pomXml(parent),
            mavenProject("child", pomXml(child))
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"annotationProcessorPaths", "configuration"})
    void managedProcessorOverriddenInChild(String overriddenElement) {
        String managedCompiler = plugin("maven-compiler-plugin", PROCESSOR)
          .replace("<plugins>", "<pluginManagement>\n        <plugins>")
          .replace("</plugins>", "</plugins>\n    </pluginManagement>");
        String parent = pom("", managedCompiler)
          .replace("<artifactId>app</artifactId>", "<artifactId>parent</artifactId>")
          .replace("<version>1</version>", "<version>1</version>\n    <packaging>pom</packaging>");
        String processors = PROCESSOR.replace("lombok-mapstruct-binding", "lombok").replace("0.2.0", "1.18.36") +
          PROCESSOR.replace("org.projectlombok", "org.mapstruct").replace("lombok-mapstruct-binding", "mapstruct-processor").replace("0.2.0", "1.6.3");
        String child = pom(DEPENDENCIES, plugin("maven-compiler-plugin", processors)
          .replace("<" + overriddenElement + ">", "<" + overriddenElement + " combine.self=\"override\">"))
          .replace("<groupId>com.example</groupId>", """
              <parent>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
              </parent>""");
        rewriteRun(
          mavenProject("parent",
            pomXml(parent),
            mavenProject("child", pomXml(child, withBinding(child)))
          )
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void nonInheritedCompilerProcessorIsActiveInDeclaringPom(boolean managed) {
        String compiler = plugin("maven-compiler-plugin", PROCESSOR)
          .replace("<version>3.13.0</version>", "<version>3.13.0</version>\n                <inherited>false</inherited>");
        if (managed) {
            compiler = compiler.replace("<plugins>", "<pluginManagement>\n        <plugins>")
              .replace("</plugins>", "</plugins>\n    </pluginManagement>");
        }
        rewriteRun(pomXml(pom(DEPENDENCIES, compiler)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void compilerProcessorNotInheritedByChild(boolean managed) {
        String compiler = plugin("maven-compiler-plugin", PROCESSOR)
          .replace("<version>3.13.0</version>", "<version>3.13.0</version>\n                <inherited>false</inherited>");
        if (managed) {
            compiler = compiler.replace("<plugins>", "<pluginManagement>\n        <plugins>")
              .replace("</plugins>", "</plugins>\n    </pluginManagement>");
        }
        String parent = pom("", compiler)
          .replace("<artifactId>app</artifactId>", "<artifactId>parent</artifactId>")
          .replace("<version>1</version>", "<version>1</version>\n    <packaging>pom</packaging>");
        String child = pom(DEPENDENCIES, "")
          .replace("<groupId>com.example</groupId>", """
              <parent>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
              </parent>""");
        rewriteRun(
          mavenProject("parent",
            pomXml(parent),
            mavenProject("child", pomXml(child, withBinding(child)))
          )
        );
    }

    @Test
    void processorCoordinatesUseProperties() {
        String before = pom(DEPENDENCIES, plugin("maven-compiler-plugin", PROCESSOR
          .replace("org.projectlombok", "${binding.group}")
          .replace("lombok-mapstruct-binding", "${binding.artifact}")))
          .replace("<dependencies>", """
              <properties>
                  <binding.group>org.projectlombok</binding.group>
                  <binding.artifact>lombok-mapstruct-binding</binding.artifact>
              </properties>
              <dependencies>""");
        rewriteRun(pomXml(before));
    }

    private static String pom(String dependencies, String build) {
        return """
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>app</artifactId>
              <version>1</version>
          %s%s</project>
          """.formatted(dependencies.indent(2), build);
    }

    private static String plugin(String artifact, String paths) {
        return """
              <build>
                  <plugins>
                      <plugin>
                          <artifactId>%s</artifactId>
                          <version>3.13.0</version>
                          <configuration>
                              <annotationProcessorPaths>
          %s                    </annotationProcessorPaths>
                          </configuration>
                      </plugin>
                  </plugins>
              </build>
          """.formatted(artifact, paths.indent(24));
    }

    private static String withBinding(String before) {
        return before.replace("    </dependencies>", BINDING.indent(2) + "    </dependencies>");
    }
}
