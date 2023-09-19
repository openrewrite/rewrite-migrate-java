package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.java.JavaParser;

import static org.openrewrite.java.Assertions.java;

public class UpgradeSunJavaToJava11Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "sun.internal"))
          .recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate").build()
            .activateRecipes("org.openrewrite.java.migrate.Java8toJava11"));
    }

    @Test
    void testInternalBindContextFactory() {
        rewriteRun(
          java(
            """
              package com.ibm.test;
               
               public class TestInternalBindContextFactoryAPIs {
                   public void testInternalBindContextFactory() {
                       com.sun.xml.internal.bind.v2.ContextFactory contextFactory = null;
                       contextFactory.hashCode();
                   }
               }
              """,
            """
              package com.ibm.test;
               
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
              package com.ibm.test;
                 
              import com.sun.xml.internal.bind.v2.ContextFactory;
                 
              public class TestInternalBindContextFactoryAPIs2 {
                public void testInternalBindContextFactory() {
                 
                    ContextFactory factory = null;
                    factory.hashCode();
                 
                }
                 
              }
              """,
            """
              package com.ibm.test;
                 
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
              package com.ibm.test;
                
              import com.sun.xml.internal.bind.v2.*;
                
              public class TestInternalBindContextFactoryAPIs3 {
                public void testInternalBindContextFactory() {
                    ContextFactory factory = null;
                    factory.hashCode();
                		
                }
                
              }
              """,
            """
              package com.ibm.test;
                
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
               	
               	public static com.sun.net.ssl.internal.www.protocol.https.Handler testMethod(Handler handler){	//flag (2)
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
               		if (handler_1 instanceof com.sun.net.ssl.internal.www.protocol.https.Handler){ //flag
                        //do nothing
               		}
               		
               		if (handler_1 instanceof Handler){ //flag
                        //do nothing
               		}
               	}
               	
               	public static com.ibm.net.ssl.www2.protocol.https.Handler testMethod(Handler handler){	//flag (2)
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
          java(
            """
              package com.test;
               
              import com.sun.net.ssl.internal.ssl.*;  // do NOT flag, handled by other rule
               
              public class TestClass_2{
               
                public static void main(String[] args) {
                    Provider provider_4 = new Provider();  // flag (2)
                }
               	
                private void fdsa( Provider p1 ){} // flag
              }
              """,
            """
              package com.test;
               
              import com.ibm.jsse2.IBMJSSEProvider2;
              import com.sun.net.ssl.internal.ssl.*;  // do NOT flag, handled by other rule
               
              public class TestClass_2{
               
                public static void main(String[] args) {
                    IBMJSSEProvider2 provider_4 = new Provider();  // flag (2)
                }
               	
                private void fdsa( IBMJSSEProvider2 p1 ){} // flag
              }
              """
          )
        );
    }
}