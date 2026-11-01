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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.test.SourceSpecs.dir;

class UpgradeGmavenGroovyForJava25Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeGmavenGroovyForJava25());
    }

    private static String pluginPom(String version) {
        return """
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>com.acme</groupId>
            <artifactId>example</artifactId>
            <version>1.0</version>
            <properties><groovy.version>%s</groovy.version></properties>
            <build><pluginManagement><plugins><plugin>
              <groupId>org.codehaus.gmaven</groupId>
              <artifactId>groovy-maven-plugin</artifactId>
              <version>2.1.1</version>
              <dependencies><dependency>
                <groupId>org.codehaus.groovy</groupId>
                <artifactId>groovy</artifactId>
                <version>${groovy.version}</version>
              </dependency></dependencies>
            </plugin></plugins></pluginManagement></build>
          </project>
          """.formatted(version);
    }

    @Test
    void upgradeExplicitCompilerInPluginManagement() {
        rewriteRun(pomXml(pluginPom("3.0.23"), pluginPom("3.0.25")));
    }

    @Test
    void retainCompatibleCompiler() {
        rewriteRun(pomXml(pluginPom("3.0.25")));
    }

    @Test
    void leaveUnrelatedGroovyDependencyAlone() {
        rewriteRun(pomXml("""
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>com.acme</groupId>
            <artifactId>example</artifactId>
            <version>1.0</version>
            <dependencies><dependency>
              <groupId>org.codehaus.groovy</groupId>
              <artifactId>groovy</artifactId>
              <version>3.0.23</version>
            </dependency></dependencies>
          </project>
          """));
    }

    @Test
    void upgradeCompilerVersionInheritedFromParent() {
        String parent = """
          <project>
            <modelVersion>4.0.0</modelVersion>
            <groupId>com.acme</groupId>
            <artifactId>parent</artifactId>
            <version>1.0</version>
            <packaging>pom</packaging>
            <properties><groovy.version>3.0.23</groovy.version></properties>
            <modules><module>child</module></modules>
          </project>
          """;
        String child = """
          <project>
            <modelVersion>4.0.0</modelVersion>
            <parent>
              <groupId>com.acme</groupId>
              <artifactId>parent</artifactId>
              <version>1.0</version>
            </parent>
            <artifactId>child</artifactId>
            <build><plugins><plugin>
              <groupId>org.codehaus.gmaven</groupId>
              <artifactId>groovy-maven-plugin</artifactId>
              <version>2.1.1</version>
              <dependencies><dependency>
                <groupId>org.codehaus.groovy</groupId>
                <artifactId>groovy</artifactId>
                <version>${groovy.version}</version>
              </dependency></dependencies>
            </plugin></plugins></build>
          </project>
          """;
        rewriteRun(
          pomXml(parent),
          dir("child", pomXml(child, child.replace("<version>${groovy.version}</version>", "<version>3.0.25</version>")))
        );
    }

    @Test
    void preserveOtherCompilerLinesAndNewerPatches() {
        rewriteRun(pomXml(pluginPom("2.4.20")), pomXml(pluginPom("3.0.25"), spec -> spec.path("other/pom.xml")));
    }
}
