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
import org.openrewrite.java.DeleteMethodArgument;

public class RemovalsServletJakarta10Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "jakarta.servlet-api-5.0.0","jakarta.servlet-api-6.0.0", "javax.servlet-api-4.0.0"))
          .recipe(Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.RemovalsServletJakarta10"));

    }

    @Test
    void testServlet() {

        rewriteRun(
          //language=java
          java("""
            package com.test;
            import java.io.IOException;

            import jakarta.servlet.ServletContext;
            import jakarta.servlet.ServletException;
            import jakarta.servlet.SingleThreadModel;
            import jakarta.servlet.UnavailableException;
            import jakarta.servlet.http.HttpServlet;
            import jakarta.servlet.http.HttpServletRequest;
            import jakarta.servlet.http.HttpServletRequestWrapper;
            import jakarta.servlet.http.HttpServletResponse;
            import jakarta.servlet.http.HttpServletResponseWrapper;
            import jakarta.servlet.http.HttpSession;
            import jakarta.servlet.http.HttpSessionContext;
            import jakarta.servlet.http.HttpUtils;
            
            public class TestJakarta extends HttpServlet implements SingleThreadModel {
            
                public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                    req.isRequestedSessionIdFromUrl();            
                    res.encodeUrl("");
                    res.encodeRedirectUrl("");        
                    res.setStatus(0,  "");            
                    res.setStatus(0);           
                    HttpServletRequestWrapper reqWrapper = new HttpServletRequestWrapper(req);
                    reqWrapper.isRequestedSessionIdFromUrl();            
                    HttpServletResponseWrapper resWrapper = new HttpServletResponseWrapper(res);     
                    resWrapper.encodeUrl("");
                    resWrapper.encodeRedirectUrl("");    
                    resWrapper.setStatus(0,  "");                 
                    HttpSession httpSession = req.getSession();
                    httpSession.getSessionContext();
                    httpSession.getValue("");
                    httpSession.getValueNames();
                    httpSession.putValue("", null);
                    httpSession.removeValue("");     
                    ServletContext servletContext = getServletContext();  
                    servletContext.getServlet("");
                    servletContext.getServlets();
                    servletContext.getServletNames();   
                    servletContext.log(null, "");   
                    req.getRealPath("");        
                    HttpServletRequestWrapper reqWrapper = new HttpServletRequestWrapper(req);
                    reqWrapper.getRealPath("");          
                }        
            }         
            """, """
            package com.test;
            import java.io.IOException;

            import jakarta.servlet.ServletContext;
            import jakarta.servlet.ServletException;
            import jakarta.servlet.SingleThreadModel;
            import jakarta.servlet.UnavailableException;
            import jakarta.servlet.http.HttpServlet;
            import jakarta.servlet.http.HttpServletRequest;
            import jakarta.servlet.http.HttpServletRequestWrapper;
            import jakarta.servlet.http.HttpServletResponse;
            import jakarta.servlet.http.HttpServletResponseWrapper;
            import jakarta.servlet.http.HttpSession;
            import jakarta.servlet.http.HttpSessionContext;
            import jakarta.servlet.http.HttpUtils;
            
            public class TestJakarta extends HttpServlet implements SingleThreadModel {
            
                public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                    req.isRequestedSessionIdFromURL();            
                    res.encodeURL("");
                    res.encodeRedirectURL("");   
                    res.setStatus(0);            
                    res.setStatus(0);          
                    HttpServletRequestWrapper reqWrapper = new HttpServletRequestWrapper(req);
                    reqWrapper.isRequestedSessionIdFromURL();            
                    HttpServletResponseWrapper resWrapper = new HttpServletResponseWrapper(res);   
                    resWrapper.encodeURL("");
                    resWrapper.encodeRedirectURL("");   
                    resWrapper.setStatus(0);                
                    HttpSession httpSession = req.getSession();
                    httpSession.getSessionContext();
                    httpSession.getAttribute("");
                    httpSession.getAttributeNames();
                    httpSession.setAttribute("", null);
                    httpSession.removeAttribute("");        
                    ServletContext servletContext = getServletContext();  
                    servletContext.getServlet("");
                    servletContext.getServlets();
                    servletContext.getServletNames();    
                    servletContext.log("", null);
                    req.getContext().getRealPath("");        
                    HttpServletRequestWrapper reqWrapper = new HttpServletRequestWrapper(req);
                    reqWrapper.getContext().getRealPath("");             
                }        
            }       
            """));
    }
    void testException() {
        rewriteRun(
          //language=java
          java("""
            package com.test;
             
            import java.io.IOException;
  
            import jakarta.servlet.ServletException;            
            import jakarta.servlet.SingleThreadModel;
            import jakarta.servlet.UnavailableException;
            import jakarta.servlet.http.HttpServlet;
            import jakarta.servlet.http.HttpServletRequest;
            import jakarta.servlet.http.HttpServletResponse;         
  
            public class Test {
            
                public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                
                 	   jakarta.servlet.Servlet servlet ;
                 	   UnavailableException unavailableEx2 = new UnavailableException("x",1);     
                 	   UnavailableException unavailableEx1 = new UnavailableException(0, null, "");     
                }
               
            }
            """, """
            package com.test;
             
            import java.io.IOException;
  
            import jakarta.servlet.ServletException;            
            import jakarta.servlet.SingleThreadModel;
            import jakarta.servlet.UnavailableException;
            import jakarta.servlet.http.HttpServlet;
            import jakarta.servlet.http.HttpServletRequest;
            import jakarta.servlet.http.HttpServletResponse;         
  
            public class Test {
            
                public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                
                 	   jakarta.servlet.Servlet servlet ;
                 	   UnavailableException unavailableEx2 = new UnavailableException("x");  
                 	   UnavailableException unavailableEx1 = new UnavailableException(0, "");
                }
               
            }
            """));
    }
}
