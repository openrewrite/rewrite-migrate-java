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
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class IBMSemeruTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "sun.internal.new"))
          .recipeFromResource("/META-INF/rewrite/ibm-java.yml", "org.openrewrite.java.migrate.IBMSemeru");
    }

    @DocumentExample
    @Test
    void doNotUseSunNetSslInternalWwwProtocolHttpsHandler() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.net.ssl.internal.www.protocol.https.*;  //do NOT flag this
                            
              class Foo{
                void bar() {
                    com.sun.net.ssl.internal.www.protocol.https.Handler handler_1 =           //flag
                        new com.sun.net.ssl.internal.www.protocol.https.Handler();            //flag
                    Handler handler_2 =   new Handler("String", 1); //flag (2)
                    testMethod(handler_1);
                    testMethod(handler_2);
                    if (handler_1 instanceof com.sun.net.ssl.internal.www.protocol.https.Handler){ //flag
                        //do nothing
                    }
                            
                    if (handler_1 instanceof Handler){ //flag
                        //do nothing
                    }
                }
                            
                public static com.sun.net.ssl.internal.www.protocol.https.Handler testMethod(Handler handler){ //flag (2)
                    return handler;
                }
              }
              """,
            """
              import com.ibm.net.ssl.www2.protocol.https.Handler;
              import com.sun.net.ssl.internal.www.protocol.https.*;  //do NOT flag this
                            
              class Foo{
                void bar() {
                    com.ibm.net.ssl.www2.protocol.https.Handler handler_1 =           //flag
                        new com.ibm.net.ssl.www2.protocol.https.Handler();            //flag
                    Handler handler_2 =   new Handler("String", 1); //flag (2)
                    testMethod(handler_1);
                    testMethod(handler_2);
                    if (handler_1 instanceof com.ibm.net.ssl.www2.protocol.https.Handler){ //flag
                        //do nothing
                    }
                            
                    if (handler_1 instanceof Handler){ //flag
                        //do nothing
                    }
                }
                            
                public static com.ibm.net.ssl.www2.protocol.https.Handler testMethod(Handler handler){ //flag (2)
                    return handler;
                }
              }
              """
          )
        );
    }

    @Test
    void doNotUseSunNetSslInternalWwwProtocol() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo{
                private String flagMe = "com.sun.net.ssl.internal.www.protocol"; //flag this
                            
                void bar() {
                    System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol"); //flag this
                    String s1 = "com.sun.net.ssl";                              //DO NOT FLAG
                    String s2 = "com.sun.net.ssl.internal";                     //DO NOT FLAG
                    String s3 = "com.sun.net.ssl.internal.ssl";                 //DO NOT FLAG
                    String s4 = "com.sun.net.ssl.internal.www";                 //DO NOT FLAG
                    String s5 = "com.sun.net.ssl.internal.www.protocol";        //flag this
                    String s6 = "com.sun.net.ssl.internal.www.protocol.https";  //DO NOT FLAG
                }
              }
              """,
            """
              class Foo{
                private String flagMe = "com.ibm.net.ssl.www2.protocol"; //flag this
                            
                void bar() {
                    System.setProperty("java.protocol.handler.pkgs", "com.ibm.net.ssl.www2.protocol"); //flag this
                    String s1 = "com.sun.net.ssl";                              //DO NOT FLAG
                    String s2 = "com.sun.net.ssl.internal";                     //DO NOT FLAG
                    String s3 = "com.sun.net.ssl.internal.ssl";                 //DO NOT FLAG
                    String s4 = "com.sun.net.ssl.internal.www";                 //DO NOT FLAG
                    String s5 = "com.ibm.net.ssl.www2.protocol";        //flag this
                    String s6 = "com.sun.net.ssl.internal.www.protocol.https";  //DO NOT FLAG
                }
              }
              """
          )
        );
    }

    @Test
    void doNotUseSunNetSslInternalSslProvider() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.net.ssl.internal.ssl.*;  // do NOT flag, handled by other rule
                            
              class TestClass_2{
                void bar() {
                    Provider provider_4 = new Provider();  // flag (2)
                }
                            
                private void fdsa( Provider p1 ){} // flag
              }
              """,
            """
              import com.ibm.jsse2.IBMJSSEProvider2;
              import com.sun.net.ssl.internal.ssl.*;  // do NOT flag, handled by other rule
                            
              class TestClass_2{
                void bar() {
                    IBMJSSEProvider2 provider_4 = new IBMJSSEProvider2();  // flag (2)
                }
                            
                private void fdsa( IBMJSSEProvider2 p1 ){} // flag
              }
              """
          )
        );
    }

    @Test
    void fullyQualifiedPackage() {
        rewriteRun(
          version(
            //language=java
            java("""
                import com.sun.net.ssl.HostnameVerifier;
                import com.sun.net.ssl.HttpsURLConnection;
                import com.sun.net.ssl.KeyManager;
                import com.sun.net.ssl.KeyManagerFactory;
                import com.sun.net.ssl.KeyManagerFactorySpi;
                import com.sun.net.ssl.SSLContext;
                import com.sun.net.ssl.SSLContextSpi;
                import com.sun.net.ssl.SSLPermission;
                import com.sun.net.ssl.TrustManager;
                import com.sun.net.ssl.TrustManagerFactory;
                import com.sun.net.ssl.TrustManagerFactorySpi;
                import com.sun.net.ssl.X509KeyManager;
                import com.sun.net.ssl.X509TrustManager;
                               
                class TestFullyQualifiedPackage {
                    com.sun.net.ssl.HostnameVerifier hv;
                    com.sun.net.ssl.HttpsURLConnection huc;
                    com.sun.net.ssl.KeyManager km;
                    com.sun.net.ssl.KeyManagerFactory kmf;
                    com.sun.net.ssl.KeyManagerFactorySpi kmfs;
                    com.sun.net.ssl.SSLContext sslc;
                    com.sun.net.ssl.SSLContextSpi sslcs;
                    com.sun.net.ssl.SSLPermission sslp;
                    com.sun.net.ssl.TrustManager tm;
                    com.sun.net.ssl.TrustManagerFactory tmf;
                    com.sun.net.ssl.TrustManagerFactorySpi tmfs;
                    com.sun.net.ssl.X509KeyManager x509km;
                    com.sun.net.ssl.X509TrustManager xtm;
                                
                    HostnameVerifier hv2;
                    HttpsURLConnection huc2;
                    KeyManager km2;
                    KeyManagerFactory kmf2;
                    KeyManagerFactorySpi kmfs2;
                    SSLContext sslc2;
                    SSLContextSpi sslcs2;
                    SSLPermission sslp2;
                    TrustManager tm2;
                    TrustManagerFactory tmf2;
                    TrustManagerFactorySpi tmfs2;
                    X509KeyManager x509km2;
                    X509TrustManager xtm2;
                }
                """,
              """
                import javax.net.ssl.HostnameVerifier;
                import javax.net.ssl.HttpsURLConnection;
                import javax.net.ssl.KeyManager;
                import javax.net.ssl.KeyManagerFactory;
                import javax.net.ssl.KeyManagerFactorySpi;
                import javax.net.ssl.SSLContext;
                import javax.net.ssl.SSLContextSpi;
                import javax.net.ssl.SSLPermission;
                import javax.net.ssl.TrustManager;
                import javax.net.ssl.TrustManagerFactory;
                import javax.net.ssl.TrustManagerFactorySpi;
                import javax.net.ssl.X509KeyManager;
                import javax.net.ssl.X509TrustManager;
                              
                class TestFullyQualifiedPackage {
                    javax.net.ssl.HostnameVerifier hv;
                    javax.net.ssl.HttpsURLConnection huc;
                    javax.net.ssl.KeyManager km;
                    javax.net.ssl.KeyManagerFactory kmf;
                    javax.net.ssl.KeyManagerFactorySpi kmfs;
                    javax.net.ssl.SSLContext sslc;
                    javax.net.ssl.SSLContextSpi sslcs;
                    javax.net.ssl.SSLPermission sslp;
                    javax.net.ssl.TrustManager tm;
                    javax.net.ssl.TrustManagerFactory tmf;
                    javax.net.ssl.TrustManagerFactorySpi tmfs;
                    javax.net.ssl.X509KeyManager x509km;
                    javax.net.ssl.X509TrustManager xtm;
                                
                    HostnameVerifier hv2;
                    HttpsURLConnection huc2;
                    KeyManager km2;
                    KeyManagerFactory kmf2;
                    KeyManagerFactorySpi kmfs2;
                    SSLContext sslc2;
                    SSLContextSpi sslcs2;
                    SSLPermission sslp2;
                    TrustManager tm2;
                    TrustManagerFactory tmf2;
                    TrustManagerFactorySpi tmfs2;
                    X509KeyManager x509km2;
                    X509TrustManager xtm2;
                }
                """
            ), 6)
        );
    }

    @Test
    void hostnameVerifier() {
        rewriteRun(
          version(
            //language=java
            java("""
                import com.sun.net.ssl.HostnameVerifier;
                               
                class TestHostnameVerifier implements HostnameVerifier {
                    public boolean verify(String arg0, String arg1) {
                        return false;
                    }
                }
                """,
              """
                import javax.net.ssl.HostnameVerifier;
                               
                class TestHostnameVerifier implements HostnameVerifier {
                    public boolean verify(String arg0, String arg1) {
                        return false;
                    }
                }
                """
            ), 6)
        );
    }
}
