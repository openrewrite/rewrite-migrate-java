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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class UseListOf extends Recipe {
    private static final MethodMatcher NEW_ARRAY_LIST = new MethodMatcher("java.util.ArrayList <constructor>()", true);
    private static final MethodMatcher LIST_ADD = new MethodMatcher("java.util.List add(..)", true);

    private static final String PROSE_REWRITES_KEY = "use-list-of.prose-rewrites";

    @Getter
    final String displayName = "Prefer `List.of(..)`";

    @Getter
    final String description = "Prefer `List.of(..)` in Java 10 or higher. Two input shapes are recognised:\n\n" +
            "- Anonymous-class initialization (`new ArrayList<>() {{ add(\"a\"); add(\"b\"); }}`), " +
            "which is replaced wholesale with `List.of(\"a\", \"b\")` (immutable result, matching the " +
            "anonymous-class idiom's typical intent).\n" +
            "- A `new ArrayList<>()` declaration followed by a chain of `target.add(..)` statements, " +
            "which is collapsed to `new ArrayList<>(List.of(..))` (preserving the mutable `ArrayList`).";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(
                        new UsesJavaVersion<>(10),
                        new UsesMethod<>(NEW_ARRAY_LIST)),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                        J.NewClass n = (J.NewClass) super.visitNewClass(newClass, ctx);

                        // Prose-pattern: see if visitBlock (above us on the cursor) decided this
                        // initializer should be wrapped with `new ArrayList<>(List.of(..))`.
                        Map<UUID, List<Expression>> rewrites = getCursor().getNearestMessage(PROSE_REWRITES_KEY);
                        if (rewrites != null) {
                            List<Expression> proseArgs = rewrites.get(n.getId());
                            if (proseArgs != null) {
                                StringJoiner joiner = new StringJoiner(", ", "new ArrayList<>(List.of(", "))");
                                for (int k = 0; k < proseArgs.size(); k++) {
                                    joiner.add("#{any()}");
                                }
                                maybeAddImport("java.util.List");
                                return JavaTemplate.builder(joiner.toString())
                                        .contextSensitive()
                                        .imports("java.util.ArrayList", "java.util.List")
                                        .build()
                                        .apply(updateCursor(n), n.getCoordinates().replace(), proseArgs.toArray());
                            }
                        }

                        // Anonymous-class form (original UseListOf logic, unchanged).
                        J.Block body = n.getBody();
                        if (NEW_ARRAY_LIST.matches(n) && body != null && body.getStatements().size() == 1) {
                            Statement statement = body.getStatements().get(0);
                            if (statement instanceof J.Block) {
                                List<Expression> args = new ArrayList<>();
                                StringJoiner listOf = new StringJoiner(", ", "List.of(", ")");
                                for (Statement stat : ((J.Block) statement).getStatements()) {
                                    if (!(stat instanceof J.MethodInvocation) || !LIST_ADD.matches((Expression) stat)) {
                                        return n;
                                    }
                                    J.MethodInvocation add = (J.MethodInvocation) stat;
                                    if (add.getArguments().size() != 1) {
                                        return n;
                                    }
                                    args.add(add.getArguments().get(0));
                                    listOf.add("#{any()}");
                                }

                                maybeRemoveImport("java.util.ArrayList");
                                maybeAddImport("java.util.List");
                                return JavaTemplate.builder(listOf.toString())
                                        .contextSensitive()
                                        .imports("java.util.List")
                                        .build()
                                        .apply(updateCursor(n), n.getCoordinates().replace(), args.toArray());
                            }
                        }

                        return n;
                    }

                    @Override
                    public J visitBlock(J.Block block, ExecutionContext ctx) {
                        // Pre-pass: scan the ORIGINAL block to identify which initializers to
                        // rewrite and which `add(..)` statements to absorb. UUIDs are stable
                        // through super.visitBlock unless a child visitor rebuilds the node,
                        // and nothing else in this recipe touches the targeted initializers
                        // before visitNewClass fires.
                        Map<UUID, List<Expression>> rewrites = new HashMap<>();
                        Set<UUID> absorbedAddIds = new HashSet<>();
                        identifyProseRewrites(block, rewrites, absorbedAddIds);

                        if (!rewrites.isEmpty()) {
                            getCursor().putMessage(PROSE_REWRITES_KEY, rewrites);
                        }

                        J.Block b = (J.Block) super.visitBlock(block, ctx);

                        if (absorbedAddIds.isEmpty()) {
                            return b;
                        }
                        // Post-pass: drop the now-absorbed `add(..)` statements from the block.
                        List<Statement> filtered = new ArrayList<>(b.getStatements().size());
                        for (Statement s : b.getStatements()) {
                            if (!absorbedAddIds.contains(s.getId())) {
                                filtered.add(s);
                            }
                        }
                        return b.withStatements(filtered);
                    }

                    /**
                     * Walk the block's statements looking for:
                     * <pre>
                     *     List&lt;T&gt; name = new ArrayList&lt;&gt;();
                     *     name.add(x1);
                     *     name.add(x2);
                     *     ...
                     * </pre>
                     * For each such sequence with at least two adds, record
                     * (initializer UUID, [args]) in {@code rewrites} and the absorbed add
                     * statement UUIDs in {@code absorbedAddIds}.
                     */
                    private void identifyProseRewrites(
                            J.Block block,
                            Map<UUID, List<Expression>> rewrites,
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
                            // (matchingTargetName already verified the initializer is a J.NewClass)

                            List<Expression> args = new ArrayList<>();
                            List<UUID> absorbedHere = new ArrayList<>();
                            int j = i + 1;
                            while (j < stmts.size()) {
                                Statement next = stmts.get(j);
                                Expression arg = matchAddCallOn(next, targetName);
                                if (arg == null || expressionReferences(arg, targetName)) {
                                    break;
                                }
                                args.add(arg);
                                absorbedHere.add(next.getId());
                                j++;
                            }
                            if (args.size() >= 2 && initializer != null) {
                                rewrites.put(initializer.getId(), args);
                                absorbedAddIds.addAll(absorbedHere);
                                i = j;
                            } else {
                                i++;
                            }
                        }
                    }

                    /**
                     * Returns the variable name if {@code decl} is a single-variable, parameterized
                     * {@code List<T>} declaration whose initializer is a no-arg {@code new ArrayList<>()}
                     * with no anonymous-class body. Returns {@code null} otherwise.
                     */
                    private String matchingTargetName(J.VariableDeclarations decl) {
                        if (decl.getVariables().size() != 1) {
                            return null;
                        }
                        // Require parameterized LHS; for raw `List` we'd be guessing at a type argument.
                        if (!(decl.getTypeExpression() instanceof J.ParameterizedType)) {
                            return null;
                        }
                        J.VariableDeclarations.NamedVariable nv = decl.getVariables().get(0);
                        if (!(nv.getInitializer() instanceof J.NewClass)) {
                            return null;
                        }
                        J.NewClass nc = (J.NewClass) nv.getInitializer();
                        if (!NEW_ARRAY_LIST.matches(nc)) {
                            return null;
                        }
                        // A body would put us in the anonymous-class case handled by visitNewClass directly.
                        if (nc.getBody() != null) {
                            return null;
                        }
                        return nv.getSimpleName();
                    }

                    /**
                     * If {@code stmt} is {@code targetName.add(arg)} matching {@link #LIST_ADD},
                     * returns the single argument expression; otherwise {@code null}. Also returns
                     * {@code null} when the argument is the {@code null} literal, since
                     * {@code List.of(..)} rejects nulls.
                     */
                    private Expression matchAddCallOn(Statement stmt, String targetName) {
                        if (!(stmt instanceof J.MethodInvocation)) {
                            return null;
                        }
                        J.MethodInvocation mi = (J.MethodInvocation) stmt;
                        if (!LIST_ADD.matches(mi)) {
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
                        AtomicBoolean found = new AtomicBoolean(false);
                        new JavaIsoVisitor<AtomicBoolean>() {
                            @Override
                            public J.Identifier visitIdentifier(J.Identifier id, AtomicBoolean f) {
                                if (name.equals(id.getSimpleName())) {
                                    f.set(true);
                                }
                                return id;
                            }
                        }.visit(expr, found);
                        return found.get();
                    }
                });
    }
}
