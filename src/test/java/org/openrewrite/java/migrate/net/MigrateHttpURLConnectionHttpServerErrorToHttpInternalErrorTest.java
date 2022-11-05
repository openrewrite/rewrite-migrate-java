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
package org.openrewrite.java.migrate.net;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("deprecation")
class MigrateHttpURLConnectionHttpServerErrorToHttpInternalErrorTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateHttpURLConnectionHttpServerErrorToHttpInternalError());
    }

    @Test
    void httpUrlConnectionHttpServerErrorToHttpInternalError() {
        //language=java
        rewriteRun(
          java(
            """
              import java.net.HttpURLConnection;

              class Test {
                  private static final int ERROR = HttpURLConnection.HTTP_SERVER_ERROR;

                  public static int method() {
                      return HttpURLConnection.HTTP_SERVER_ERROR;
                  }
              }
              """,
            """
              import java.net.HttpURLConnection;

              class Test {
                  private static final int ERROR = HttpURLConnection.HTTP_INTERNAL_ERROR;

                  public static int method() {
                      return HttpURLConnection.HTTP_INTERNAL_ERROR;
                  }
              }
              """
          )
        );
    }

    @Test
    void httpUrlConnectionHttpServerErrorToHttpInternalErrorAsStaticImport() {
        //language=java
        rewriteRun(
          java(
            """
              import static java.net.HttpURLConnection.HTTP_SERVER_ERROR;

              class Test {
                  private static final int ERROR = HTTP_SERVER_ERROR;

                  public static int method() {
                      return HTTP_SERVER_ERROR;
                  }
              }
              """,
            """
              import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;

              class Test {
                  private static final int ERROR = HTTP_INTERNAL_ERROR;

                  public static int method() {
                      return HTTP_INTERNAL_ERROR;
                  }
              }
              """
          )
        );
    }

    @Test
    void httpUrlConnectionHttpServerErrorToHttpInternalErrorTypeCheckStaticImport() {
        //language=java
        rewriteRun(
          java(
            """
              public class LocalError {
                  public static final int HTTP_SERVER_ERROR = 500;
              }
              """
          ),
          java(
            """
              import static java.net.HttpURLConnection.HTTP_SERVER_ERROR;

              class Test {
                  private static final int ERROR = HTTP_SERVER_ERROR;
                  private static final int LOCAL_ERROR = LocalError.HTTP_SERVER_ERROR;

                  public static int method() {
                      return HTTP_SERVER_ERROR;
                  }
              }
              """,
            """
              import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;

              class Test {
                  private static final int ERROR = HTTP_INTERNAL_ERROR;
                  private static final int LOCAL_ERROR = LocalError.HTTP_SERVER_ERROR;

                  public static int method() {
                      return HTTP_INTERNAL_ERROR;
                  }
              }
              """
          )
        );
    }
}
