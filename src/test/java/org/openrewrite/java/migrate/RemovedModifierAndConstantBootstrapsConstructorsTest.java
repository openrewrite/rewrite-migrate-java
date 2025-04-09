/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemovedModifierAndConstantBootstrapsConstructorsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/java-version-17.yml", "org.openrewrite.java.migrate.RemovedModifierAndConstantBootstrapsConstructors");
    }

    @DocumentExample
    @Test
    void moveToStaticTest() {
        rewriteRun(
          //language=java
          java(
            """
              import java.lang.invoke.ConstantBootstraps;
              import java.lang.reflect.Modifier;

              class RemovedModifierAndConstantBootstrapsConstructorsApp {
                   public void testModifier() throws Exception {
                       Modifier modifier = new Modifier();
                       modifier.classModifiers();
                       modifier.fieldModifiers();
                       modifier.isFinal(1);
                       modifier.isStatic(1);
                       Modifier.isPublic(0);
                   }
                   public void testConstantBootstraps() throws Exception {
                       ConstantBootstraps constantBootstraps = new ConstantBootstraps();
                       constantBootstraps.enumConstant(null,null,null);
                       constantBootstraps.primitiveClass(null,null,null);
                       ConstantBootstraps.nullConstant(null, null, null);
                   }
              }
              """,
            """
              import java.lang.invoke.ConstantBootstraps;
              import java.lang.reflect.Modifier;

              class RemovedModifierAndConstantBootstrapsConstructorsApp {
                   public void testModifier() throws Exception {
                       Modifier modifier = new Modifier();
                       Modifier.classModifiers();
                       Modifier.fieldModifiers();
                       Modifier.isFinal(1);
                       Modifier.isStatic(1);
                       Modifier.isPublic(0);
                   }
                   public void testConstantBootstraps() throws Exception {
                       ConstantBootstraps constantBootstraps = new ConstantBootstraps();
                       ConstantBootstraps.enumConstant(null,null,null);
                       ConstantBootstraps.primitiveClass(null,null,null);
                       ConstantBootstraps.nullConstant(null, null, null);
                   }
              }
              """
          )
        );
    }
}
