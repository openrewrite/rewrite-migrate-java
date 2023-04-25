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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;

import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class UseJavaUtilBase64Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseJavaUtilBase64("test.sun.misc"))
          .parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            //language=java
            .dependsOn(
              """
                    package test.sun.misc;
                                                    
                    import java.io.InputStream;
                    import java.io.ByteArrayInputStream;
                    import java.io.OutputStream;
                    import java.io.ByteArrayOutputStream;
                    import java.io.PrintStream;
                    import java.io.IOException;
                    import java.nio.ByteBuffer;
                                 
                    @SuppressWarnings("RedundantThrows")
                    class CharacterEncoder {
                        public void encode(InputStream inStream, OutputStream outStream) throws IOException {
                        }

                        public void encode(byte[] aBuffer, OutputStream aStream) throws IOException {
                        }
                                                    
                        public String encode(byte[] aBuffer) {
                            return "";
                        }

                        public void encode(ByteBuffer aBuffer, OutputStream aStream) throws IOException {
                        }
                        
                        public String encode(ByteBuffer aBuffer) {
                            return "";
                        }
                                
                        public void encodeBuffer(InputStream inStream, OutputStream outStream) throws IOException {
                        }
                                                    
                        public void encodeBuffer(byte[] aBuffer, OutputStream aStream) throws IOException {
                        }
                                                    
                        public String encodeBuffer(byte[] aBuffer) {
                            return "";
                        }
                        
                        public void encodeBuffer(ByteBuffer aBuffer, OutputStream aStream) throws IOException {
                        }
                        
                        public String encodeBuffer(ByteBuffer aBuffer) {
                            return "";
                        }
                    }
                """,
              """
                  package test.sun.misc;
                                                  
                  import java.io.InputStream;
                  import java.io.ByteArrayInputStream;
                  import java.io.OutputStream;
                  import java.io.ByteArrayOutputStream;
                  import java.io.PrintStream;
                  import java.io.IOException;
                  import java.nio.ByteBuffer;
                  
                  @SuppressWarnings("RedundantThrows")
                  class CharacterDecoder {
                      public void decode(InputStream inStream, OutputStream outStream) throws IOException {
                      }
                  
                      public void decode(String inString, OutputStream outStream) throws IOException {
                      }
                                                  
                      public void decodeBuffer(InputStream inStream, OutputStream outStream) throws IOException {
                      }
                  
                      public void decodeBuffer(String inString, OutputStream outStream) throws IOException {
                      }
                                                  
                      public byte[] decodeBuffer(String inString) throws IOException {
                          return new byte[0];
                      }
                      
                      public byte[] decodeBuffer(InputStream inStream) throws IOException {
                          return new byte[0];
                      }
                  }
              """,
              """
                package test.sun.misc;
                class BASE64Encoder extends CharacterEncoder {
                }
              """,
              """
                package test.sun.misc;
                class BASE64Decoder extends CharacterDecoder {
                }
              """
            ));
    }

    @Test
    void encodeDecode() {
        rewriteRun(
          //language=java
          java(
            """
              package test.sun.misc;
                          
              import test.sun.misc.BASE64Encoder;
              import test.sun.misc.BASE64Decoder;
              import java.io.IOException;

              class Test {
                  void test(byte[] bBytes) {
                      BASE64Encoder encoder = new BASE64Encoder();
                      String encoded = encoder.encode(bBytes);
                      encoded += encoder.encodeBuffer(bBytes);
                      try {
                          byte[] decoded = new BASE64Decoder().decodeBuffer(encoded);
                      } catch (IOException e) {
                          e.printStackTrace();
                      }
                  }
              }
              """,
            """
              package test.sun.misc;
                          
              import java.util.Base64;
                          
              class Test {
                  void test(byte[] bBytes) {
                      Base64.Encoder encoder = Base64.getEncoder();
                      String encoded = encoder.encodeToString(bBytes);
                      encoded += encoder.encodeToString(bBytes);
                      byte[] decoded = Base64.getDecoder().decode(encoded);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/212")
    @Test
    void otherBase64() {
        //language=java
        rewriteRun(
          java("""
              package test.sun.misc;
              
              public class App {
                  public static void main(String[] args) {
                      String encode = new BASE64Encoder().encode(new byte[16]);
                  }
              }
              
              class Base64 {
              }
              """,
            """
              /*~~(Already using a class named Base64 other than java.util.Base64. Manual intervention required.)~~>*/package test.sun.misc;
              
              public class App {
                  public static void main(String[] args) {
                      String encode = new BASE64Encoder().encode(new byte[16]);
                  }
              }
              
              class Base64 {
              }
              """)
        );
    }
}
