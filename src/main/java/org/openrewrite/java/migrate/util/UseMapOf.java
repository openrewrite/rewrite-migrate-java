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
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class UseMapOf extends Recipe {
    private static final MethodMatcher NEW_HASH_MAP = new MethodMatcher("java.util.HashMap <constructor>()", true);
    private static final MethodMatcher MAP_PUT = new MethodMatcher("java.util.Map put(..)", true);

    private static final String PROSE_REWRITES_KEY = "use-map-of.prose-rewrites";

    @Getter
    final String displayName = "Prefer `Map.of(..)`";

    @Getter
    final String description = "Prefer `Map.of(..)` instead of using `java.util.Map#put(..)` in Java 10 or higher. " +
            "Two input shapes are recognised:\n\n" +
            "- Anonymous-class initialization (`new HashMap<>() {{ put(k, v); ... }}`), which is replaced " +
            "wholesale with `Map.of(k, v, ...)` (or `Map.ofEntries(...)` past ten entries) — immutable result.\n" +
            "- A `new HashMap<>()` declaration followed by a chain of `target.put(k, v)` statements, " +
            "which is collapsed to `new HashMap<>(Map.of(..))` (or `new HashMap<>(Map.ofEntries(..))`) — " +
            "preserving the mutable `HashMap`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(
                        new UsesJavaVersion<>(10),
                        new UsesMethod<>(NEW_HASH_MAP)),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                        J.NewClass n = (J.NewClass) super.visitNewClass(newClass, ctx);

                        // Prose-pattern: see if visitBlock decided this initializer should be wrapped
                        // with `new HashMap<>(Map.of(..))` or `new HashMap<>(Map.ofEntries(..))`.
                        Map<UUID, List<Expression>> rewrites = getCursor().getNearestMessage(PROSE_REWRITES_KEY);
                        if (rewrites != null) {
                            List<Expression> proseArgs = rewrites.get(n.getId());
                            if (proseArgs != null) {
                                // proseArgs is [k1, v1, k2, v2, ...]
                                int pairCount = proseArgs.size() / 2;
                                boolean useEntries = pairCount > 10;
                                StringJoiner inner = useEntries ?
                                        new StringJoiner(", ", "Map.ofEntries(", ")") :
                                        new StringJoiner(", ", "Map.of(", ")");
                                for (int p = 0; p < pairCount; p++) {
                                    if (useEntries) {
                                        inner.add("Map.entry(#{any()}, #{any()})");
                                    } else {
                                        inner.add("#{any()}");
                                        inner.add("#{any()}");
                                    }
                                }
                                String src = "new HashMap<>(" + inner + ")";
                                maybeAddImport("java.util.Map");
                                return JavaTemplate.builder(src)
                                        .contextSensitive()
                                        .imports("java.util.HashMap", "java.util.Map")
                                        .build()
                                        .apply(updateCursor(n), n.getCoordinates().replace(), proseArgs.toArray());
                            }
                        }

                        // Anonymous-class form (original UseMapOf logic, unchanged).
                        J.Block body = n.getBody();
                        if (NEW_HASH_MAP.matches(n) && body != null && body.getStatements().size() == 1 &&
                                TypeUtils.isOfClassType(n.getClazz() != null ? n.getClazz().getType() : null, "java.util.HashMap")) {
                            Statement statement = body.getStatements().get(0);
                            if (statement instanceof J.Block) {
                                List<Statement> putStatements = ((J.Block) statement).getStatements();
                                List<Expression> args = new ArrayList<>();
                                boolean useEntries = putStatements.size() > 10;
                                StringJoiner template = useEntries ?
                                        new StringJoiner(", ", "Map.ofEntries(", ")") :
                                        new StringJoiner(", ", "Map.of(", ")");
                                for (Statement stat : putStatements) {
                                    if (!(stat instanceof J.MethodInvocation) || !MAP_PUT.matches((Expression) stat)) {
                                        return n;
                                    }
                                    J.MethodInvocation put = (J.MethodInvocation) stat;
                                    for (Expression arg : put.getArguments()) {
                                        if (J.Literal.isLiteralValue(arg, null)) {
                                            return n;
                                        }
                                    }
                                    args.addAll(put.getArguments());
                                    if (useEntries) {
                                        template.add("Map.entry(#{any()}, #{any()})");
                                    } else {
                                        template.add("#{any()}");
                                        template.add("#{any()}");
                                    }
                                }

                                maybeRemoveImport("java.util.HashMap");
                                maybeAddImport("java.util.Map");
                                J applied = JavaTemplate.builder(template.toString())
                                        .contextSensitive()
                                        .imports("java.util.Map")
                                        .build()
                                        .apply(updateCursor(n), n.getCoordinates().replace(), args.toArray());
                                if (putStatements.size() > 1 && applied instanceof J.MethodInvocation) {
                                    J.MethodInvocation mapCall = (J.MethodInvocation) applied;
                                    List<Expression> mapArgs = mapCall.getArguments();
                                    List<Expression> withPrefixes = new ArrayList<>(mapArgs.size());
                                    int step = useEntries ? 1 : 2;
                                    for (int i = 0; i < mapArgs.size(); i++) {
                                        Expression arg = mapArgs.get(i);
                                        if (i % step == 0) {
                                            arg = arg.withPrefix(putStatements.get(i / step).getPrefix());
                                        }
                                        withPrefixes.add(arg);
                                    }
                                    return mapCall.withArguments(withPrefixes);
                                }
                                return applied;
                            }
                        }

                        return n;
                    }

                    @Override
                    public J visitBlock(J.Block block, ExecutionContext ctx) {
                        Map<UUID, List<Expression>> rewrites = new HashMap<>();
                        Set<UUID> absorbedPutIds = new HashSet<>();
                        identifyProseRewrites(block, rewrites, absorbedPutIds);

                        if (!rewrites.isEmpty()) {
                            getCursor().putMessage(PROSE_REWRITES_KEY, rewrites);
                        }

                        J.Block b = (J.Block) super.visitBlock(block, ctx);

                        if (absorbedPutIds.isEmpty()) {
                            return b;
                        }
                        List<Statement> filtered = new ArrayList<>(b.getStatements().size());
                        for (Statement s : b.getStatements()) {
                            if (!absorbedPutIds.contains(s.getId())) {
                                filtered.add(s);
                            }
                        }
                        return b.withStatements(filtered);
                    }

                    /**
                     * Walk the block's statements looking for:
                     * <pre>
                     *     Map&lt;K, V&gt; name = new HashMap&lt;&gt;();
                     *     name.put(k1, v1);
                     *     name.put(k2, v2);
                     *     ...
                     * </pre>
                     * For each such sequence with at least two puts, record
                     * (initializer UUID, [k1, v1, k2, v2, ...]) in {@code rewrites} and the
                     * absorbed put statement UUIDs in {@code absorbedPutIds}.
                     */
                    private void identifyProseRewrites(
                            J.Block block,
                            Map<UUID, List<Expression>> rewrites,
                            Set<UUID> absorbedPutIds) {
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

                            List<Expression> args = new ArrayList<>();
                            List<UUID> absorbedHere = new ArrayList<>();
                            int pairs = 0;
                            int j = i + 1;
                            while (j < stmts.size()) {
                                Statement next = stmts.get(j);
                                List<Expression> kv = matchPutCallOn(next, targetName);
                                if (kv == null) {
                                    break;
                                }
                                if (expressionReferences(kv.get(0), targetName) ||
                                        expressionReferences(kv.get(1), targetName)) {
                                    break;
                                }
                                args.addAll(kv);
                                absorbedHere.add(next.getId());
                                pairs++;
                                j++;
                            }
                            if (pairs >= 2 && initializer != null) {
                                rewrites.put(initializer.getId(), args);
                                absorbedPutIds.addAll(absorbedHere);
                                i = j;
                            } else {
                                i++;
                            }
                        }
                    }

                    /**
                     * Returns the variable name if {@code decl} is a single-variable, parameterized
                     * {@code Map<K, V>} declaration whose initializer is a no-arg {@code new HashMap<>()}
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
                        if (!NEW_HASH_MAP.matches(nc)) {
                            return null;
                        }
                        if (nc.getBody() != null) {
                            return null;
                        }
                        return nv.getSimpleName();
                    }

                    /**
                     * If {@code stmt} is {@code targetName.put(k, v)} matching {@link #MAP_PUT},
                     * returns [key, value] as a list; otherwise {@code null}. Returns {@code null}
                     * if either argument is the {@code null} literal, since {@code Map.of(..)} and
                     * {@code Map.entry(..)} reject nulls.
                     */
                    private List<Expression> matchPutCallOn(Statement stmt, String targetName) {
                        if (!(stmt instanceof J.MethodInvocation)) {
                            return null;
                        }
                        J.MethodInvocation mi = (J.MethodInvocation) stmt;
                        if (!MAP_PUT.matches(mi)) {
                            return null;
                        }
                        if (mi.getArguments().size() != 2) {
                            return null;
                        }
                        if (!(mi.getSelect() instanceof J.Identifier)) {
                            return null;
                        }
                        if (!targetName.equals(((J.Identifier) mi.getSelect()).getSimpleName())) {
                            return null;
                        }
                        for (Expression arg : mi.getArguments()) {
                            if (arg instanceof J.Literal && ((J.Literal) arg).getValue() == null) {
                                return null;
                            }
                        }
                        return mi.getArguments();
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
