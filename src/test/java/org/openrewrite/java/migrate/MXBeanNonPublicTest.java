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

class MXBeanNonPublicTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MXBeanNonPublic()).allSources(src -> src.markers(javaVersion(8)));
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
                                
              @MXBean
              interface TestNonPublicInterfaceAnnotationMXBean {
                                
              }
              """,
            """
              import javax.management.MXBean;
                             
              @MXBean
              public interface TestNonPublicInterfaceAnnotationMXBean {
                             
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
                                
              @MXBean(true)
              interface TestNonPublicInterfaceAnnotationTrueMXBean {
                                
              }
              """,
            """
              import javax.management.MXBean;
                             
              @MXBean(true)
              public interface TestNonPublicInterfaceAnnotationTrueMXBean {
                             
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
              @javax.management.MXBean(value=true)
              interface TestNonPublicInterfaceAnnotationValueTrueMXBean {
                                
              }
              """,
            """
              @javax.management.MXBean(value = true)
              public interface TestNonPublicInterfaceAnnotationValueTrueMXBean {
                             
              }
              """
          )
        );
    }

    @Test
    void mbeanSuffix() {
        rewriteRun(
          //language=java
          java(
            """
              interface TestNonPublicInterfaceMBean {
              }
              """,
            """
              public interface TestNonPublicInterfaceMBean {
              }
              """
          )
        );
    }

    @Test
    void mxbeanSuffix() {
        rewriteRun(
          //language=java
          java(
            """
              interface TestNonPublicInterfaceMXBean {
              }
              """,
            """
              public interface TestNonPublicInterfaceMXBean {
              }
              """
          )
        );
    }

    @Nested
    class NoChange {
        @Test
        void packagePrivateWithNoAnnotation() {
            rewriteRun(
              //language=java
              java(
                """
                  interface TestNonPublicInterface {
                  }
                  """
              )
            );
        }

        @Test
        void annotationWithArgumentFalse() {
            rewriteRun(
              //language=java
              java(
                """
                  import javax.management.MXBean;
                  
                  @MXBean(false)
                  interface TestPublicInterfaceAnnotationFalseMXBean {
                  }
                  """
              )
            );
        }

        @Test
        void annotationWithArgumentValueFalse() {
            rewriteRun(
              //language=java
              java(
                """
                  import javax.management.MXBean;
                  
                  @MXBean(value=false)
                  interface TestPublicInterfaceAnnotationValueFalseMXBean {
                  }
                  """
              )
            );
        }

        @Test
        void annotationWithArgumentTrueButAlreadyPublic() {
            rewriteRun(
              //language=java
              java(
                """
                  import javax.management.MXBean;
                  
                  @MXBean(true)
                  public interface TestPublicInterfaceAnnotationTrueMXBean {
                  }
                  """
              )
            );
        }

        @Test
        void annotatonWithArgumentValueTrueButAlreadyPublic() {
            rewriteRun(
              //language=java
              java(
                """
                  import javax.management.MXBean;
                  
                  @MXBean(value=true)
                  public interface TestPublicInterfaceAnnotationValueTrueMXBean {
                  }
                  """
              )
            );
        }

        @Test
        void sufficMBeanButAlreadyPublic() {
            rewriteRun(
              //language=java
              java(
                """
                  public interface TestPublicInterfaceMBean {
                  }
                  """
              )
            );
        }

        @Test
        void suffixMXBeanButAlreadyPublic() {
            rewriteRun(
              //language=java
              java(
                """
                  public interface TestPublicInterfaceMXBean {
                  }
                  """
              )
            );
        }
    }
}