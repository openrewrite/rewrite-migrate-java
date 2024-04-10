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

class UseJoinColumnForMappingTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "javax.persistence-api-2.2")
            .dependsOn("public class Account {}"))
          .recipe(new UseJoinColumnForMapping());
    }

    @DocumentExample
    @Test
    void useJoinColumnForManyToOne() {
        //language=java
        rewriteRun(
          java(
            """
              import javax.persistence.Entity;
              import javax.persistence.Column;
              import javax.persistence.Id;
              import javax.persistence.ManyToOne;

              @Entity
              public class TransactionEntity {
                  @Id
                  private int id;

                  private long transactionNumber;
                  private double amount;

                  @ManyToOne
                  @Column(name="account")
                  private Account account;
              }
              """,
            """
              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.JoinColumn;
              import javax.persistence.ManyToOne;

              @Entity
              public class TransactionEntity {
                  @Id
                  private int id;

                  private long transactionNumber;
                  private double amount;

                  @ManyToOne
                  @JoinColumn(name="account")
                  private Account account;
              }
              """
          )
        );
    }

    @Test
    void useJoinColumnForOneToOne() {
        //language=java
        rewriteRun(
          java(
            """
              import javax.persistence.Entity;
              import javax.persistence.Column;
              import javax.persistence.Id;
              import javax.persistence.OneToOne;

              @Entity
              public class TransactionEntity {
                  @Id
                  private int id;

                  private long transactionNumber;
                  private double amount;

                  @OneToOne
                  @Column(name="account")
                  private Account account;
              }
              """,
            """
              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.JoinColumn;
              import javax.persistence.OneToOne;

              @Entity
              public class TransactionEntity {
                  @Id
                  private int id;

                  private long transactionNumber;
                  private double amount;

                  @OneToOne
                  @JoinColumn(name="account")
                  private Account account;
              }
              """
          )
        );
    }

    @Test
    void useJoinColumnForManyToOneNoAttr() {
        //language=java
        rewriteRun(
          java(
            """
              import javax.persistence.Entity;
              import javax.persistence.Column;
              import javax.persistence.Id;
              import javax.persistence.ManyToOne;

              @Entity
              public class TransactionEntity {
                  @Id
                  private int id;

                  private long transactionNumber;
                  private double amount;

                  @ManyToOne
                  @Column
                  private Account account;
              }
              """,
            """
              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.JoinColumn;
              import javax.persistence.ManyToOne;

              @Entity
              public class TransactionEntity {
                  @Id
                  private int id;

                  private long transactionNumber;
                  private double amount;

                  @ManyToOne
                  @JoinColumn
                  private Account account;
              }
              """
          )
        );
    }
    @Test
    void useJoinColumnForOneToOneNoAttr() {
        //language=java
        rewriteRun(
          java(
            """
              import javax.persistence.Entity;
              import javax.persistence.Column;
              import javax.persistence.Id;
              import javax.persistence.OneToOne;

              @Entity
              public class TransactionEntity {
                  @Id
                  private int id;

                  private long transactionNumber;
                  private double amount;

                  @OneToOne
                  @Column
                  private Account account;
              }
              """,
            """
              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.JoinColumn;
              import javax.persistence.OneToOne;

              @Entity
              public class TransactionEntity {
                  @Id
                  private int id;

                  private long transactionNumber;
                  private double amount;

                  @OneToOne
                  @JoinColumn
                  private Account account;
              }
              """
          )
        );
    }

    @Test
    void doNotChangeIfNoMapping() {
        //language=java
        rewriteRun(
          java(
            """
              import javax.persistence.Entity;
              import javax.persistence.Column;
              import javax.persistence.Id;
              import javax.persistence.ManyToOne;

              @Entity
              public class TransactionEntity {
                  @Id
                  private int id;

                  private long transactionNumber;
                  private double amount;

                  @Column(name="account")
                  private Account account;
              }
              """
          )
        );
    }

    @Test
    void doNotChangeIfNoColumn() {
        //language=java
        rewriteRun(
          java(
            """
              import javax.persistence.Entity;
              import javax.persistence.Column;
              import javax.persistence.Id;
              import javax.persistence.ManyToOne;

              @Entity
              public class TransactionEntity {
                  @Id
                  private int id;

                  private long transactionNumber;
                  private double amount;

                  @ManyToOne
                  private Account account;
              }
              """
          )
        );
    }

    @Test
    void dropAttributesOnColumnChange() { // and avoid on unaffected @Column
        //language=java
        rewriteRun(
          java(
            """
              import javax.persistence.Entity;
              import javax.persistence.Column;
              import javax.persistence.Id;
              import javax.persistence.ManyToOne;

              @Entity
              public class TransactionEntity {
                  @Id
                  private int id;

                  private long transactionNumber;
                  @Column(name="amount", length=512, precision=12, scale=2)
                  private double amount;

                  @ManyToOne
                  @Column(name="account", length=512, precision=12, scale=2)
                  private Account account;
              }
              """,
            """
              import javax.persistence.*;

              @Entity
              public class TransactionEntity {
                  @Id
                  private int id;

                  private long transactionNumber;
                  @Column(name="amount", length=512, precision=12, scale=2)
                  private double amount;

                  @ManyToOne
                  @JoinColumn(name="account")
                  private Account account;
              }
              """
          )
        );
    }

    @Test
    void handleInnerClass() {
        //language=java
        rewriteRun(
          java(
            """
              import javax.persistence.Entity;
              import javax.persistence.Column;
              import javax.persistence.Id;
              import javax.persistence.ManyToOne;

              @Entity
              public class TransactionEntity {
                  @Id
                  private int id;

                  private long transactionNumber;
                  private double amount;

                  class InnerClass{
                      @ManyToOne
                      @Column(name="account")
                      private Account account;
                  }
              }
              """,
            """
              import javax.persistence.Entity;
              import javax.persistence.Id;
              import javax.persistence.JoinColumn;
              import javax.persistence.ManyToOne;

              @Entity
              public class TransactionEntity {
                  @Id
                  private int id;

                  private long transactionNumber;
                  private double amount;

                  class InnerClass{
                      @ManyToOne
                      @JoinColumn(name="account")
                      private Account account;
                  }
              }
              """
          )
        );
    }
}