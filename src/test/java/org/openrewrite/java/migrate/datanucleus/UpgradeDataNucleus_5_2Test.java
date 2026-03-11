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
package org.openrewrite.java.migrate.datanucleus;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.xml.Assertions.xml;

class UpgradeDataNucleus_5_2Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.datanucleus.UpgradeDataNucleus_5_2");
    }

    @DocumentExample
    @Test
    void renameQueryPropertyKeys() {
        rewriteRun(
          properties(
            """
            datanucleus.jdoql.strict=true
            datanucleus.jpql.strict=true
            datanucleus.sql.syntaxChecks=true
            """,
            """
            datanucleus.query.jdoql.strict=true
            datanucleus.query.jpql.strict=true
            datanucleus.query.sql.syntaxChecks=true
            """
          )
        );
    }

    @Test
    void renameQueryPropertyKeysInXml() {
        rewriteRun(
          xml(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <persistence>
              <persistence-unit name="test">
                <properties>
                  <property name="datanucleus.jdoql.strict" value="true"/>
                  <property name="datanucleus.jpql.strict" value="true"/>
                </properties>
              </persistence-unit>
            </persistence>
            """,
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <persistence>
              <persistence-unit name="test">
                <properties>
                  <property name="datanucleus.query.jdoql.strict" value="true"/>
                  <property name="datanucleus.query.jpql.strict" value="true"/>
                </properties>
              </persistence-unit>
            </persistence>
            """
          )
        );
    }

    @Test
    void changePackage() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .dependsOn(
              //language=java
              """
              package org.datanucleus.store.rdbms.mapping.datastore;
              public class SomeMapping {}
              """,
              //language=java
              """
              package org.datanucleus.store.rdbms.mapping.column;
              public class SomeMapping {}
              """
            )),
          //language=java
          java(
            """
            import org.datanucleus.store.rdbms.mapping.datastore.SomeMapping;

            class A {
                SomeMapping mapping;
            }
            """,
            """
            import org.datanucleus.store.rdbms.mapping.column.SomeMapping;

            class A {
                SomeMapping mapping;
            }
            """
          )
        );
    }

    @Test
    void fullChainRenamesAllProperties() {
        rewriteRun(
          properties(
            """
            datanucleus.schema.autoCreateSchema=true
            datanucleus.transactionType=JTA
            datanucleus.jdoql.strict=true
            """,
            """
            datanucleus.schema.autoCreateDatabase=true
            datanucleus.transaction.type=JTA
            datanucleus.query.jdoql.strict=true
            """
          )
        );
    }
}
