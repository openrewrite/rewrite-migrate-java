/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.javax;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class AddCommonAnnotationsDependenciesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource(
            "/META-INF/rewrite/add-common-annotations-dependencies.yml",
            "org.openrewrite.java.migrate.javax.AddCommonAnnotationsDependencies")
          .allSources(src -> src.markers(javaVersion(8)));
    }

    @Test
    void addDependencyIfAnnotationJsr250Present() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("jakarta.annotation-api")),
          mavenProject("my-project",
            //language=java
            srcMainJava(
              java(
                """
                  import javax.annotation.Generated;

                  @Generated("Hello")
                  class A {
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.sample</groupId>
                  <artifactId>sample</artifactId>
                  <version>1.0.0</version>
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
                """
            )
          )
        );
    }
}
