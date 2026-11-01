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
package org.openrewrite.java.migrate.javax;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.SourceSpecs.dir;

class AddApisForJaxwsCodegenTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddApisForJaxwsCodegen());
    }

    private String pom(String group, String version, String extra) {
        return """
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>example</groupId>
              <artifactId>client</artifactId>
              <version>1</version>
              <build><plugins><plugin>
                  <groupId>%s</groupId>
                  <artifactId>jaxws-maven-plugin</artifactId>
                  <version>%s</version>
                  <executions><execution><goals><goal>wsimport</goal></goals></execution></executions>
                  %s
              </plugin></plugins></build>
          </project>
          """.formatted(group, version, extra);
    }

    @Test
    void noOriginalJavaSources() {
        String before = pom("org.codehaus.mojo", "2.6", "");
        rewriteRun(pomXml(before, withApis(before)));
    }

    private String withApis(String before) {
        return before.replace("    <build>", """
          <dependencies>
              <dependency>
                  <groupId>jakarta.jws</groupId>
                  <artifactId>jakarta.jws-api</artifactId>
                  <version>2.1.0</version>
              </dependency>
              <dependency>
                  <groupId>jakarta.xml.bind</groupId>
                  <artifactId>jakarta.xml.bind-api</artifactId>
                  <version>2.3.3</version>
              </dependency>
              <dependency>
                  <groupId>jakarta.xml.ws</groupId>
                  <artifactId>jakarta.xml.ws-api</artifactId>
                  <version>2.3.3</version>
              </dependency>
          </dependencies>
          """.indent(4) + "    <build>");
    }

    @Test
    void siblingDoesNotReceiveDependencies() {
        String client = pom("org.codehaus.mojo", "2.6", "");
        rewriteRun(
          pomXml(client, withApis(client), s -> s.path("client/pom.xml")),
          pomXml(client.replace("wsimport", "wsgen"), s -> s.path("other/pom.xml"))
        );
    }

    @Test
    void migrationUpgradesGeneratorAndAddsApis() {
        String before = pom("org.codehaus.mojo", "2.4.1", "");
        rewriteRun(spec -> spec.recipeFromResources("org.openrewrite.java.migrate.javax.MigrateJaxBWSPlugin"),
          pomXml(before, withApis(before.replace("2.4.1", "2.6"))));
    }

    private String codecentric(String version, String extra) {
        return pom("de.codecentric", version, extra)
          .replace("jaxws-maven-plugin", "cxf-spring-boot-starter-maven-plugin")
          .replace("wsimport", "generate");
    }

    @Test
    void migrationUpgradesCodecentricAndAddsApis() {
        String before = codecentric("1.0.7.RELEASE", "");
        rewriteRun(spec -> spec.recipeFromResources("org.openrewrite.java.migrate.javax.MigrateJaxBWSPlugin"),
          pomXml(before, withApis(before.replace("1.0.7.RELEASE", "2.3.0.RELEASE"))));
    }

    @Test
    void codecentricDefaultAndExplicitGenerateSources() {
        String before = codecentric("2.3.0.RELEASE", "");
        String explicit = before.replace("<goals>", "<phase>generate-sources</phase><goals>");
        rewriteRun(
          pomXml(before, withApis(before), s -> s.path("default/pom.xml")),
          pomXml(explicit, withApis(explicit), s -> s.path("explicit/pom.xml"))
        );
    }

    @Test
    void codecentricWithExistingWsApiAddsMissingCompileApis() {
        String before = codecentric("2.3.0.RELEASE", "");
        String after = withApis(before);
        String wsOnly = after.replaceAll("(?s)        <dependency>\\s*<groupId>jakarta\\.jws</groupId>.*?</dependency>\\n", "")
          .replaceAll("(?s)        <dependency>\\s*<groupId>jakarta\\.xml\\.bind</groupId>.*?</dependency>\\n", "");
        rewriteRun(pomXml(wsOnly, after));
    }

    @Test
    void codecentricModernAndCustomToolsAreExcluded() {
        String before = codecentric("2.3.0.RELEASE", "");
        String modern = withApis(before).replace("2.3.3", "4.0.2").replace("2.1.0", "3.0.0");
        rewriteRun(
          pomXml(modern, s -> s.path("modern/pom.xml")),
          pomXml(modern.replace("<dependencies>", "<dependencyManagement><dependencies>")
            .replace("</dependencies>", "</dependencies></dependencyManagement>"), s -> s.path("managed-api/pom.xml")),
          pomXml(codecentric("2.3.0.RELEASE", "<dependencies/>"), s -> s.path("custom/pom.xml")),
          pomXml(codecentric("2.3.0.RELEASE", "<configuration><executable>/custom/tool</executable></configuration>"), s -> s.path("external/pom.xml")),
          pomXml(before.replace("<build>", "<build><pluginManagement><plugins><plugin><groupId>de.codecentric</groupId>" +
            "<artifactId>cxf-spring-boot-starter-maven-plugin</artifactId><version>2.3.0.RELEASE</version>" +
            "<dependencies><dependency><groupId>com.sun.xml.ws</groupId><artifactId>jaxws-tools</artifactId>" +
            "<version>4.0.3</version></dependency></dependencies></plugin></plugins></pluginManagement>"), s -> s.path("managed-tool/pom.xml")),
          pomXml(before.replace("<goals>", "<phase>generate-test-sources</phase><goals>"), s -> s.path("test/pom.xml")),
          pomXml(codecentric("2.3.1-jaxb-jaxws-3.0.0-M4", ""), s -> s.path("milestone/pom.xml"))
        );
    }

    @Test
    void existingApisAreIdempotent() {
        rewriteRun(pomXml(withApis(pom("org.codehaus.mojo", "2.6", ""))));
    }

    @Test
    void modernApisAndManagedApisAreUntouched() {
        String modern = pom("org.codehaus.mojo", "2.6", "").replace("    <build>", """
              <dependencies>
                  <dependency>
                      <groupId>jakarta.xml.ws</groupId>
                      <artifactId>jakarta.xml.ws-api</artifactId>
                      <version>4.0.2</version>
                  </dependency>
              </dependencies>
              <build>""");
        rewriteRun(
          pomXml(modern, s -> s.path("modern-api/pom.xml")),
          pomXml(modern.replace("<dependencies>", "<dependencyManagement><dependencies>")
            .replace("</dependencies>", "</dependencies></dependencyManagement>"), s -> s.path("managed-api/pom.xml"))
        );
    }

    @Test
    void managedToolOverridesAreUntouched() {
        String before = pom("org.codehaus.mojo", "2.6", "");
        rewriteRun(pomXml(before.replace("<build>", """
          <build><pluginManagement><plugins><plugin>
              <groupId>org.codehaus.mojo</groupId>
              <artifactId>jaxws-maven-plugin</artifactId>
              <version>2.6</version>
              <dependencies><dependency>
                  <groupId>com.sun.xml.ws</groupId><artifactId>jaxws-tools</artifactId><version>4.0.3</version>
              </dependency></dependencies>
          </plugin></plugins></pluginManagement>""")));
    }

    @Test
    void unknownOrModernToolsAndOverridesAreUntouched() {
        rewriteRun(
          pomXml(pom("com.sun.xml.ws", "3.0.0", ""), s -> s.path("modern3/pom.xml")),
          pomXml(pom("com.sun.xml.ws", "4.0.3", ""), s -> s.path("modern4/pom.xml")),
          pomXml(pom("org.codehaus.mojo", "2.7", ""), s -> s.path("unknown/pom.xml")),
          pomXml(pom("org.codehaus.mojo", "2.6", "<configuration><executable>/custom/wsimport</executable></configuration>"), s -> s.path("external/pom.xml")),
          pomXml(pom("org.codehaus.mojo", "2.6", "<dependencies/>"), s -> s.path("overrides/pom.xml")),
          pomXml(pom("org.codehaus.mojo", "2.6", "").replace("<goals>", "<phase>generate-test-sources</phase><goals>"), s -> s.path("test/pom.xml")),
          pomXml(pom("org.codehaus.mojo", "2.6", "").replace("<plugins>", "<pluginManagement><plugins>").replace("</plugins>", "</plugins></pluginManagement>"), s -> s.path("managed/pom.xml"))
        );
    }
    @Test
    void preserveTransitiveModernApiWithoutDependencyManagement() {
        String before = pom("org.codehaus.mojo", "2.6", "").replace("    <build>", """
              <dependencies>
                  <dependency>
                      <groupId>org.glassfish.jaxb</groupId>
                      <artifactId>jaxb-runtime</artifactId>
                      <version>4.0.5</version>
                  </dependency>
              </dependencies>
              <build>""");
        rewriteRun(pomXml(before));
    }

    @Test
    void codecentricInheritedExecutionExecutableIsExcluded() {
        rewriteRun(s -> s.recipe(new AddApisForJaxwsCodegen()),
          pomXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>example</groupId><artifactId>parent</artifactId><version>1</version>
              <packaging>pom</packaging><modules><module>child</module></modules>
              <build><plugins><plugin>
                <groupId>de.codecentric</groupId><artifactId>cxf-spring-boot-starter-maven-plugin</artifactId><version>2.3.0.RELEASE</version>
                <executions><execution><id>generate</id>
                  <configuration><executable>/custom/wsimport</executable></configuration>
                </execution></executions>
              </plugin></plugins></build>
            </project>
            """),
          dir("child", pomXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <parent><groupId>example</groupId><artifactId>parent</artifactId><version>1</version></parent>
              <artifactId>child</artifactId>
              <build><plugins><plugin>
                <groupId>de.codecentric</groupId><artifactId>cxf-spring-boot-starter-maven-plugin</artifactId><version>2.3.0.RELEASE</version>
                <executions><execution><id>generate</id><goals><goal>generate</goal></goals></execution></executions>
              </plugin></plugins></build>
            </project>
            """)));
    }

    @Test
    void inheritedExecutionExecutableIsExcluded() {
        rewriteRun(s -> s.recipe(new AddApisForJaxwsCodegen()),
          pomXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>example</groupId><artifactId>parent</artifactId><version>1</version>
              <packaging>pom</packaging><modules><module>child</module></modules>
              <build><plugins><plugin>
                <groupId>org.codehaus.mojo</groupId><artifactId>jaxws-maven-plugin</artifactId><version>2.6</version>
                <executions><execution><id>generate</id>
                  <configuration><executable>/custom/wsimport</executable></configuration>
                </execution></executions>
              </plugin></plugins></build>
            </project>
            """),
          dir("child", pomXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <parent><groupId>example</groupId><artifactId>parent</artifactId><version>1</version></parent>
              <artifactId>child</artifactId>
              <build><plugins><plugin>
                <groupId>org.codehaus.mojo</groupId><artifactId>jaxws-maven-plugin</artifactId><version>2.6</version>
                <executions><execution><id>generate</id><goals><goal>wsimport</goal></goals></execution></executions>
              </plugin></plugins></build>
            </project>
            """)));
    }

    @Test
    void inheritedExecutionToolchainIsExcluded() {
        rewriteRun(s -> s.recipe(new AddApisForJaxwsCodegen()),
          pomXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>example</groupId><artifactId>parent</artifactId><version>1</version>
              <packaging>pom</packaging><modules><module>child</module></modules>
              <build><plugins><plugin>
                <groupId>org.codehaus.mojo</groupId><artifactId>jaxws-maven-plugin</artifactId><version>2.6</version>
                <executions><execution><id>generate</id>
                  <configuration><useJdkToolchainExecutable>true</useJdkToolchainExecutable></configuration>
                </execution></executions>
              </plugin></plugins></build>
            </project>
            """),
          dir("child", pomXml("""
            <project>
              <modelVersion>4.0.0</modelVersion>
              <parent><groupId>example</groupId><artifactId>parent</artifactId><version>1</version></parent>
              <artifactId>child</artifactId>
              <build><plugins><plugin>
                <groupId>org.codehaus.mojo</groupId><artifactId>jaxws-maven-plugin</artifactId><version>2.6</version>
                <executions><execution><id>generate</id><goals><goal>wsimport</goal></goals></execution></executions>
              </plugin></plugins></build>
            </project>
            """)));
    }

}
