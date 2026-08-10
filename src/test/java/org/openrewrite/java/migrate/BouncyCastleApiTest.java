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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class BouncyCastleApiTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("bcprov-jdk15on"));
    }

    private static RecipeSpec derStringRecipe(RecipeSpec spec) {
        return spec.recipeFromResource(
          "/META-INF/rewrite/bouncycastle-jdk18on.yml",
          "org.openrewrite.java.migrate.BouncyCastleDerStringGetInstanceReturnType");
    }

    @DocumentExample
    @Test
    void widenLocalVariableToAsn1String() {
        rewriteRun(
          BouncyCastleApiTest::derStringRecipe,
          //language=java
          java(
            """
              import org.bouncycastle.asn1.DERIA5String;

              class A {
                  String extract(Object o) {
                      DERIA5String value = DERIA5String.getInstance(o);
                      return value.getString();
                  }
              }
              """,
            """
              import org.bouncycastle.asn1.ASN1IA5String;
              import org.bouncycastle.asn1.DERIA5String;

              class A {
                  String extract(Object o) {
                      ASN1IA5String value = DERIA5String.getInstance(o);
                      return value.getString();
                  }
              }
              """
          )
        );
    }

    @Test
    void widenField() {
        rewriteRun(
          BouncyCastleApiTest::derStringRecipe,
          //language=java
          java(
            """
              import org.bouncycastle.asn1.DERPrintableString;

              class A {
                  private final DERPrintableString name = DERPrintableString.getInstance(new byte[0]);
              }
              """,
            """
              import org.bouncycastle.asn1.ASN1PrintableString;
              import org.bouncycastle.asn1.DERPrintableString;

              class A {
                  private final ASN1PrintableString name = DERPrintableString.getInstance(new byte[0]);
              }
              """
          )
        );
    }

    @Test
    void widenTaggedObjectOverload() {
        rewriteRun(
          BouncyCastleApiTest::derStringRecipe,
          //language=java
          java(
            """
              import org.bouncycastle.asn1.ASN1TaggedObject;
              import org.bouncycastle.asn1.DERUTF8String;

              class A {
                  DERUTF8String extract(ASN1TaggedObject tagged) {
                      DERUTF8String value = DERUTF8String.getInstance(tagged, true);
                      return value;
                  }
              }
              """,
            """
              import org.bouncycastle.asn1.ASN1TaggedObject;
              import org.bouncycastle.asn1.ASN1UTF8String;
              import org.bouncycastle.asn1.DERUTF8String;

              class A {
                  DERUTF8String extract(ASN1TaggedObject tagged) {
                      ASN1UTF8String value = DERUTF8String.getInstance(tagged, true);
                      return value;
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotTouchConstructorsBecauseAsn1TypesAreAbstract() {
        rewriteRun(
          BouncyCastleApiTest::derStringRecipe,
          //language=java
          java(
            """
              import org.bouncycastle.asn1.DERIA5String;

              class A {
                  DERIA5String create(String value) {
                      return new DERIA5String(value);
                  }
              }
              """
          )
        );
    }

    @Test
    void alreadyDeclaredAsAsn1String() {
        rewriteRun(
          // The upstream recipe re-attributes the invocation type on every match, so a cycle is consumed
          // even though the printed source is unchanged.
          spec -> derStringRecipe(spec).expectedCyclesThatMakeChanges(1),
          //language=java
          java(
            """
              import org.bouncycastle.asn1.ASN1IA5String;
              import org.bouncycastle.asn1.DERIA5String;

              class A {
                  String extract(Object o) {
                      ASN1IA5String value = DERIA5String.getInstance(o);
                      return value.getString();
                  }
              }
              """
          )
        );
    }

    @Test
    void sphincsPlusMovedToPqcLegacy() {
        rewriteRun(
          spec -> spec.recipeFromResource(
            "/META-INF/rewrite/bouncycastle-jdk18on.yml",
            "org.openrewrite.java.migrate.BouncyCastleSphincsPlusToPqcLegacy"),
          //language=java
          java(
            """
              import org.bouncycastle.pqc.crypto.sphincsplus.SPHINCSPlusParameters;
              import org.bouncycastle.pqc.crypto.sphincsplus.SPHINCSPlusSigner;

              class A {
                  SPHINCSPlusSigner signer = new SPHINCSPlusSigner();
                  SPHINCSPlusParameters parameters = SPHINCSPlusParameters.sha2_128f;
              }
              """,
            """
              import org.bouncycastle.pqc.legacy.sphincsplus.SPHINCSPlusParameters;
              import org.bouncycastle.pqc.legacy.sphincsplus.SPHINCSPlusSigner;

              class A {
                  SPHINCSPlusSigner signer = new SPHINCSPlusSigner();
                  SPHINCSPlusParameters parameters = SPHINCSPlusParameters.sha2_128f;
              }
              """
          )
        );
    }
}
