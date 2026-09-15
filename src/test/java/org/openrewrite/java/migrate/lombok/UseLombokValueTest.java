/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.lombok;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseLombokValueTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseLombokValue());
    }

    @DocumentExample
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1046")
    @Test
    void replaceValueClassBoilerplate() {
        rewriteRun(
          //language=java
          java(
            """
              public final class Person {
                  private final String name;
                  private final int age;

                  public Person(String name, int age) {
                      this.name = name;
                      this.age = age;
                  }

                  public String getName() {
                      return name;
                  }

                  public int getAge() {
                      return age;
                  }

                  @Override
                  public boolean equals(Object o) {
                      if (this == o) {
                          return true;
                      }
                      if (o == null || getClass() != o.getClass()) {
                          return false;
                      }
                      Person person = (Person) o;
                      return age == person.age && name.equals(person.name);
                  }

                  @Override
                  public int hashCode() {
                      int result = name.hashCode();
                      return 31 * result + age;
                  }

                  @Override
                  public String toString() {
                      return name + age;
                  }
              }
              """,
            """
              import lombok.Value;

              @Value
              public class Person {
                  String name;
                  int age;

                  @Override
                  public boolean equals(Object o) {
                      if (this == o) {
                          return true;
                      }
                      if (o == null || getClass() != o.getClass()) {
                          return false;
                      }
                      Person person = (Person) o;
                      return age == person.age && name.equals(person.name);
                  }

                  @Override
                  public int hashCode() {
                      int result = name.hashCode();
                      return 31 * result + age;
                  }

                  @Override
                  public String toString() {
                      return name + age;
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotReplaceVarargsConstructor() {
        rewriteRun(
          //language=java
          java(
            """
              final class Person {
                  private final String[] names;

                  public Person(String... names) {
                      this.names = names;
                  }

                  public String[] getNames() {
                      return names;
                  }

                  @Override
                  public boolean equals(Object o) {
                      return o instanceof Person && ((Person) o).names == names;
                  }

                  @Override
                  public int hashCode() {
                      return 1;
                  }

                  @Override
                  public String toString() {
                      return Integer.toString(names.length);
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotReplaceClassWithoutAllObjectMethods() {
        rewriteRun(
          //language=java
          java(
            """
              final class Person {
                  private final String name;

                  public Person(String name) {
                      this.name = name;
                  }

                  public String getName() {
                      return name;
                  }

                  @Override
                  public boolean equals(Object o) {
                      return o instanceof Person && ((Person) o).name.equals(name);
                  }

                  @Override
                  public int hashCode() {
                      return 1;
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotReplaceGettersWithIncompatibleSignatures() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.IOException;

              final class CheckedExceptionGetter {
                  private final String name;

                  public CheckedExceptionGetter(String name) {
                      this.name = name;
                  }

                  public String getName() throws IOException {
                      return name;
                  }

                  @Override
                  public boolean equals(Object o) {
                      return o instanceof CheckedExceptionGetter && ((CheckedExceptionGetter) o).name.equals(name);
                  }

                  @Override
                  public int hashCode() {
                      return 1;
                  }

                  @Override
                  public String toString() {
                      return name;
                  }
              }

              final class GenericGetter {
                  private final String name;

                  public GenericGetter(String name) {
                      this.name = name;
                  }

                  public <T> String getName() {
                      return name;
                  }

                  @Override
                  public boolean equals(Object o) {
                      return o instanceof GenericGetter && ((GenericGetter) o).name.equals(name);
                  }

                  @Override
                  public int hashCode() {
                      return 1;
                  }

                  @Override
                  public String toString() {
                      return name;
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotReplaceClassWithAccessors() {
        rewriteRun(
          //language=java
          java(
            """
              import lombok.experimental.Accessors;

              @Accessors(fluent = true)
              final class Person {
                  private final String name;

                  public Person(String name) {
                      this.name = name;
                  }

                  public String getName() {
                      return name;
                  }

                  @Override
                  public boolean equals(Object o) {
                      return o instanceof Person && ((Person) o).name.equals(name);
                  }

                  @Override
                  public int hashCode() {
                      return 1;
                  }

                  @Override
                  public String toString() {
                      return name;
                  }
              }
              """
          )
        );
    }
}
