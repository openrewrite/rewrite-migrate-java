/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class JavaxWsToJakartaWsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpath("javax.ws.rs-api"))
          .recipeFromResource(
            "/META-INF/rewrite/jakarta-ee-9.yml",
            "org.openrewrite.java.migrate.jakarta.JavaxWsToJakartaWs");
    }

    @Test
    void switchesJavaxWsApiDependencyToJakartaWsApiDependency() {
        rewriteRun(
          mavenProject(
            "Sample",
            srcMainJava(
              //language=java
              java(
                """
                  import javax.ws.rs.core.MediaType;
                  public class TestApplication {
                      MediaType getType() {
                          return MediaType.APPLICATION_JSON_TYPE;
                      }
                  }
                  """,
                """
                  import jakarta.ws.rs.core.MediaType;
                  public class TestApplication {
                      MediaType getType() {
                          return MediaType.APPLICATION_JSON_TYPE;
                      }
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <dependencies>
                        <dependency>
                            <groupId>javax.ws.rs</groupId>
                            <artifactId>javax.ws.rs-api</artifactId>
                            <version>2.1.1</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.ws.rs</groupId>
                            <artifactId>jakarta.ws.rs-api</artifactId>
                            <version>3.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }

    @Test
    void addsJakartaWsApiDependencyIfExistingInTransitive() {
        rewriteRun(
          mavenProject(
            "Sample",
            srcMainJava(
              //language=java
              java(
                """
                  import javax.ws.rs.core.MediaType;
                  public class TestApplication {
                      MediaType getType() {
                          return MediaType.APPLICATION_JSON_TYPE;
                      }
                  }
                  """,
                """
                  import jakarta.ws.rs.core.MediaType;
                  public class TestApplication {
                      MediaType getType() {
                          return MediaType.APPLICATION_JSON_TYPE;
                      }
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                </project>
                """,
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.ws.rs</groupId>
                            <artifactId>jakarta.ws.rs-api</artifactId>
                            <version>3.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }

    @Test
    void ignoresJakartaWsApiDependencyIfAlreadyExisting() {
        rewriteRun(
          mavenProject(
            "Sample",
            //language=xml
            pomXml(
              """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.ws.rs</groupId>
                            <artifactId>jakarta.ws.rs-api</artifactId>
                            <version>3.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }
}
