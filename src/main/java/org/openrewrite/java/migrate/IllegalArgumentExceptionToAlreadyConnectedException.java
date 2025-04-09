/*
 * Copyright 2024 the original author or authors.
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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

public class IllegalArgumentExceptionToAlreadyConnectedException extends Recipe {

    private static final String ILLEGAL_ARGUMENT_EXCEPTION = "java.lang.IllegalArgumentException";
    private static final String ALREADY_CONNECTED_EXCEPTION = "java.nio.channels.AlreadyConnectedException";

    @Override
    public String getDisplayName() {
        return "Replace `IllegalArgumentException` with `AlreadyConnectedException` in `DatagramChannel.send()` method";
    }

    @Override
    public String getDescription() {
        return "Replace `IllegalArgumentException` with `AlreadyConnectedException` for DatagramChannel.send() to ensure compatibility with Java 11+.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String datagramChannelSendMethodPattern = "java.nio.channels.DatagramChannel send(java.nio.ByteBuffer, java.net.SocketAddress)";
        return Preconditions.check(new UsesMethod<>(datagramChannelSendMethodPattern), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Try visitTry(J.Try tryStatement, ExecutionContext ctx) {
                J.Try try_ = super.visitTry(tryStatement, ctx);
                if (FindMethods.find(try_, datagramChannelSendMethodPattern).isEmpty()) {
                    return try_;
                }
                return try_.withCatches(ListUtils.map(try_.getCatches(), catch_ -> {
                    if (TypeUtils.isOfClassType(catch_.getParameter().getType(), ILLEGAL_ARGUMENT_EXCEPTION)) {
                        maybeAddImport(ALREADY_CONNECTED_EXCEPTION);
                        return (J.Try.Catch) new ChangeType(ILLEGAL_ARGUMENT_EXCEPTION, ALREADY_CONNECTED_EXCEPTION, true, null)
                                .getVisitor().visit(catch_, ctx);
                    }
                    return catch_;
                }));
            }
        });
    }
}
