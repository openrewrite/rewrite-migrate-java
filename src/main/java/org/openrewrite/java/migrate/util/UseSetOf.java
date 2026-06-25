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
package org.openrewrite.java.migrate.util;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class UseSetOf extends Recipe {
    private static final MethodMatcher NEW_HASH_SET = new MethodMatcher("java.util.HashSet <constructor>()", true);
    private static final MethodMatcher SET_ADD = new MethodMatcher("java.util.Set add(..)", true);

    private static final String PROSE_REWRITES_KEY = "use-set-of.prose-rewrites";

    @Getter
    final String displayName = "Prefer `Set.of(..)`";

    @Getter
    final String description = "Prefer `Set.of(..)` in Java 10 or higher. Two input shapes are recognised:\n\n" +
            "- Anonymous-class initialization (`new HashSet<>() {{ add(\"a\"); add(\"b\"); }}`), " +
            "which is replaced wholesale with `Set.of(\"a\", \"b\")` (immutable result, matching the " +
            "anonymous-class idiom's typical intent).\n" +
            "- A `new HashSet<>()` declaration followed by a chain of `target.add(..)` statements, " +
            "which is collapsed to `new HashSet<>(Set.of(..))` (preserving the mutable `HashSet`).";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(
                        new UsesJavaVersion<>(10),
                        new UsesMethod<>(NEW_HASH_SET)),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                        J.NewClass n = (J.NewClass) super.visitNewClass(newClass, ctx);

                        // Prose-pattern: see if visitBlock (above us on the cursor) decided this
                        // initializer should be wrapped with `new HashSet<>(Set.of(..))`.
                        Map<UUID, List<J.MethodInvocation>> rewrites = getCursor().getNearestMessage(PROSE_REWRITES_KEY);
                        if (rewrites != null) {
                            List<J.MethodInvocation> adds = rewrites.get(n.getId());
                            if (adds != null) {
                                List<Expression> args = new ArrayList<>();
                                StringJoiner joiner = new StringJoiner(", ", "new HashSet<>(Set.of(", "))");
                                for (J.MethodInvocation add : adds) {
                                    args.add(add.getArguments().get(0));
                                    joiner.add("#{any()}");
                                }
                                maybeAddImport("java.util.Set");
                                J applied = JavaTemplate.builder(joiner.toString())
                                        .contextSensitive()
                                        .imports("java.util.HashSet", "java.util.Set")
                                        .build()
                                        .apply(updateCursor(n), n.getCoordinates().replace(), args.toArray());
                                // Reattach each add's prefix so the elements land one-per-line and any
                                // leading comments survive, then autoformat to nest the indentation.
                                return autoFormat(reattachElementPrefixes(applied, adds), ctx);
                            }
                        }

                        // Anonymous-class form (original UseSetOf logic, unchanged).
                        J.Block body = n.getBody();
                        if (NEW_HASH_SET.matches(n) && body != null && body.getStatements().size() == 1) {
                            Statement statement = body.getStatements().get(0);
                            if (statement instanceof J.Block) {
                                List<Expression> args = new ArrayList<>();
                                StringJoiner setOf = new StringJoiner(", ", "Set.of(", ")");
                                for (Statement stat : ((J.Block) statement).getStatements()) {
                                    if (!(stat instanceof J.MethodInvocation) || !SET_ADD.matches((Expression) stat)) {
                                        return n;
                                    }
                                    J.MethodInvocation add = (J.MethodInvocation) stat;
                                    if (add.getArguments().size() != 1) {
                                        return n;
                                    }
                                    args.add(add.getArguments().get(0));
                                    setOf.add("#{any()}");
                                }

                                maybeRemoveImport("java.util.HashSet");
                                maybeAddImport("java.util.Set");
                                return JavaTemplate.builder(setOf.toString())
                                        .contextSensitive()
                                        .imports("java.util.Set")
                                        .build()
                                        .apply(updateCursor(n), n.getCoordinates().replace(), args.toArray());
                            }
                        }

                        return n;
                    }

                    /**
                     * Re-applies the absorbed add statements' prefixes to the generated
                     * {@code new HashSet<>(Set.of(..))} so each element keeps its own line and any
                     * leading comments. {@code adds} holds one invocation per element, in order.
                     */
                    private J reattachElementPrefixes(J applied, List<J.MethodInvocation> adds) {
                        if (!(applied instanceof J.NewClass)) {
                            return applied;
                        }
                        J.NewClass nc = (J.NewClass) applied;
                        if (nc.getArguments().size() != 1 || !(nc.getArguments().get(0) instanceof J.MethodInvocation)) {
                            return applied;
                        }
                        J.MethodInvocation setCall = (J.MethodInvocation) nc.getArguments().get(0);
                        List<Expression> setArgs = setCall.getArguments();
                        List<Expression> withPrefixes = new ArrayList<>(setArgs.size());
                        for (int i = 0; i < setArgs.size(); i++) {
                            withPrefixes.add(setArgs.get(i).withPrefix(adds.get(i).getPrefix()));
                        }
                        return nc.withArguments(Collections.singletonList(setCall.withArguments(withPrefixes)));
                    }

                    @Override
                    public J visitBlock(J.Block block, ExecutionContext ctx) {
                        Map<UUID, List<J.MethodInvocation>> rewrites = new HashMap<>();
                        Set<UUID> absorbedAddIds = new HashSet<>();
                        identifyProseRewrites(block, rewrites, absorbedAddIds);

                        if (!rewrites.isEmpty()) {
                            getCursor().putMessage(PROSE_REWRITES_KEY, rewrites);
                        }

                        J.Block b = (J.Block) super.visitBlock(block, ctx);

                        // Post-pass: drop the now-absorbed `add(..)` statements from the block.
                        return b.withStatements(ListUtils.filter(b.getStatements(), s -> !absorbedAddIds.contains(s.getId())));
                    }

                    private void identifyProseRewrites(
                            J.Block block,
                            Map<UUID, List<J.MethodInvocation>> rewrites,
                            Set<UUID> absorbedAddIds) {
                        List<Statement> stmts = block.getStatements();
                        int i = 0;
                        while (i < stmts.size()) {
                            Statement stmt = stmts.get(i);
                            if (!(stmt instanceof J.VariableDeclarations)) {
                                i++;
                                continue;
                            }
                            J.VariableDeclarations decl = (J.VariableDeclarations) stmt;
                            String targetName = matchingTargetName(decl);
                            if (targetName == null) {
                                i++;
                                continue;
                            }
                            J.NewClass initializer = (J.NewClass) decl.getVariables().get(0).getInitializer();

                            List<J.MethodInvocation> adds = new ArrayList<>();
                            List<UUID> absorbedHere = new ArrayList<>();
                            int j = i + 1;
                            while (j < stmts.size()) {
                                Statement next = stmts.get(j);
                                Expression arg = matchAddCallOn(next, targetName);
                                if (arg == null || expressionReferences(arg, targetName)) {
                                    break;
                                }
                                adds.add((J.MethodInvocation) next);
                                absorbedHere.add(next.getId());
                                j++;
                            }
                            if (adds.size() >= 2 && initializer != null) {
                                rewrites.put(initializer.getId(), adds);
                                absorbedAddIds.addAll(absorbedHere);
                                i = j;
                            } else {
                                i++;
                            }
                        }
                    }

                    /**
                     * Returns the variable name if {@code decl} is a single-variable, parameterized
                     * {@code Set<T>} declaration whose initializer is a no-arg {@code new HashSet<>()}
                     * with no anonymous-class body. Returns {@code null} otherwise.
                     */
                    private String matchingTargetName(J.VariableDeclarations decl) {
                        if (decl.getVariables().size() != 1) {
                            return null;
                        }
                        if (!(decl.getTypeExpression() instanceof J.ParameterizedType)) {
                            return null;
                        }
                        J.VariableDeclarations.NamedVariable nv = decl.getVariables().get(0);
                        if (!(nv.getInitializer() instanceof J.NewClass)) {
                            return null;
                        }
                        J.NewClass nc = (J.NewClass) nv.getInitializer();
                        if (!NEW_HASH_SET.matches(nc)) {
                            return null;
                        }
                        if (nc.getBody() != null) {
                            return null;
                        }
                        return nv.getSimpleName();
                    }

                    private Expression matchAddCallOn(Statement stmt, String targetName) {
                        if (!(stmt instanceof J.MethodInvocation)) {
                            return null;
                        }
                        J.MethodInvocation mi = (J.MethodInvocation) stmt;
                        if (!SET_ADD.matches(mi)) {
                            return null;
                        }
                        if (mi.getArguments().size() != 1) {
                            return null;
                        }
                        if (!(mi.getSelect() instanceof J.Identifier)) {
                            return null;
                        }
                        if (!targetName.equals(((J.Identifier) mi.getSelect()).getSimpleName())) {
                            return null;
                        }
                        Expression arg = mi.getArguments().get(0);
                        if (arg instanceof J.Literal && ((J.Literal) arg).getValue() == null) {
                            return null;
                        }
                        return arg;
                    }

                    private boolean expressionReferences(Expression expr, String name) {
                        return new JavaIsoVisitor<AtomicBoolean>() {
                            @Override
                            public J.Identifier visitIdentifier(J.Identifier id, AtomicBoolean f) {
                                if (name.equals(id.getSimpleName())) {
                                    f.set(true);
                                }
                                return id;
                            }
                        }.reduce(expr, new AtomicBoolean(false)).get();
                    }
                });
    }
}
