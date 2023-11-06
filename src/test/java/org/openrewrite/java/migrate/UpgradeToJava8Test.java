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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class UpgradeToJava8Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/java-version-8.yml", "org.openrewrite.java.migrate.UpgradeToJava8")
          .allSources(src -> src.markers(javaVersion(8)));
    }

    @Test
    void classAndNonPublicInterface() {
        rewriteRun(
          //language=java
          java(
            """
              import javax.management.InstanceAlreadyExistsException;
              import javax.management.MBeanException;
              import javax.management.MBeanRegistrationException;
              import javax.management.MBeanServer;
              import javax.management.MXBean;
              import javax.management.NotCompliantMBeanException;
              import javax.management.ObjectName;
              import javax.management.ReflectionException;
              
              class TestClassAndNonPublicInterface {
                  public void regMBean(ObjectName objectName, MBeanServer server) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
                      Object obj = new NonPublic();
                      server.registerMBean(obj, objectName);
                  }
              
                  public void createMBean(ObjectName objectName, MBeanServer server) throws InstanceAlreadyExistsException, NotCompliantMBeanException, ReflectionException, MBeanException {
                      server.createMBean("bean", objectName);
                  }
              
                  class NonPublic implements NonPublicMBean {
                  }
              
                  @MXBean
                  private interface NonPublicMBean {
                  }
              }
              """,
            """
              import javax.management.InstanceAlreadyExistsException;
              import javax.management.MBeanException;
              import javax.management.MBeanRegistrationException;
              import javax.management.MBeanServer;
              import javax.management.MXBean;
              import javax.management.NotCompliantMBeanException;
              import javax.management.ObjectName;
              import javax.management.ReflectionException;
                             
              class TestClassAndNonPublicInterface {
                  public void regMBean(ObjectName objectName, MBeanServer server) throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
                      Object obj = new NonPublic();
                      server.registerMBean(obj, objectName);
                  }
              
                  public void createMBean(ObjectName objectName, MBeanServer server) throws InstanceAlreadyExistsException, NotCompliantMBeanException, ReflectionException, MBeanException {
                      server.createMBean("bean", objectName);
                  }
              
                  class NonPublic implements NonPublicMBean {
                  }
              
                  @MXBean
                  public interface NonPublicMBean {
                  }
              }
              """
          )
        );
    }

    @Test
    void nonPublicInterfaceAnnotationMXBean() {
        rewriteRun(
          //language=java
          java(
            """
              import javax.management.MXBean;
                                
              // flag this interface
                                
              @MXBean
              interface TestNonPublicInterfaceAnnotationMXBean {
                                
              }
              """,
            """
              import javax.management.MXBean;
                             
              // flag this interface
                             
              @MXBean
              public
              interface TestNonPublicInterfaceAnnotationMXBean {
                             
              }
              """
          )
        );
    }

    @Test
    void nonPublicInterfaceAnnotationTrueMXBean() {
        rewriteRun(
          //language=java
          java(
            """
              import javax.management.MXBean;
                                
              // flag this interface
                                
              @MXBean(true)
              interface TestNonPublicInterfaceAnnotationTrueMXBean {
                                
              }
              """,
            """
              import javax.management.MXBean;
                             
              // flag this interface
                             
              @MXBean(true)
              public
              interface TestNonPublicInterfaceAnnotationTrueMXBean {
                             
              }
              """
          )
        );
    }

    @Test
    void nonPublicInterfaceAnnotationValueTrueMXBean() {
        rewriteRun(
          //language=java
          java(
            """
              // flag this interface
                                
              @javax.management.MXBean(value=true)
              interface TestNonPublicInterfaceAnnotationValueTrueMXBean {
                                
              }
              """,
            """
              // flag this interface
                             
              @javax.management.MXBean(value = true)
              public
              interface TestNonPublicInterfaceAnnotationValueTrueMXBean {
                             
              }
              """
          )
        );
    }

    @Test
    void nonPublicInterfaceMBean() {
        rewriteRun(
          //language=java
          java(
            """
              // flag this interface
                                
              abstract interface TestNonPublicInterfaceMBean {
                                
              }
              """,
            """
              // flag this interface
                             
              public abstract interface TestNonPublicInterfaceMBean {
                             
              }
              """
          )
        );
    }

    @Test
    void nonPublicInterfaceMXBean() {
        rewriteRun(
          //language=java
          java(
            """
              // flag this interface
                                
              interface TestNonPublicInterfaceMXBean {
                                
              }
              """,
            """
              // flag this interface
                             
              public interface TestNonPublicInterfaceMXBean {
                             
              }
              """
          )
        );
    }

    @Nested
    class NoChange {
        @Test
        void nonPublicInterface() {
            rewriteRun(
              //language=java
              java(
                """
                  // don't flag this interface
                                    
                  abstract interface TestNonPublicInterface {
                                    
                  }
                  """
              )
            );
        }

        @Test
        void publicInterfaceAnnotationFalseMXBean() {
            rewriteRun(
              //language=java
              java(
                """
                  import javax.management.MXBean;
                                    
                  // don't flag this interface
                                    
                  @MXBean(false)
                  public interface TestPublicInterfaceAnnotationFalseMXBean {
                                    
                  }
                  """
              )
            );
        }

        @Test
        void publicInterfaceAnnotationTrueMXBean() {
            rewriteRun(
              //language=java
              java(
                """
                  import javax.management.MXBean;
                                    
                  // don't flag this interface
                                    
                  @MXBean(true)
                  public interface TestPublicInterfaceAnnotationTrueMXBean {
                                    
                  }
                  """
              )
            );
        }

        @Test
        void publicInterfaceAnnotationValueFalseMXBean() {
            rewriteRun(
              //language=java
              java(
                """
                  import javax.management.MXBean;
                                    
                  // don't flag this interface
                                    
                  @MXBean(value=false)
                  public interface TestPublicInterfaceAnnotationValueFalseMXBean {
                                    
                  }
                  """
              )
            );
        }

        @Test
        void publicInterfaceAnnotationValueTrueMXBean() {
            rewriteRun(
              //language=java
              java(
                """
                  import javax.management.MXBean;
                                    
                  // don't flag this interface
                                    
                  @MXBean(value=true)
                  public interface TestPublicInterfaceAnnotationValueTrueMXBean {
                                    
                  }
                  """
              )
            );
        }

        @Test
        void publicInterfaceMBean() {
            rewriteRun(
              //language=java
              java(
                """
                  // don't flag this interface
                                    
                  public interface TestPublicInterfaceMBean {
                                    
                  }
                  """
              )
            );
        }

        @Test
        void publicInterfaceMXBean() {
            rewriteRun(
              //language=java
              java(
                """
                  // don't flag this interface
                                    
                  public interface TestPublicInterfaceMXBean {
                                    
                  }
                  """
              )
            );
        }
    }
}