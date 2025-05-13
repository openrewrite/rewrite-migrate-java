package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

public class JavaxMailToJakartaMailTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpath("mail-1.4.7", "javax.mail-api-1.6.2", "jakarta.mail-api-2.0.1"))
          .recipeFromResource(
            "/META-INF/rewrite/jakarta-ee-9.yml",
            "org.openrewrite.java.migrate.jakarta.JavaxMailToJakartaMail");
    }

    @Test
    void switchesJavaxMailApiDependencyToJakartaMailApiDependency() {
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
                            <groupId>javax.mail</groupId>
                            <artifactId>javax.mail-api</artifactId>
                            <version>1.4.7</version>
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
                            <groupId>jakarta.mail</groupId>
                            <artifactId>jakarta.mail-api</artifactId>
                            <version>2.0.1</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          ),
          srcMainJava(
            //language=java
            java(
              """
                import javax.mail.Session;
                public class TestApplication {
                }
                """,
              """
                import jakarta.mail.Session;
                public class TestApplication {
                }
                """
            )
          )
        );
    }

    @Test
    void switchesJavaxMailDependencyToJakartaMailApiDependency() {
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
                            <groupId>javax.mail</groupId>
                            <artifactId>mail</artifactId>
                            <version>1.3.3</version>
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
                            <groupId>jakarta.mail</groupId>
                            <artifactId>jakarta.mail-api</artifactId>
                            <version>2.0.1</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          ),
          srcMainJava(
            //language=java
            java(
              """
                import javax.mail.Session;
                public class TestApplication {
                }
                """,
              """
                import jakarta.mail.Session;
                public class TestApplication {
                }
                """
            )
          )
        );
    }
}
