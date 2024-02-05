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

package org.openrewrite.java.migrate.javaee;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.migrate.jakarta.UpdateGetRealPath;
import org.openrewrite.java.migrate.javax.AddTableGenerator;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddTableGeneratorTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "javax.persistence-api-2.2"))
          .recipe(new AddTableGenerator());
    }

    @DocumentExample
    @Test
    void generatedValueExample() {
        rewriteRun(
          //language=java
          java(
            """
               package com.ibm.test;
               
               import javax.persistence.Entity;
               import javax.persistence.GeneratedValue;
               import javax.persistence.GenerationType;
               import javax.persistence.Id;
               import javax.persistence.TableGenerator;
               
               @Entity
               public class GeneratedValueExample  {
               
               	// flag it
               	@Id
               	@GeneratedValue(strategy=GenerationType.AUTO)
               	private int id;
               	
               	// flag it.  Does not require @Id
               	@GeneratedValue
               	private int id2;
               	
               	// flag it even though it has a TableGenerator since GeneratedValue is default
               	// A second TableGenerator will be created.
               	@TableGenerator(name = "SOME_TABLE", table = "SOME_TABLE", pkColumnName = "ID", valueColumnName = "SEQUENCE_VALUE", pkColumnValue = "0")
               	@GeneratedValue
               	private int id3;
               
               }
               """, """
              package com.ibm.test;
                                     
              import javax.persistence.Entity;
              import javax.persistence.GeneratedValue;
              import javax.persistence.GenerationType;
              import javax.persistence.Id;
              import javax.persistence.TableGenerator;
                                     
              @Entity
              public class GeneratedValueExample  {
                                     
                  // flag it
                  @Id
                  @javax.persistence.TableGenerator(name = "OPENJPA_SEQUENCE_TABLE", table = "OPENJPA_SEQUENCE_TABLE", pkColumnName = "ID", valueColumnName = "SEQUENCE_VALUE", pkColumnValue = "0")
                  @GeneratedValue(strategy = javax.persistence.GenerationType.TABLE, generator = "OPENJPA_SEQUENCE_TABLE")
                  private int id;
                                     
                  // flag it.  Does not require @Id
                  @GeneratedValue(strategy = javax.persistence.GenerationType.TABLE, generator = "OPENJPA_SEQUENCE_TABLE")
                  @javax.persistence.TableGenerator(name = "OPENJPA_SEQUENCE_TABLE", table = "OPENJPA_SEQUENCE_TABLE", pkColumnName = "ID", valueColumnName = "SEQUENCE_VALUE", pkColumnValue = "0")
                  private int id2;
                                     
                  // flag it even though it has a TableGenerator since GeneratedValue is default
                  // A second TableGenerator will be created.
                  @TableGenerator(name = "SOME_TABLE", table = "SOME_TABLE", pkColumnName = "ID", valueColumnName = "SEQUENCE_VALUE", pkColumnValue = "0")
                  @javax.persistence.TableGenerator(name = "OPENJPA_SEQUENCE_TABLE", table = "OPENJPA_SEQUENCE_TABLE", pkColumnName = "ID", valueColumnName = "SEQUENCE_VALUE", pkColumnValue = "0")
                  @GeneratedValue(strategy = javax.persistence.GenerationType.TABLE, generator = "OPENJPA_SEQUENCE_TABLE")
                  private int id3;
                                     
              }
              """
          )
        );
    }

    @Test
    void generatedValueName() {
        rewriteRun(
          //language=java
          java(
            """
               package com.ibm.test;
               import javax.persistence.Entity;
               import javax.persistence.GeneratedValue;
               import javax.persistence.Id;
               
               @Entity
               class GeneratedValueName  {
               	// flag it
               	@Id
               	@GeneratedValue
               	private String name;
               }
               """, """
                       package com.ibm.test;
                       import javax.persistence.Entity;
                       import javax.persistence.GeneratedValue;
                       import javax.persistence.Id;
                       
                       @Entity
                       class GeneratedValueName  {
                           // flag it
                           @Id
                           @javax.persistence.TableGenerator(name = "OPENJPA_SEQUENCE_TABLE", table = "OPENJPA_SEQUENCE_TABLE", pkColumnName = "ID", valueColumnName = "SEQUENCE_VALUE", pkColumnValue = "0")
                           @GeneratedValue(strategy = javax.persistence.GenerationType.TABLE, generator = "OPENJPA_SEQUENCE_TABLE")
                           private String name;
                       }
                       """
          )
        );
    }

    @Test
    void generatedValueQuickFixApplied() {
        rewriteRun(
          //language=java
          java(
            """
               package com.ibm.test;
               import javax.persistence.Entity;
               import javax.persistence.GeneratedValue;
               import javax.persistence.GenerationType;
               import javax.persistence.Id;
               import javax.persistence.TableGenerator;
               
               @Entity
               class GeneratedValueQuickFixApplied  {
               	// not flagged since GeneratedValue is not default or AUTO
               	@Id
               	@TableGenerator(name = "OPENJPA_SEQUENCE_TABLE", table = "OPENJPA_SEQUENCE_TABLE", pkColumnName = "ID", valueColumnName = "SEQUENCE_VALUE", pkColumnValue = "0")
               	@GeneratedValue(strategy = GenerationType.TABLE, generator = "OPENJPA_SEQUENCE_TABLE")
               	private int id;
               }
               """
          )
        );
    }

    @Test
    void generatedValueStrategySpaces() {
        rewriteRun(
          //language=java
          java(
            """
               package com.ibm.test;
               import javax.persistence.Entity;
               import javax.persistence.GeneratedValue;
               import javax.persistence.GenerationType;
               import javax.persistence.Id;
               
               @Entity
               class GeneratedValueStrategySpaces  {
               	// flag it
               	@Id
               	@GeneratedValue( strategy = GenerationType.AUTO )
               	private int id;
               }
               """,
            """
               package com.ibm.test;
               import javax.persistence.Entity;
               import javax.persistence.GeneratedValue;
               import javax.persistence.GenerationType;
               import javax.persistence.Id;
               
               @Entity
               class GeneratedValueStrategySpaces  {
                   // flag it
                   @Id
                   @javax.persistence.TableGenerator(name = "OPENJPA_SEQUENCE_TABLE", table = "OPENJPA_SEQUENCE_TABLE", pkColumnName = "ID", valueColumnName = "SEQUENCE_VALUE", pkColumnValue = "0")
                   @GeneratedValue(strategy = javax.persistence.GenerationType.TABLE, generator = "OPENJPA_SEQUENCE_TABLE")
                   private int id;
               }
               """
          )
        );
    }
}
