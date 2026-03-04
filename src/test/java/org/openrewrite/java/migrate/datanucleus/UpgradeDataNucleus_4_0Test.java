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

class UpgradeDataNucleus_4_0Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.datanucleus.UpgradeDataNucleus_4_0");
    }

    @Test
    @DocumentExample
    void renameSchemaPropertyKeys() {
        rewriteRun(
          properties(
            """
            datanucleus.autoCreateSchema=true
            datanucleus.autoCreateTables=true
            datanucleus.autoCreateColumns=true
            datanucleus.autoCreateConstraints=true
            datanucleus.validateSchema=true
            datanucleus.validateTables=true
            datanucleus.validateColumns=true
            datanucleus.validateConstraints=true
            """,
            """
            datanucleus.schema.autoCreateAll=true
            datanucleus.schema.autoCreateTables=true
            datanucleus.schema.autoCreateColumns=true
            datanucleus.schema.autoCreateConstraints=true
            datanucleus.schema.validateAll=true
            datanucleus.schema.validateTables=true
            datanucleus.schema.validateColumns=true
            datanucleus.schema.validateConstraints=true
            """
          )
        );
    }

    @Test
    void renameMetadataAndOtherPropertyKeys() {
        rewriteRun(
          properties(
            """
            datanucleus.metadata.validate=true
            datanucleus.defaultInheritanceStrategy=JDO2
            datanucleus.managedRuntime=true
            """,
            """
            datanucleus.metadata.xml.validate=true
            datanucleus.metadata.defaultInheritanceStrategy=JDO2
            datanucleus.jmxType=true
            """
          )
        );
    }

    @Test
    void renameSchemaPropertiesInXml() {
        rewriteRun(
          xml(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <persistence>
              <persistence-unit name="test">
                <properties>
                  <property name="datanucleus.autoCreateSchema" value="true"/>
                  <property name="datanucleus.validateSchema" value="true"/>
                </properties>
              </persistence-unit>
            </persistence>
            """,
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <persistence>
              <persistence-unit name="test">
                <properties>
                  <property name="datanucleus.schema.autoCreateAll" value="true"/>
                  <property name="datanucleus.schema.validateAll" value="true"/>
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
              package org.datanucleus;
              public class PersistenceConfiguration {}
              """,
              //language=java
              """
              package org.datanucleus;
              public class Configuration {}
              """
            )),
          //language=java
          java(
            """
            import org.datanucleus.PersistenceConfiguration;

            class A {
                PersistenceConfiguration config;
            }
            """,
            """
            import org.datanucleus.Configuration;

            class A {
                Configuration config;
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
              package org.datanucleus.store.types.simple;
              public class SimpleList {}
              """,
              //language=java
              """
              package org.datanucleus.store.types.wrappers;
              public class SimpleList {}
              """
            )),
          //language=java
          java(
            """
            import org.datanucleus.store.types.simple.SimpleList;

            class A {
                SimpleList list;
            }
            """,
            """
            import org.datanucleus.store.types.wrappers.SimpleList;

            class A {
                SimpleList list;
            }
            """
          )
        );
    }
}
