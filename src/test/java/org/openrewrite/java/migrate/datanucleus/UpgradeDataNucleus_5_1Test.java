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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.xml.Assertions.xml;

class UpgradeDataNucleus_5_1Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.datanucleus.UpgradeDataNucleus_5_1");
    }

    @DocumentExample
    @Test
    void renameTransactionProperties() {
        rewriteRun(
          properties(
            """
            datanucleus.nontransactionalRead=true
            datanucleus.nontransactionalWrite=false
            datanucleus.transactionType=JTA
            datanucleus.transactionIsolation=read-committed
            """,
            """
            datanucleus.transaction.nontx.read=true
            datanucleus.transaction.nontx.write=false
            datanucleus.transaction.type=JTA
            datanucleus.transaction.isolation=read-committed
            """
          )
        );
    }

    @Test
    void renameJtaAndCacheProperties() {
        rewriteRun(
          properties(
            """
            datanucleus.jtaLocator=autodetect
            datanucleus.jtaJndiLocation=java:comp/TransactionManager
            datanucleus.datastoreTransactionFlushLimit=1000
            datanucleus.cache.level2.timeout=3600000
            """,
            """
            datanucleus.transaction.jta.transactionManagerLocator=autodetect
            datanucleus.transaction.jta.transactionManagerJNDI=java:comp/TransactionManager
            datanucleus.flush.auto.objectLimit=1000
            datanucleus.cache.level2.expireMillis=3600000
            """
          )
        );
    }

    @Test
    void renameTransactionPropertiesInXml() {
        rewriteRun(
          xml(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <persistence>
              <persistence-unit name="test">
                <properties>
                  <property name="datanucleus.transactionType" value="JTA"/>
                  <property name="datanucleus.jtaLocator" value="autodetect"/>
                </properties>
              </persistence-unit>
            </persistence>
            """,
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <persistence>
              <persistence-unit name="test">
                <properties>
                  <property name="datanucleus.transaction.type" value="JTA"/>
                  <property name="datanucleus.transaction.jta.transactionManagerLocator" value="autodetect"/>
                </properties>
              </persistence-unit>
            </persistence>
            """
          )
        );
    }
}
