/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.migrate.guava;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NoGuavaListsNewCopyOnWriteArrayListTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaListsNewCopyOnWriteArrayList())
          .parser(JavaParser.fromJavaVersion().classpath("guava"));
    }

    @DocumentExample
    @Test
    void replaceWithNewCopyOnWriteArrayList() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.*;

              import java.util.List;

              class Test {
                  List<Integer> cardinalsWorldSeries = Lists.newCopyOnWriteArrayList();
              }
              """,
            """
              import java.util.List;
              import java.util.concurrent.CopyOnWriteArrayList;

              class Test {
                  List<Integer> cardinalsWorldSeries = new CopyOnWriteArrayList<>();
              }
              """
          )
        );
    }

    @Test
    void replaceWithNewCopyOnWriteArrayListCollection() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.*;

              import java.util.Collections;
              import java.util.List;

              class Test {
                  List<Integer> l = Collections.emptyList();
                  List<Integer> cardinalsWorldSeries = Lists.newCopyOnWriteArrayList(l);
              }
              """,
            """
              import java.util.Collections;
              import java.util.List;
              import java.util.concurrent.CopyOnWriteArrayList;

              class Test {
                  List<Integer> l = Collections.emptyList();
                  List<Integer> cardinalsWorldSeries = new CopyOnWriteArrayList<>(l);
              }
              """
          )
        );
    }
}
