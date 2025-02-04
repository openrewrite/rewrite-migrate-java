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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class ChangeDefaultKeyStoreTest implements RewriteTest {

    @Language("java")
    private static final String BEFORE = """
      import java.io.FileInputStream;
      import java.io.IOException;
      import java.security.Key;
      import java.security.KeyStore;

      class Foo {
       	void bar() {
       		try{
       			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
       			char[] password = "your_keystore_password".toCharArray();
       			FileInputStream keystoreFile = new FileInputStream("path_to_your_keystore_file.jks");
       			keystore.load(keystoreFile, password);
       		}
       		catch (Exception e) {
       			e.printStackTrace();
       		}
       	}
      }
      """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeDefaultKeyStore())
          .allSources(src -> src.markers(javaVersion(11)));
    }

    @DocumentExample
    @Test
    void keyStoreDefaultTypeChangedToExplicitType() {
        rewriteRun(
          //language=java
          java(
            BEFORE,
            """
              import java.io.FileInputStream;
              import java.io.IOException;
              import java.security.Key;
              import java.security.KeyStore;

              class Foo {
               	void bar() {
               		try{
               			KeyStore keystore = KeyStore.getInstance("jks");
               			char[] password = "your_keystore_password".toCharArray();
               			FileInputStream keystoreFile = new FileInputStream("path_to_your_keystore_file.jks");
               			keystore.load(keystoreFile, password);
               		}
               		catch (Exception e) {
               			e.printStackTrace();
               		}
               	}
              }
              """
          )
        );
    }

    @Test
    void keepExplicitType() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.FileInputStream;
              import java.io.IOException;
              import java.security.Key;
              import java.security.KeyStore;

              class Foo {
               	void bar() {
               		try{
               			KeyStore keystore = KeyStore.getInstance("jks");
               			char[] password = "your_keystore_password".toCharArray();
               			FileInputStream keystoreFile = new FileInputStream("path_to_your_keystore_file.jks");
               			keystore.load(keystoreFile, password);
               		}
               		catch (Exception e) {
               			e.printStackTrace();
               		}
               	}
              }
              """)
        );
    }

    @Test
    void keepStringForJava8() {
        rewriteRun(
          //language=java
          java(
            BEFORE,
            spec -> spec.markers(javaVersion(8)))
        );
    }

    @Test
    void keepStringForJava17() {
        rewriteRun(
          //language=java
          java(
            BEFORE,
            spec -> spec.markers(javaVersion(17)))
        );
    }
}
