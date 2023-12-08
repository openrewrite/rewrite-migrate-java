/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class CastArraysAsListToListTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new CastArraysAsListToList());
    }

    @Test
    public void positiveCases() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              import java.util.Arrays;
              import java.util.List;
                            
              public class Main {
                  public static void main(String[] args) {
                      Integer[] array1 = (Integer[]) Arrays.asList(1, 2, 3).toArray();
                      Integer[][] array2 = (Integer[][]) Arrays.asList(new Integer[]{1}, new Integer[]{2}).toArray();
                      Object[][] array3 = (Object[][]) Arrays.asList(new Object[]{}, new Object[]{}).toArray();
                  }
              }
              """,

            """
              import java.util.ArrayList;
              import java.util.Arrays;
              import java.util.List;
                            
              public class Main {
                  public static void main(String[] args) {
                      Integer[] array1 = Arrays.asList(1, 2, 3).toArray(new Integer[0]);
                      Integer[][] array2 = Arrays.asList(new Integer[]{1}, new Integer[]{2}).toArray(new Integer[0][]);
                      Object[][] array3 = Arrays.asList(new Object[]{}, new Object[]{}).toArray(new Object[0][]);
                  }
              }
              """
          )
        );
    }

    @Test
    public void negativeCases() {
        //language=java
        rewriteRun(
          java("""
            import java.util.Arrays;
            import java.util.Collections;
            public class Main {

                public static void main(String[] args) {
                    Object[] array1 = (Object[]) Arrays.asList("a","b").toArray();
                    Integer[] array2 = (Integer[]) Collections.singletonList(1).toArray();
                    Integer[] array3 = Arrays.asList(1, 2, 3).toArray(new Integer[0]);
                }
            }
            """
          )
        );
    }
}
