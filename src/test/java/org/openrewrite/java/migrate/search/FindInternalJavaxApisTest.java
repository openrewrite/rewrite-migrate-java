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
package org.openrewrite.java.migrate.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FindInternalJavaxApisTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindInternalJavaxApis(null));
    }

    @DocumentExample
    @Test
    void returnsJavaxApi() {
        //language=java
        rewriteRun(
          java(
            """
              package org.openrewrite;
              
              interface Api {
                  javax.xml.stream.StreamFilter test();
              }
              """
          ),
          java(
            """
              package org.openrewrite;
              
              import javax.xml.stream.StreamFilter;
              
              class Consumer {
                  void test(Api api) {
                      StreamFilter sf = api.test();
                  }
              }
              """,
            """
              package org.openrewrite;
              
              import javax.xml.stream.StreamFilter;
              
              class Consumer {
                  void test(Api api) {
                      StreamFilter sf = /*~~>*/api.test();
                  }
              }
              """
          )
        );
    }

    @Test
    void usesJavaxApiInParameter() {
        //language=java
        rewriteRun(
          java(
            """
              package org.openrewrite;
              
              interface Api {
                  void test(javax.xml.stream.StreamFilter sf);
              }
              """
          ),
          java(
            """
              package org.openrewrite;
              
              import javax.xml.stream.StreamFilter;
              
              class Consumer {
                  void test(Api api, StreamFilter sf) {
                      api.test(sf);
                  }
              }
              """,
            """
              package org.openrewrite;
              
              import javax.xml.stream.StreamFilter;
              
              class Consumer {
                  void test(Api api, StreamFilter sf) {
                      /*~~>*/api.test(sf);
                  }
              }
              """
          )
        );
    }
}
