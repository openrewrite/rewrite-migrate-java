package org.openrewrite.java.migrate.javaee;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.migrate.javax.AddDefaultConstructorToEntityClass;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class AddDefaultConstructorToEntityClassTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "javax.persistence-api-2.2"))
          .recipe(new AddDefaultConstructorToEntityClass());
    }

    @DocumentExample
    @Test
    void addMissingDefaultConstructorToEntity() {
        //language=java
        rewriteRun(
          java(
            """
              import javax.persistence.Entity;
              import javax.persistence.Id;

              @Entity
              public class MissingNoArgConstructorEntity {
                  @Id
                  private int id;

                  public MissingNoArgConstructorEntity(int id) {
                      this.id = id;
                  }
              }
              """, """
              import javax.persistence.Entity;
              import javax.persistence.Id;

              @Entity
              public class MissingNoArgConstructorEntity {
                  @Id
                  private int id;

                  public MissingNoArgConstructorEntity(int id) {
                      this.id = id;
                  }

                  public MissingNoArgConstructorEntity() {
                  }
              }
              """)
          );
    }

    @Test
    void addMissingDefaultConstructorToMappedSuperclass() {
        //language=java
        rewriteRun(
          java(
            """
              import javax.persistence.MappedSuperclass;
              import javax.persistence.Id;

              @MappedSuperclass
              public class MissingNoArgConstructorEntity {
                  @Id
                  private int id;

                  public MissingNoArgConstructorEntity(int id) {
                      this.id = id;
                  }
              }
              """, """
              import javax.persistence.MappedSuperclass;
              import javax.persistence.Id;

              @MappedSuperclass
              public class MissingNoArgConstructorEntity {
                  @Id
                  private int id;

                  public MissingNoArgConstructorEntity(int id) {
                      this.id = id;
                  }

                  public MissingNoArgConstructorEntity() {
                  }
              }
              """)
        );
    }

    @Test
    void alreadyHasDefaultConstructor() {
        //language=java
        rewriteRun(
          java(
            """
              import javax.persistence.Entity;
              import javax.persistence.Id;

              @Entity
              public class MissingNoArgConstructorEntity {
                  @Id
                  private int id;

                  public MissingNoArgConstructorEntity() {
                  }

                  public MissingNoArgConstructorEntity(int id) {
                      this.id = id;
                  }
              }
              """)
        );
    }

    @Test
    void classIsNotEntity() {
        //language=java
        rewriteRun(
          java(
            """
              import javax.persistence.Entity;
              import javax.persistence.Id;

              public class MissingNoArgConstructorEntity {
                  @Id
                  private int id;

                  public MissingNoArgConstructorEntity(int id) {
                      this.id = id;
                  }
              }
              """)
        );
    }

    @Test
    void hasNoArgMethodAndNoDefaultConstructor() {
        //language=java
        rewriteRun(
          java(
            """
              import javax.persistence.Entity;
              import javax.persistence.Id;

              @Entity
              public class MissingNoArgConstructorEntity {
                  @Id
                  private int id;

                  public doNothing() {
                  }

                  public MissingNoArgConstructorEntity(int id) {
                      this.id = id;
                  }
              }
              ""","""
              import javax.persistence.Entity;
              import javax.persistence.Id;

              @Entity
              public class MissingNoArgConstructorEntity {
                  @Id
                  private int id;

                  public doNothing() {
                  }

                  public MissingNoArgConstructorEntity(int id) {
                      this.id = id;
                  }

                  public MissingNoArgConstructorEntity() {
                  }
              }
              """)
        );
    }
}