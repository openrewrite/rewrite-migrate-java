/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.lang;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static java.util.Arrays.asList;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindThreadStartInConstructor extends Recipe {

    String displayName = "Find `Thread.start()` calls made during construction of a non-final class";

    String description = "Finds `Thread.start()` invocations reached during construction of a " +
            "non-`final` class — from a constructor body, an instance field initializer, or an " +
            "instance initializer block. Starting a thread before construction completes lets the " +
            "new thread observe a partially-initialised object; the problem is compounded when a " +
            "subclass extends the class, because the superclass constructor starts the thread " +
            "before the subclass' own fields have been initialised. Move the `start()` call to a " +
            "separate method callers invoke after construction, or declare the class `final`.";

    Set<String> tags = new HashSet<>(asList("RSPEC-S2693"));

    private static final MethodMatcher THREAD_START = new MethodMatcher("java.lang.Thread start()", true);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesMethod<>(THREAD_START),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                        if (!THREAD_START.matches(mi)) {
                            return mi;
                        }
                        if (isDuringConstructionOfNonFinalClass()) {
                            return SearchResult.found(mi,
                                    "`Thread.start()` called during construction of a non-final class. " +
                                            "The new thread can observe a partially-initialised object, and " +
                                            "any subclass' fields are guaranteed unset. Move the call to a " +
                                            "separate method or declare the class `final`.");
                        }
                        return mi;
                    }

                    /**
                     * Walks up from the invocation site looking for the first frame that decides
                     * the context: a lambda / static initializer / regular method → not construction;
                     * a constructor → in construction; a class boundary reached before any of those
                     * → we're in an instance field initializer or instance init block, also in
                     * construction. Anonymous classes are treated as effectively final.
                     */
                    private boolean isDuringConstructionOfNonFinalClass() {
                        for (Iterator<Object> it = getCursor().getPath(); it.hasNext(); ) {
                            Object p = it.next();
                            if (p instanceof J.Lambda) {
                                return false;
                            }
                            if (p instanceof J.Block && ((J.Block) p).isStatic()) {
                                return false;
                            }
                            if (p instanceof J.MethodDeclaration) {
                                J.MethodDeclaration md = (J.MethodDeclaration) p;
                                if (!md.isConstructor()) {
                                    return false;
                                }
                                return !enclosingClassIsFinal();
                            }
                            if (p instanceof J.NewClass && ((J.NewClass) p).getBody() != null) {
                                // Inside an anonymous class body — effectively final, can't be extended.
                                return false;
                            }
                            if (p instanceof J.ClassDeclaration) {
                                return !isEffectivelyFinal((J.ClassDeclaration) p);
                            }
                        }
                        return false;
                    }

                    private boolean enclosingClassIsFinal() {
                        J.ClassDeclaration cd = getCursor().firstEnclosing(J.ClassDeclaration.class);
                        return cd != null && isEffectivelyFinal(cd);
                    }

                    private boolean isEffectivelyFinal(J.ClassDeclaration cd) {
                        if (cd.hasModifier(J.Modifier.Type.Final)) {
                            return true;
                        }
                        J.ClassDeclaration.Kind.Type kind = cd.getKind();
                        return kind == J.ClassDeclaration.Kind.Type.Record ||
                                kind == J.ClassDeclaration.Kind.Type.Enum;
                    }
                }
        );
    }
}
