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

class AddTransientAnnotationToCollectionsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "javax.persistence-api-2.2"))
          .recipe(new AddTransientAnnotationToCollections());
    }

    @Test
    @DocumentExample
    void addTransient() {
        rewriteRun(
          java(
            """
              import java.util.Collection;
              import java.util.List;
                            
              import javax.persistence.Entity;
              import javax.persistence.Id;
                            
              @Entity
              public class UnannotatedCollectionEntity {
                  @Id
                  private int id;

                  private Collection collectionField;
                  private List listField;
              }
              """,
            """
              import java.util.Collection;
              import java.util.List;
                            
              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.Transient;
                            
              @Entity
              public class UnannotatedCollectionEntity {
                  @Id
                  private int id;

                  @Transient
                  private Collection collectionField;
                  @Transient
                  private List listField;
              }
              """
          )
        );
    }

    @Test
    void ignoreAlreadyAnnotatedCollection() {
        rewriteRun(
          java(
            """
              import java.util.Collection;
              import java.util.List;
                            
              import javax.persistence.Entity;
              import javax.persistence.Id;
                            
              @Entity
              public class UnannotatedCollectionEntity {
                  @Id
                  private int id;

                  @Id
                  private Collection collectionField;
                  private List listField;
              }
              """,
            """
              import java.util.Collection;
              import java.util.List;
                            
              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.Transient;
                            
              @Entity
              public class UnannotatedCollectionEntity {
                  @Id
                  private int id;

                  @Id
                  private Collection collectionField;
                  @Transient
                  private List listField;
              }
              """
          )
        );
    }

    @Test
    void variousCollections() {
        rewriteRun(
          java(
            """
              import java.util.Collection;
              import java.util.List;
                            
              import javax.persistence.Entity;
              import javax.persistence.Id;
                            
              @Entity
              public class UnannotatedCollectionEntity {
                  @Id
                  private int id;
                  private String string;
                  private String[] arrayString;

                  private Collection collectionField;
                  private List listField;
                  private java.beans.beancontext.BeanContext beanContext;
                  private java.beans.beancontext.BeanContextServices beanCServices;
                  private java.util.concurrent.BlockingDeque<?> bdeque;
                  private java.util.concurrent.BlockingQueue<?> bqueue;
                  private java.util.Deque<?> deque;
                  private java.util.List<?> list;
                  private java.util.NavigableSet<?> navSet;
                  private java.util.Queue<?> queue;
                  private java.util.Set<?> set;
                  private java.util.SortedSet<?> sortedSet;
                  private java.util.concurrent.TransferQueue<?> transferQueue;
              }
              """,
            """
              import java.util.Collection;
              import java.util.List;
                            
              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.Transient;
                            
              @Entity
              public class UnannotatedCollectionEntity {
                  @Id
                  private int id;
                  private String string;
                  private String[] arrayString;

                  @Transient
                  private Collection collectionField;
                  @Transient
                  private List listField;
                  @Transient
                  private java.beans.beancontext.BeanContext beanContext;
                  @Transient
                  private java.beans.beancontext.BeanContextServices beanCServices;
                  @Transient
                  private java.util.concurrent.BlockingDeque<?> bdeque;
                  @Transient
                  private java.util.concurrent.BlockingQueue<?> bqueue;
                  @Transient
                  private java.util.Deque<?> deque;
                  @Transient
                  private java.util.List<?> list;
                  @Transient
                  private java.util.NavigableSet<?> navSet;
                  @Transient
                  private java.util.Queue<?> queue;
                  @Transient
                  private java.util.Set<?> set;
                  @Transient
                  private java.util.SortedSet<?> sortedSet;
                  @Transient
                  private java.util.concurrent.TransferQueue<?> transferQueue;
              }
              """
          )
        );
    }
}