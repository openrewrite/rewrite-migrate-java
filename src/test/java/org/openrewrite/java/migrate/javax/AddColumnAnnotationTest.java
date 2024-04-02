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

class AddColumnAnnotationTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "javax.persistence-api-2.2"))
          .recipe(new AddColumnAnnotation());
    }

    @DocumentExample
    @Test
    void addColumnAnnotationAlongElementCollection() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;

              import javax.persistence.ElementCollection;
              import javax.persistence.Entity;
              import javax.persistence.Id;

              @Entity
              public class ElementCollectionEntity {
                  @Id
                  private int id;

                  @ElementCollection
                  private List<String> listofStrings;
              }
              """,
            """
              import java.util.List;

              import javax.persistence.Column;
              import javax.persistence.ElementCollection;
              import javax.persistence.Entity;
              import javax.persistence.Id;

              @Entity
              public class ElementCollectionEntity {
                  @Id
                  private int id;

                  @Column(name = "element")
                  @ElementCollection
                  private List<String> listofStrings;
              }
              """
          )
        );
    }

    @Test
    void updateColumnAnnotationWithoutExistingAttributes() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import javax.persistence.ElementCollection;
              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.Column;

              @Entity
              public class ElementCollectionEntity {
                  @Id
                  private int id;

                  @ElementCollection
                  @Column
                  private List<String> listofStrings;
              }
              """, """
              import java.util.List;
              import javax.persistence.ElementCollection;
              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.Column;

              @Entity
              public class ElementCollectionEntity {
                  @Id
                  private int id;

                  @ElementCollection
                  @Column(name = "element")
                  private List<String> listofStrings;
              }
              """
          )
        );
    }

    @Test
    void updateColumnAnnotationWithExistingAttributes() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import javax.persistence.ElementCollection;
              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.Column;

              @Entity
              public class ElementCollectionEntity {
                  @Id
                  private int id;

                  @Column(nullable = false, length = 512)
                  @ElementCollection
                  private List<String> listofStrings;
              }
              """,
            """
              import java.util.List;
              import javax.persistence.ElementCollection;
              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.Column;

              @Entity
              public class ElementCollectionEntity {
                  @Id
                  private int id;

                  @Column(name = "element", nullable = false, length = 512)
                  @ElementCollection
                  private List<String> listofStrings;
              }
              """
          )
        );
    }

    @Test
    void addColumnToApplicableFieldWhileAvoidingOthers() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import javax.persistence.ElementCollection;
              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.Column;

              @Entity
              public class ElementCollectionEntity {
                  @Id
                  private int id;

                  @Column
                  private List<Integer> listOfInts;

                  @ElementCollection
                  private List<String> listofStrings;
              }
              """,
            """
              import java.util.List;
              import javax.persistence.ElementCollection;
              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.Column;

              @Entity
              public class ElementCollectionEntity {
                  @Id
                  private int id;

                  @Column
                  private List<Integer> listOfInts;

                  @Column(name = "element")
                  @ElementCollection
                  private List<String> listofStrings;
              }
              """
          )
        );
    }

    @Test
    void doNotChangeColumnWithoutSiblingElementCollection() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import javax.persistence.ElementCollection;
              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.Column;

              @Entity
              public class ElementCollectionEntity {
                  @Id
                  private int id;

                  @Column
                  private List<String> listofStrings;
              }
              """
          )
        );
    }

    @Test
    void doNotChangeColumnWithExistingNameAttribute() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import javax.persistence.ElementCollection;
              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.Column;

              @Entity
              public class ElementCollectionEntity {
                  @Id
                  private int id;

                  @ElementCollection
                  @Column(name = "test")
                  private List<String> listofStrings;
              }
              """
          )
        );
    }

    @Test
    void doNotAddColumnMappingToTransient() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import javax.persistence.ElementCollection;
              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.Column;
              import javax.persistence.Transient;

              @Entity
              public class ElementCollectionEntity {
                  @Id
                  private int id;

                  @Transient
                  @ElementCollection
                  private List<String> listofStrings;
              }
              """
          )
        );
    }

    @Test
    void doNotAddColumnMappingIfClassNotEntity() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import javax.persistence.ElementCollection;
              import javax.persistence.Id;
              import javax.persistence.Column;

              public class ElementCollectionEntity {
                  @Id
                  private int id;

                  @ElementCollection
                  private List<String> listofStrings;
              }
              """
          )
        );
    }

    @Test
    void handleInnerClass() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import javax.persistence.ElementCollection;
              import javax.persistence.Entity;
              import javax.persistence.Id;

              @Entity
              public class ElementCollectionEntity {
                  @Id
                  private int id;

                  @ElementCollection
                  private List<String> listofStrings;

                  class InnerClass {
                      @Id
                      private int id2;
                      
                      @ElementCollection
                      private List<String> listofStrings2;
                  }
              }
              """,
            """
              import java.util.List;

              import javax.persistence.Column;
              import javax.persistence.ElementCollection;
              import javax.persistence.Entity;
              import javax.persistence.Id;

              @Entity
              public class ElementCollectionEntity {
                  @Id
                  private int id;

                  @Column(name = "element")
                  @ElementCollection
                  private List<String> listofStrings;
                  
                  class InnerClass {
                      @Id
                      private int id2;

                      @Column(name = "element")
                      @ElementCollection
                      private List<String> listofStrings2;
                  }
              }
              """
          )
        );
    }
}