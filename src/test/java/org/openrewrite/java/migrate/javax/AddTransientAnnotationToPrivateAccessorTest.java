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
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddTransientAnnotationToPrivateAccessorTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "javax.persistence-api-2.2"))
          .recipe(new AddTransientAnnotationToPrivateAccessor());
    }

    @DocumentExample
    @Test
    void addTransientToMethodReturningIdentifier() {
        //language=java
        rewriteRun(
          java(
            """
              package entities;

              import javax.persistence.Entity;
              import javax.persistence.Id;

              @Entity
              public class PrivateAccessor  {
                  private int id;
                  private int nonPersistentField;

                  @Id
                  public int getId() {
                      return id;
                  }

                  private int getNonPersistentField() {
                      return nonPersistentField;
                  }
              }
              """,
            """
              package entities;

              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.Transient;

              @Entity
              public class PrivateAccessor  {
                  private int id;
                  private int nonPersistentField;

                  @Id
                  public int getId() {
                      return id;
                  }

                  @Transient
                  private int getNonPersistentField() {
                      return nonPersistentField;
                  }
              }
              """
          )
        );
    }

    @Test
    void addTransientToMethodReturningFieldAccess() {
        //language=java
        rewriteRun(
          java(
            """
              package entities;

              import javax.persistence.Entity;
              import javax.persistence.Id;

              @Entity
              public class PrivateAccessor  {
                  private int id;
                  private int nonPersistentField;

                  @Id
                  public int getId() {
                      return id;
                  }

                  private int getNonPersistentField() {
                      return this.nonPersistentField;
                  }
              }
              """,
            """
              package entities;

              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.Transient;

              @Entity
              public class PrivateAccessor  {
                  private int id;
                  private int nonPersistentField;

                  @Id
                  public int getId() {
                      return id;
                  }

                  @Transient
                  private int getNonPersistentField() {
                      return this.nonPersistentField;
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangePublicGetter() {
        //language=java
        rewriteRun(
          java(
            """
              package entities;

              import javax.persistence.Entity;
              import javax.persistence.Id;

              @Entity
              public class PrivateAccessor  {
                private int id;
                private int field;

                @Id
                public int getId() {
                  return id;
                }
              
                public int getField() {
                  return field; // Public method
                }
              }
              """
          )
        );
    }


    @Test
    void doNotChangeVoidReturnType() {
        //language=java
        rewriteRun(
          java(
            """
              package entities;

              import javax.persistence.Entity;
              import javax.persistence.Id;

              @Entity
              public class PrivateAccessor  {
                  private int id;
                  private int field;

                  @Id
                  public int getId() {
                    return id;
                  }

                  private void getField() {
                    // void return type
                  }
              }
              """
          )
        );
    }
}