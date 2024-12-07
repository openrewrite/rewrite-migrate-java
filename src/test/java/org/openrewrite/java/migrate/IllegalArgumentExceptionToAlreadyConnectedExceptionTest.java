/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class IllegalArgumentExceptionToAlreadyConnectedExceptionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new IllegalArgumentExceptionToAlreadyConnectedException());
    }

    @DocumentExample
    @Test
    void catchException() {
        rewriteRun(
          //language=java
          java(
            """
              import java.nio.ByteBuffer;
              import java.net.SocketAddress;
              import java.nio.channels.DatagramChannel;

              public class Test {
                  public void sendDataCatch() {
                      try {
                          DatagramChannel channel = DatagramChannel.open();
                          channel.send(ByteBuffer.allocate(1024), new java.net.InetSocketAddress("localhost", 8080));
                      } catch (IllegalArgumentException e) {
                          System.out.println("Caught Exception");
                      }
                  }
              }
              """,
            """
              import java.nio.ByteBuffer;
              import java.net.SocketAddress;
              import java.nio.channels.DatagramChannel;

              public class Test {
                  public void sendDataCatch() {
                      try {
                          DatagramChannel channel = DatagramChannel.open();
                          channel.send(ByteBuffer.allocate(1024), new java.net.InetSocketAddress("localhost", 8080));
                      } catch (AlreadyConnectedException e) {
                          System.out.println("Caught Exception");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void rethrowException() {
        rewriteRun(
          //language=java
          java(
            """
              import java.nio.ByteBuffer;
              import java.net.SocketAddress;
              import java.nio.channels.DatagramChannel;

              public class Test {
                  public void sendDataRethrow() {
                      try {
                          DatagramChannel channel = DatagramChannel.open();
                          channel.send(ByteBuffer.allocate(1024), new java.net.InetSocketAddress("localhost", 8080));
                      } catch (IllegalArgumentException e) {
                          throw new IllegalArgumentException("DatagramChannel already connected to a different address");
                      }
                  }
              }
              """,
            """
              import java.nio.ByteBuffer;
              import java.net.SocketAddress;
              import java.nio.channels.DatagramChannel;

              public class Test {
                  public void sendDataRethrow() {
                      try {
                          DatagramChannel channel = DatagramChannel.open();
                          channel.send(ByteBuffer.allocate(1024), new java.net.InetSocketAddress("localhost", 8080));
                      } catch (AlreadyConnectedException e) {
                          throw new AlreadyConnectedException("DatagramChannel already connected to a different address");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void retainOtherCaughtExceptions() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.IOException;import java.nio.ByteBuffer;
              import java.net.SocketAddress;
              import java.nio.channels.DatagramChannel;

              public class Test {
                  public void sendData() {
                      try {
                          DatagramChannel channel = DatagramChannel.open();
                          channel.send(ByteBuffer.allocate(1024), new java.net.InetSocketAddress("localhost", 8080));
                      } catch (IOException e) {
                          System.out.println("Caught Exception");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void retainIllegalArgumentExceptionWithoutChannelSend() {
        rewriteRun(
          //language=java
          java(
            """
              import java.nio.ByteBuffer;
              import java.net.SocketAddress;
              import java.nio.channels.DatagramChannel;

              public class Test {
                  public void sendData() {
                      try {
                          DatagramChannel channel = DatagramChannel.open();
                      } catch (IllegalArgumentException e) {
                          System.out.println("Caught Exception");
                      }
                  }
              }
              """
          )
        );
    }
}
