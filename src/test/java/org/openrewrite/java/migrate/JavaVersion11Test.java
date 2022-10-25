package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

public class JavaVersion11Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.migrate")
          .build().activateRecipes("org.openrewrite.java.migrate.JavaVersion11"));
    }

    @Test
    void changeJavaVersion() {

        rewriteRun(
          pomXml("""
                <project>
                  <modelVersion>4.0.0</modelVersion>
                   
                  <properties>
                    <java.version>1.8</java.version>
                  </properties>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                </project>
            """, """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                   
                  <properties>
                    <java.version>11</java.version>
                  </properties>
                  
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                </project>
            """
          ));
    }

    @Test
    void changeMavenCompiler() {
        rewriteRun(
          pomXml("""
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                     
                    <properties>
                      <maven.compiler.source>1.8</maven.compiler.source>
                      <maven.compiler.target>1.8</maven.compiler.target>
                    </properties>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """,
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                     
                    <properties>
                      <maven.compiler.source>11</maven.compiler.source>
                      <maven.compiler.target>11</maven.compiler.target>
                    </properties>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """)
        );
    }
}
