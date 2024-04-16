package org.openrewrite.java.migrate.javax;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.java.Assertions.version;
import static org.openrewrite.maven.Assertions.pomXml;

@SuppressWarnings("LanguageMismatch")
class AddCommonAnnotationsDependenciesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
          Environment.builder()
            .scanYamlResources()
            .build()
            .activateRecipes("org.openrewrite.java.migrate.javax.AddCommonAnnotationsDependencies"))
          .allSources(src -> src.markers(javaVersion(8)));
    }

    @Test
    void addDependencyIfAnnotationJsr250Present() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("jakarta.annotation-api")),
          mavenProject("my-project",
            //language=java
            srcMainJava(version(java("""
              import javax.annotation.Generated;
              
              @Generated("Hello")
              class A {
              }
              """), 8)),
            //language=xml
            pomXml("""
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <dependencies>
                  </dependencies>

                </project>
                """,
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>

                  <dependencies>
                    <dependency>
                      <groupId>jakarta.annotation</groupId>
                      <artifactId>jakarta.annotation-api</artifactId>
                      <version>1.3.5</version>
                    </dependency>
                  </dependencies>

                </project>
                """)
          )
        );
    }

    @Test
    void dontAddDependencyWhenAnnotationJsr250Absent() {
        rewriteRun(
          mavenProject("my-project",
            //language=java
            srcMainJava(java("""
                class A {
                }
                """,
              src -> src.markers(javaVersion(8)))),
            //language=xml
            pomXml("""
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.sample</groupId>
                <artifactId>sample</artifactId>
                <version>1.0.0</version>

                <dependencies>
                </dependencies>

              </project>
              """)
          )
        );
    }

}
