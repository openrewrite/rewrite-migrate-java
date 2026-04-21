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
package org.openrewrite.java.migrate.util;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;
import static org.openrewrite.kotlin.Assertions.kotlin;

class MigrateCollectionsSingletonMapTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateCollectionsSingletonMap());
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/72")
    @Test
    void singletonMap() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.*;

                class Test {
                    Map<String,String> set = Collections.singletonMap("hello", "world");
                }
                """,
              """
                import java.util.Map;

                class Test {
                    Map<String,String> set = Map.of("hello", "world");
                }
                """
            ),
            9
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/72")
    @Test
    void singletonMapCustomType() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.*;
                import java.time.LocalDate;

                class Test {
                    Map<String,LocalDate> map = Collections.singletonMap("date", LocalDate.now());
                }
                """,
              """
                import java.util.Map;
                import java.time.LocalDate;

                class Test {
                    Map<String,LocalDate> map = Map.of("date", LocalDate.now());
                }
                """
            ),
            9
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/571")
    @Test
    void shouldNotConvertLiteralNull() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.*;

                class Test {
                    Map<String, String> mapWithNullKey = Collections.singletonMap(null, "foo");
                    Map<String, String> mapWithNullValue = Collections.singletonMap("bar", null);
                }
                """
            ),
            9
          )
        );
    }

    @Test
    void singletonMapAsArgument() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.*;

                class Test {
                    void take(Map<String, Object> m) {}
                    void call(String key, Object value) {
                        take(Collections.singletonMap(key, value));
                    }
                }
                """,
              """
                import java.util.Map;

                class Test {
                    void take(Map<String, Object> m) {}
                    void call(String key, Object value) {
                        take(Map.of(key, value));
                    }
                }
                """
            ),
            9
          )
        );
    }

    @Test
    void singletonMapInSwitchExpression() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.*;

                class Test {
                    Map<String, Object> get(int i) {
                        return switch (i) {
                            case 0 -> Collections.singletonMap("a", "b");
                            default -> null;
                        };
                    }
                }
                """,
              """
                import java.util.Map;

                class Test {
                    Map<String, Object> get(int i) {
                        return switch (i) {
                            case 0 -> Map.of("a", "b");
                            default -> null;
                        };
                    }
                }
                """
            ),
            9
          )
        );
    }

    @Test
    void doesNotModifyKotlinSources() {
        rewriteRun(
          version(
            //language=kotlin
            kotlin(
              """
                import java.util.Collections

                class Test {
                    fun make(key: Int, value: Int): Map<Int, Int> = Collections.singletonMap(key, value)
                }
                """
            ),
            9
          )
        );
    }

    @Test
    void singletonMapInTernaryWithMethodCallValue() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.*;

                class Converter { String convert(String o) { return "x"; } }

                class Test {
                    private final Converter converter = new Converter();

                    public Object beforeBodyWrite(Object body) {
                        return body instanceof String
                                ? Collections.singletonMap("alps", converter.convert((String) body))
                                : body;
                    }
                }
                """,
              """
                import java.util.Map;

                class Converter { String convert(String o) { return "x"; } }

                class Test {
                    private final Converter converter = new Converter();

                    public Object beforeBodyWrite(Object body) {
                        return body instanceof String
                                ? Map.of("alps", converter.convert((String) body))
                                : body;
                    }
                }
                """
            ),
            9
          )
        );
    }
}
