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

class UpgradeDataNucleus_5_0Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.datanucleus.UpgradeDataNucleus_5_0");
    }

    @DocumentExample
    @Test
    void renamePropertyKeys() {
        rewriteRun(
          properties(
            """
            datanucleus.query.compileOptimiser=true
            datanucleus.rdbms.adapter.informixUseSerialForIdentity=true
            datanucleus.rdbms.oracleNlsSortOrder=BINARY
            datanucleus.schema.autoCreateSchema=true
            """,
            """
            datanucleus.query.compileOptimiseVarThis=true
            datanucleus.rdbms.informix.useSerialForIdentity=true
            datanucleus.rdbms.oracle.nlsSortOrder=BINARY
            datanucleus.schema.autoCreateDatabase=true
            """
          )
        );
    }

    @Test
    void renamePropertyKeysInXml() {
        rewriteRun(
          xml(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <persistence>
              <persistence-unit name="test">
                <properties>
                  <property name="datanucleus.schema.autoCreateSchema" value="true"/>
                </properties>
              </persistence-unit>
            </persistence>
            """,
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <persistence>
              <persistence-unit name="test">
                <properties>
                  <property name="datanucleus.schema.autoCreateDatabase" value="true"/>
                </properties>
              </persistence-unit>
            </persistence>
            """
          )
        );
    }

    @Test
    void changeType() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .dependsOn(
              //language=java
              """
              package org.datanucleus.store.rdbms.adapter;
              public class MSSQLServerAdapter {}
              """,
              //language=java
              """
              package org.datanucleus.store.rdbms.adapter;
              public class SQLServerAdapter {}
              """
            )),
          //language=java
          java(
            """
            import org.datanucleus.store.rdbms.adapter.MSSQLServerAdapter;

            class A {
                MSSQLServerAdapter adapter;
            }
            """,
            """
            import org.datanucleus.store.rdbms.adapter.SQLServerAdapter;

            class A {
                SQLServerAdapter adapter;
            }
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
              package org.datanucleus.store.exceptions;
              public class SomeStoreException extends RuntimeException {}
              """,
              //language=java
              """
              package org.datanucleus.exceptions;
              public class SomeStoreException extends RuntimeException {}
              """
            )),
          //language=java
          java(
            """
            import org.datanucleus.store.exceptions.SomeStoreException;

            class A {
                void test() throws SomeStoreException {}
            }
            """,
            """
            import org.datanucleus.exceptions.SomeStoreException;

            class A {
                void test() throws SomeStoreException {}
            }
            """
          )
        );
    }
}
