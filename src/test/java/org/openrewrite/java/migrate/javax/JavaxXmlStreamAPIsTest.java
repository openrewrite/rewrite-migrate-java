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
package org.openrewrite.java.migrate.javax;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class JavaxXmlStreamAPIsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/javax-xml-stream.yml", "org.openrewrite.java.migrate.javax.JavaxXmlStreamAPIs");
    }

    @DocumentExample
    @Test
    void xmlEventFactoryNewInstance() {
        //language=java
        rewriteRun(
          java(
            """
              import javax.xml.stream.XMLEventFactory;

              public class Test {
                  public void method() {
                      XMLEventFactory eventFactory = XMLEventFactory.newInstance("test", Test.class.getClassLoader());
                  }
              }
              """,
            """
              import javax.xml.stream.XMLEventFactory;

              public class Test {
                  public void method() {
                      XMLEventFactory eventFactory = XMLEventFactory.newFactory("test", Test.class.getClassLoader());
                  }
              }
              """
          )
        );
    }

    @Test
    void xmlInputFactoryNewInstance() {
        //language=java
        rewriteRun(
          java(
            """
              import javax.xml.stream.XMLInputFactory;

              public class Test {
                  public void method() {
                      XMLInputFactory inputFactory = XMLInputFactory.newInstance("test", Test.class.getClassLoader());
                  }
              }
              """,
            """
              import javax.xml.stream.XMLInputFactory;

              public class Test {
                  public void method() {
                      XMLInputFactory inputFactory = XMLInputFactory.newFactory("test", Test.class.getClassLoader());
                  }
              }
              """
          )
        );
    }

    @Test
    void xmlOutputFactoryNewInstance() {
        //language=java
        rewriteRun(
          java(
            """
              import javax.xml.stream.XMLOutputFactory;

              public class Test {
                  public void method() {
                      XMLOutputFactory outputFactory = XMLOutputFactory.newInstance("test", Test.class.getClassLoader());
                  }
              }
              """,
            """
              import javax.xml.stream.XMLOutputFactory;

              public class Test {
                  public void method() {
                      XMLOutputFactory outputFactory = XMLOutputFactory.newFactory("test", Test.class.getClassLoader());
                  }
              }
              """
          )
        );
    }
}
