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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UpgradeSunJavaToJava11Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "sun.internal"))
          .recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate").build()
            .activateRecipes("org.openrewrite.java.migrate.Java8toJava11", "org.openrewrite.java.migrate.IBMSemeru"));
    }

    @Test
    void testInternalBindContextFactory() {
        //language=java
        rewriteRun(
          java(
            """
              public class TestInternalBindContextFactoryAPIs {
                  public void testInternalBindContextFactory() {
                      com.sun.xml.internal.bind.v2.ContextFactory contextFactory = null;
                      contextFactory.hashCode();
                  }
              }
              """,
            """
              public class TestInternalBindContextFactoryAPIs {
                  public void testInternalBindContextFactory() {
                      com.sun.xml.bind.v2.ContextFactory contextFactory = null;
                      contextFactory.hashCode();
                  }
              }
              """
          ),
          java(
            """
              import com.sun.xml.internal.bind.v2.ContextFactory;
                 
              public class TestInternalBindContextFactoryAPIs2 {
                public void testInternalBindContextFactory() {
                    ContextFactory factory = null;
                    factory.hashCode();
                }
              }
              """,
            """
              import com.sun.xml.bind.v2.ContextFactory;
                 
              public class TestInternalBindContextFactoryAPIs2 {
                public void testInternalBindContextFactory() {
                    ContextFactory factory = null;
                    factory.hashCode();
                }
              }
              """
          ),
          java(
            """
              import com.sun.xml.internal.bind.v2.*;
                
              public class TestInternalBindContextFactoryAPIs3 {
                public void testInternalBindContextFactory() {
                    ContextFactory factory = null;
                    factory.hashCode();
                }
                
              }
              """,
            """
              import com.sun.xml.bind.v2.ContextFactory;
              import com.sun.xml.internal.bind.v2.*;
                
              public class TestInternalBindContextFactoryAPIs3 {
                public void testInternalBindContextFactory() {
                    ContextFactory factory = null;
                    factory.hashCode();
                }
                
              }
              """
          )
        );
    }

    @Test
    void testJREDoNotUseSunNetSslInternalWwwProtocolHttpsHandler() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;
                            
              import com.sun.net.ssl.internal.www.protocol.https.*;  //do NOT flag this
                            
              public class TestClass_1{
                            
                public static void main(String[] args) {
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
              package com.test;
                            
              import com.ibm.net.ssl.www2.protocol.https.Handler;
              import com.sun.net.ssl.internal.www.protocol.https.*;  //do NOT flag this
                            
              public class TestClass_1{
                            
                public static void main(String[] args) {
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
    void testJREDoNotUseSunNetSslInternalWwwProtocol() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;
                            
              public class TestClass_1{
                private String flagMe = "com.sun.net.ssl.internal.www.protocol"; //flag this
                            
                public static void main(String[] args) {
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
              package com.test;
               
              public class TestClass_1{
                private String flagMe = "com.ibm.net.ssl.www2.protocol"; //flag this
                            
                public static void main(String[] args) {
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
    void testJREDoNotUseSunNetSslInternalSslProvider() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.net.ssl.internal.ssl.*;  // do NOT flag, handled by other rule
                            
              public class TestClass_2{
               
                public static void main(String[] args) {
                    Provider provider_4 = new Provider();  // flag (2)
                }
                            
                private void fdsa( Provider p1 ){} // flag
              }
              """,
            """
              import com.ibm.jsse2.IBMJSSEProvider2;
              import com.sun.net.ssl.internal.ssl.*;  // do NOT flag, handled by other rule
                            
              public class TestClass_2{
                            
                public static void main(String[] args) {
                    IBMJSSEProvider2 provider_4 = new IBMJSSEProvider2();  // flag (2)
                }
                            
                private void fdsa( IBMJSSEProvider2 p1 ){} // flag
              }
              """
          )
        );
    }

    @Test
    void testFullyQualifiedPackage() {
        rewriteRun(
          //language=java
          java(
            """
               package com.test;
               
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
               
               public class TestFullyQualifiedPackage {
               
               	public com.sun.net.ssl.HostnameVerifier hv;
               	public com.sun.net.ssl.HttpsURLConnection huc;
               	public com.sun.net.ssl.KeyManager km;
               	public com.sun.net.ssl.KeyManagerFactory kmf;
               	public com.sun.net.ssl.KeyManagerFactorySpi kmfs;
               	public com.sun.net.ssl.SSLContext sslc;
               	public com.sun.net.ssl.SSLContextSpi sslcs;
               	public com.sun.net.ssl.SSLPermission sslp;
               	public com.sun.net.ssl.TrustManager tm;
               	public com.sun.net.ssl.TrustManagerFactory tmf;
               	public com.sun.net.ssl.TrustManagerFactorySpi tmfs;
               	public com.sun.net.ssl.X509KeyManager x509km;
               	public com.sun.net.ssl.X509TrustManager xtm;
               	
               	public  HostnameVerifier hv2;
               	public  HttpsURLConnection huc2;
               	public  KeyManager km2;
               	public  KeyManagerFactory kmf2;
               	public  KeyManagerFactorySpi kmfs2;
               	public  SSLContext sslc2;
               	public  SSLContextSpi sslcs2;
               	public  SSLPermission sslp2;
               	public  TrustManager tm2;
               	public  TrustManagerFactory tmf2;
               	public  TrustManagerFactorySpi tmfs2;
               	public  X509KeyManager x509km2;
               	public  X509TrustManager xtm2;
               }
               """,
            """
              package com.test;
              
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
              
              public class TestFullyQualifiedPackage {
                             
              	public javax.net.ssl.HostnameVerifier hv;
              	public javax.net.ssl.HttpsURLConnection huc;
              	public javax.net.ssl.KeyManager km;
              	public javax.net.ssl.KeyManagerFactory kmf;
              	public javax.net.ssl.KeyManagerFactorySpi kmfs;
              	public javax.net.ssl.SSLContext sslc;
              	public javax.net.ssl.SSLContextSpi sslcs;
              	public javax.net.ssl.SSLPermission sslp;
              	public javax.net.ssl.TrustManager tm;
              	public javax.net.ssl.TrustManagerFactory tmf;
              	public javax.net.ssl.TrustManagerFactorySpi tmfs;
              	public javax.net.ssl.X509KeyManager x509km;
              	public javax.net.ssl.X509TrustManager xtm;
              	
              	public  HostnameVerifier hv2;
              	public  HttpsURLConnection huc2;
              	public  KeyManager km2;
              	public  KeyManagerFactory kmf2;
              	public  KeyManagerFactorySpi kmfs2;
              	public  SSLContext sslc2;
              	public  SSLContextSpi sslcs2;
              	public  SSLPermission sslp2;
              	public  TrustManager tm2;
              	public  TrustManagerFactory tmf2;
              	public  TrustManagerFactorySpi tmfs2;
              	public  X509KeyManager x509km2;
              	public  X509TrustManager xtm2;
              }
              """
          )
        );
    }

    @Test
    void testHostnameVerifier() {
        rewriteRun(
          //language=java
          java(
            """
               package com.test;
               
               import com.sun.net.ssl.HostnameVerifier;
               
               public class TestHostnameVerifier implements HostnameVerifier {
               
               	public TestHostnameVerifier() {
               		
               	}
               
               	public boolean verify(String arg0, String arg1) {
               		
               		return false;
               	}
               
               }
               """,
            """
               package com.test;
               
               import javax.net.ssl.HostnameVerifier;
               
               public class TestHostnameVerifier implements HostnameVerifier {
               
               	public TestHostnameVerifier() {
               		
               	}
               
               	public boolean verify(String arg0, String arg1) {
               		
               		return false;
               	}
               
               }
               """
          )
        );
    }
}
