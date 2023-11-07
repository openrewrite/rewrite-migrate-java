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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;


 class DeprecatedCDIAPIsRemoved40Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "jakarta.enterprise.cdi-api-3.0.0-M4","jakarta.enterprise.cdi-api-4.0.1"))
          .recipe(Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta").build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.DeprecatedCDIAPIsRemoved40"));
    }

    @Test
    void updateCDI() {
        rewriteRun(
          //language=java
          java("""             
            package sample.cdi;
            
            import jakarta.enterprise.event.Observes;
            import jakarta.enterprise.inject.New;
            import jakarta.enterprise.inject.spi.AnnotatedType;
            import jakarta.enterprise.inject.spi.Bean;
            import jakarta.enterprise.inject.spi.BeanManager;
            import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
            import jakarta.enterprise.inject.spi.Extension;
            import java.util.Set;
            
            public class JakartaCdiMethods implements Extension {            
            
            	public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
            		System.out.println("SPI registered extention beforeBeanDiscovery has been fired");
            		AnnotatedType<String> producerType = beanManager.createAnnotatedType(String.class);
 
            		beforeBeanDiscovery.addAnnotatedType(producerType);									// Flag this one
            		beforeBeanDiscovery.addAnnotatedType(producerType, "my unique id");				// Not this one
            		beforeBeanDiscovery.addAnnotatedType(String.class, "my other unique id");	// Not this one
            
            		beanManager.createInjectionTarget(producerType);
            
            		beanManager.fireEvent(beforeBeanDiscovery);
            
            		Set<Bean<?>> myBeans = beanManager.getBeans("my precious beans");
            		Bean myFavoriteBean = myBeans.stream().findFirst().get();
            		myFavoriteBean.isNullable();
            	}
            }
            """, """
            package sample.cdi;
            
            import jakarta.enterprise.event.Observes;
            import jakarta.enterprise.inject.New;
            import jakarta.enterprise.inject.spi.AnnotatedType;
            import jakarta.enterprise.inject.spi.Bean;
            import jakarta.enterprise.inject.spi.BeanManager;
            import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
            import jakarta.enterprise.inject.spi.Extension;
            import java.util.Set;
            
            public class JakartaCdiMethods implements Extension {
            
            	public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
            		System.out.println("SPI registered extention beforeBeanDiscovery has been fired");
            		AnnotatedType<String> producerType = beanManager.createAnnotatedType(String.class);
            
            		beforeBeanDiscovery.addAnnotatedType(producerType, null);									// Flag this one
            		beforeBeanDiscovery.addAnnotatedType(producerType, "my unique id");				// Not this one
            		beforeBeanDiscovery.addAnnotatedType(String.class, "my other unique id");	// Not this one
            
            		beanManager.getInjectionTargetFactory().createInjectionTarget(producerType);
            
            		beanManager.getEvent().fire(beforeBeanDiscovery);
            
            		Set<Bean<?>> myBeans = beanManager.getBeans("my precious beans");
            		Bean myFavoriteBean = myBeans.stream().findFirst().get();           
            	}
            }
            """));
    }
}
