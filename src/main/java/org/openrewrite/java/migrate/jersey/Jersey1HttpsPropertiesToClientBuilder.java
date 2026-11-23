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
package org.openrewrite.java.migrate.jersey;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

import java.util.*;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class Jersey1HttpsPropertiesToClientBuilder extends Recipe {

    private static final String HTTPS_PROPERTIES = "com.sun.jersey.client.urlconnection.HTTPSProperties";
    private static final String PROPERTY_NAME = "com.sun.jersey.client.impl.urlconnection.httpsProperties";
    private static final String CLIENT = "com.sun.jersey.api.client.Client";
    private static final String CLIENT_CONFIG = "com.sun.jersey.api.client.config.ClientConfig";
    private static final String DEFAULT_CLIENT_CONFIG = "com.sun.jersey.api.client.config.DefaultClientConfig";

    private static final MethodMatcher NEW_HTTPS_PROPERTIES = new MethodMatcher(HTTPS_PROPERTIES + " <constructor>(..)");
    private static final MethodMatcher NEW_DEFAULT_CLIENT_CONFIG = new MethodMatcher(DEFAULT_CLIENT_CONFIG + " <constructor>(..)");
    private static final MethodMatcher CREATE_WITH_CONFIG = new MethodMatcher(CLIENT + " create(" + CLIENT_CONFIG + ")");
    private static final MethodMatcher MAP_PUT = new MethodMatcher("java.util.Map put(..)", true);
    private static final MethodMatcher GET_PROPERTIES =
            new MethodMatcher("com.sun.jersey.core.util.FeaturesAndProperties getProperties()", true);

    String displayName = "Configure TLS on the JAX-RS 2 `ClientBuilder` instead of with Jersey 1 `HTTPSProperties`";

    String description = "Jersey 1 sets the hostname verifier and `SSLContext` of its default `HttpURLConnection` " +
            "client with an `HTTPSProperties` put into the client configuration under " +
            "`HTTPSProperties.PROPERTY_HTTPS_PROPERTIES`, a property that Jersey 2 does not have. Where a " +
            "`new DefaultClientConfig(..)` held in a local variable gets that property in a statement of its own, and is " +
            "then given to one `Client.create(..)` later in the same block, removes the statement and creates the client " +
            "with `ClientBuilder.newBuilder().withConfig(..).hostnameVerifier(..).sslContext(..).build()`. A verifier or " +
            "context that is missing or `null` is left out, as Jersey 1 did not apply it either; when both are, " +
            "`Client.create(..)` stays for the recipe that moves it to `ClientBuilder.newClient(..)`. An `HTTPSProperties` " +
            "held in a local variable that is used for nothing else is inlined. Left alone are a configuration that is " +
            "created elsewhere, that is passed, assigned or returned anywhere but to that one `Client.create(..)`, or that " +
            "gets the property more than once; a property put in a nested block, such as a `try` or an `if`, while the " +
            "client is created outside it; a `Client.create(..)` that is not evaluated exactly once with its statement, as " +
            "in a lambda, a loop or a condition; an `HTTPSProperties` that is subclassed or used for anything else; a " +
            "verifier or context that could evaluate differently where the client is created, such as a call with other " +
            "statements than calls on the configuration in between, or a variable assigned in between; and the " +
            "constructors that throw `NoSuchAlgorithmException` inside a `try` that catches it, which would no longer " +
            "compile.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(HTTPS_PROPERTIES, true), new JavaVisitor<ExecutionContext>() {

            /**
             * The {@code Client.create(..)} calls to replace, by id, with the verifier and context to build them with.
             */
            private final Map<UUID, Tls> creates = new HashMap<>();

            @Override
            public J visitBlock(J.Block block, ExecutionContext ctx) {
                Set<UUID> removed = new HashSet<>();
                for (int i = 0; i < block.getStatements().size(); i++) {
                    Migration migration = migration(getCursor(), block, i);
                    if (migration != null) {
                        removed.addAll(migration.getRemoved());
                        if (migration.getTls() != null) {
                            creates.put(migration.getCreate(), migration.getTls());
                        }
                    }
                }
                J j = super.visitBlock(block, ctx);
                if (removed.isEmpty() || !(j instanceof J.Block)) {
                    return j;
                }
                maybeRemoveImport(HTTPS_PROPERTIES);
                J.Block b = (J.Block) j;
                List<Statement> statements = new ArrayList<>();
                Space prefix = null;
                for (Statement s : b.getStatements()) {
                    if (removed.contains(s.getId())) {
                        if (prefix == null && s.getPrefix().getComments().isEmpty()) {
                            prefix = s.getPrefix();
                        }
                        continue;
                    }
                    // the statement that follows what is removed keeps the blank line before either, but starts a
                    // block without one
                    if (prefix != null && s.getPrefix().getComments().isEmpty() &&
                        (statements.isEmpty() || newlines(prefix) > newlines(s.getPrefix()))) {
                        s = s.withPrefix(prefix);
                    }
                    statements.add(s);
                    prefix = null;
                }
                return b.withStatements(statements);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J j = super.visitMethodInvocation(method, ctx);
                Tls tls = creates.remove(method.getId());
                if (tls == null || !(j instanceof J.MethodInvocation)) {
                    return j;
                }
                J.MethodInvocation m = (J.MethodInvocation) j;
                JavaSourceFile cu = getCursor().firstEnclosingOrThrow(JavaSourceFile.class);
                List<Object> parameters = new ArrayList<>();
                StringBuilder code = new StringBuilder(Jersey2Templates.typeName(this, cu, "javax.ws.rs.client.ClientBuilder"))
                        .append(".newBuilder().withConfig(#{any(javax.ws.rs.core.Configuration)})");
                parameters.add(m.getArguments().get(0));
                if (tls.getHostnameVerifier() != null) {
                    code.append(".hostnameVerifier(#{any(javax.net.ssl.HostnameVerifier)})");
                    parameters.add(tls.getHostnameVerifier());
                }
                if (tls.getSslContext() != null) {
                    code.append(".sslContext(#{any(javax.net.ssl.SSLContext)})");
                    parameters.add(tls.getSslContext());
                }
                code.append(".build()");
                maybeRemoveImport(CLIENT);
                return Jersey2Templates.template(code.toString(), ctx, "javax.ws.rs.client.ClientBuilder")
                        .apply(getCursor(), m.getCoordinates().replace(), parameters.toArray());
            }
        });
    }

    /**
     * @return what to remove and replace when the statement at the index puts {@code HTTPSProperties} into a client
     * configuration that only that statement and one {@code Client.create(..)} later in the block use, or
     * {@code null} when anything else is going on.
     */
    private static @Nullable Migration migration(Cursor blockCursor, J.Block block, int putIndex) {
        List<Statement> statements = block.getStatements();
        if (!(statements.get(putIndex) instanceof J.MethodInvocation) ||
            !isHttpsPut((J.MethodInvocation) statements.get(putIndex))) {
            return null;
        }
        J.MethodInvocation put = (J.MethodInvocation) statements.get(putIndex);
        String config = ((J.Identifier) ((J.MethodInvocation) put.getSelect()).getSelect()).getSimpleName();
        int configIndex = declaration(statements, putIndex, config, NEW_DEFAULT_CLIENT_CONFIG);
        if (configIndex < 0) {
            return null;
        }

        // the configuration is put into once and created a client from once, and otherwise only called methods on
        int httpsPuts = 0;
        Cursor create = null;
        for (Cursor reference : references(blockCursor, block, config)) {
            J.Identifier identifier = reference.getValue();
            Cursor parent = reference.getParentTreeCursor();
            if (!(parent.getValue() instanceof J.MethodInvocation)) {
                return null;
            }
            J.MethodInvocation call = parent.getValue();
            if (call.getSelect() == identifier) {
                Object grandparent = parent.getParentTreeCursor().getValue();
                if (grandparent instanceof J.MethodInvocation && ((J.MethodInvocation) grandparent).getSelect() == call &&
                    isHttpsPut((J.MethodInvocation) grandparent)) {
                    httpsPuts++;
                }
            } else if (create == null && CREATE_WITH_CONFIG.matches(call)) {
                create = parent;
            } else {
                return null;
            }
        }
        if (httpsPuts != 1 || create == null) {
            return null;
        }
        int createIndex = statementIndex(create, block);
        if (createIndex <= putIndex) {
            return null;
        }

        // the HTTPSProperties is created in the put, or held in a local variable that nothing else uses
        Expression value = put.getArguments().get(1);
        int sourceIndex = putIndex;
        J.NewClass properties;
        List<UUID> removed = new ArrayList<>();
        removed.add(put.getId());
        if (value instanceof J.NewClass) {
            properties = (J.NewClass) value;
        } else if (value instanceof J.Identifier) {
            String name = ((J.Identifier) value).getSimpleName();
            sourceIndex = declaration(statements, putIndex, name, NEW_HTTPS_PROPERTIES);
            if (sourceIndex < 0 || ((J.VariableDeclarations) statements.get(sourceIndex)).getVariables().size() != 1) {
                return null;
            }
            List<Cursor> references = references(blockCursor, block, name);
            if (references.size() != 1 || references.get(0).getValue() != value) {
                return null;
            }
            J.VariableDeclarations declaration = (J.VariableDeclarations) statements.get(sourceIndex);
            properties = (J.NewClass) Objects.requireNonNull(declaration.getVariables().get(0).getInitializer());
            removed.add(declaration.getId());
        } else {
            return null;
        }
        if (!NEW_HTTPS_PROPERTIES.matches(properties) || properties.getBody() != null ||
            properties.getConstructorType() == null) {
            return null;
        }
        for (JavaType thrown : properties.getConstructorType().getThrownExceptions()) {
            if (caughtAround(blockCursor, thrown)) {
                return null;
            }
        }

        List<Expression> arguments = properties.getArguments();
        Expression hostnameVerifier = !arguments.isEmpty() ? argument(arguments.get(0)) : null;
        Expression sslContext = arguments.size() > 1 ? argument(arguments.get(1)) : null;
        if (hostnameVerifier != null || sslContext != null) {
            // the arguments are now evaluated where the client is created
            Set<String> changed = changedNames(statements.subList(sourceIndex + 1, createIndex + 1));
            boolean quiet = true;
            for (int i = sourceIndex + 1; i < createIndex; i++) {
                quiet &= i == putIndex || i == configIndex || isCallOn(statements.get(i), config);
            }
            if (!quiet && (hostnameVerifier != null && !stable(hostnameVerifier, changed) ||
                           sslContext != null && !stable(sslContext, changed))) {
                return null;
            }
        }
        J.MethodInvocation createCall = create.getValue();
        return new Migration(removed, createCall.getId(),
                hostnameVerifier == null && sslContext == null ? null : new Tls(hostnameVerifier, sslContext));
    }

    /**
     * {@code x.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, ..)} on a variable of the Jersey 1
     * {@code ClientConfig} or {@code DefaultClientConfig}.
     */
    private static boolean isHttpsPut(J.MethodInvocation m) {
        if (!MAP_PUT.matches(m) || !(m.getSelect() instanceof J.MethodInvocation)) {
            return false;
        }
        J.MethodInvocation getter = (J.MethodInvocation) m.getSelect();
        if (!GET_PROPERTIES.matches(getter) || !(getter.getSelect() instanceof J.Identifier)) {
            return false;
        }
        JavaType owner = getter.getSelect().getType();
        if (!TypeUtils.isOfClassType(owner, CLIENT_CONFIG) && !TypeUtils.isOfClassType(owner, DEFAULT_CLIENT_CONFIG)) {
            return false;
        }
        Expression key = m.getArguments().get(0);
        if (key instanceof J.Literal) {
            return PROPERTY_NAME.equals(((J.Literal) key).getValue());
        }
        JavaType.Variable field = key instanceof J.Identifier ? ((J.Identifier) key).getFieldType() :
                key instanceof J.FieldAccess ? ((J.FieldAccess) key).getName().getFieldType() : null;
        return field != null && "PROPERTY_HTTPS_PROPERTIES".equals(field.getName()) &&
               TypeUtils.isOfClassType(field.getOwner(), HTTPS_PROPERTIES);
    }

    /**
     * @return the index of the statement before {@code before} that declares the variable, initialized by a call
     * that the matcher matches, or -1.
     */
    private static int declaration(List<Statement> statements, int before, String name, MethodMatcher initializer) {
        for (int i = 0; i < before; i++) {
            if (statements.get(i) instanceof J.VariableDeclarations) {
                for (J.VariableDeclarations.NamedVariable variable : ((J.VariableDeclarations) statements.get(i)).getVariables()) {
                    if (name.equals(variable.getSimpleName())) {
                        Expression init = variable.getInitializer();
                        return init instanceof J.NewClass && ((J.NewClass) init).getBody() == null &&
                               initializer.matches(init) ? i : -1;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * @return each identifier in the block that refers to a variable of the name, other than where it is declared.
     */
    private static List<Cursor> references(Cursor blockCursor, J.Block block, String name) {
        List<Cursor> references = new ArrayList<>();
        new JavaIsoVisitor<List<Cursor>>() {
            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, List<Cursor> refs) {
                visit(fieldAccess.getTarget(), refs);
                return fieldAccess;
            }

            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable,
                                                                      List<Cursor> refs) {
                visit(variable.getInitializer(), refs);
                return variable;
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, List<Cursor> refs) {
                if (name.equals(identifier.getSimpleName()) && identifier.getFieldType() != null) {
                    refs.add(getCursor());
                }
                return identifier;
            }
        }.visit(block, references, blockCursor.getParentOrThrow());
        return references;
    }

    /**
     * @return the index of the statement of the block that holds the {@code Client.create(..)}, or -1 when it is
     * not evaluated exactly once whenever that statement is, as in a nested block, a lambda, a loop or a condition.
     */
    private static int statementIndex(Cursor create, J.Block block) {
        Tree child = create.getValue();
        for (Cursor c = create.getParent(); c != null; c = c.getParent()) {
            Object value = c.getValue();
            if (value == block) {
                return block.getStatements().indexOf(child);
            }
            if (!(value instanceof Tree)) {
                continue;
            }
            if (!(value instanceof J.MethodInvocation || value instanceof J.NewClass ||
                  value instanceof J.VariableDeclarations || value instanceof J.VariableDeclarations.NamedVariable ||
                  value instanceof J.Return || value instanceof J.Assignment || value instanceof J.Parentheses ||
                  value instanceof J.TypeCast || value instanceof J.FieldAccess)) {
                return -1;
            }
            child = (Tree) value;
        }
        return -1;
    }

    /**
     * @return whether a {@code try} around the block, in the same method, catches the exception by a type more
     * specific than {@code Exception}, which no longer compiles once nothing in the {@code try} throws it.
     */
    private static boolean caughtAround(Cursor blockCursor, JavaType thrown) {
        Tree child = blockCursor.getValue();
        for (Cursor c = blockCursor.getParent(); c != null; c = c.getParent()) {
            Object value = c.getValue();
            if (!(value instanceof Tree)) {
                continue;
            }
            if (value instanceof J.Try && ((J.Try) value).getBody().getId().equals(child.getId())) {
                for (J.Try.Catch aCatch : ((J.Try) value).getCatches()) {
                    JavaType caught = aCatch.getParameter().getTree().getType();
                    for (JavaType type : caught instanceof JavaType.MultiCatch ?
                            ((JavaType.MultiCatch) caught).getThrowableTypes() : singletonList(caught)) {
                        if (!TypeUtils.isOfClassType(type, "java.lang.Exception") &&
                            !TypeUtils.isOfClassType(type, "java.lang.Throwable") &&
                            TypeUtils.isAssignableTo(type, thrown)) {
                            return true;
                        }
                    }
                }
            }
            if (value instanceof J.MethodDeclaration || value instanceof J.Lambda || value instanceof J.NewClass ||
                value instanceof J.ClassDeclaration || value instanceof JavaSourceFile) {
                return false;
            }
            child = (Tree) value;
        }
        return false;
    }

    /**
     * @return the argument, or {@code null} when it is missing or the literal {@code null}, which Jersey 1 does not
     * apply.
     */
    private static @Nullable Expression argument(Expression e) {
        return e instanceof J.Empty || e instanceof J.Literal && ((J.Literal) e).getType() == JavaType.Primitive.Null ?
                null : e;
    }

    /**
     * @return the names that the statements assign or declare.
     */
    private static Set<String> changedNames(List<Statement> statements) {
        Set<String> names = new HashSet<>();
        JavaIsoVisitor<Set<String>> visitor = new JavaIsoVisitor<Set<String>>() {
            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, Set<String> n) {
                addName(assignment.getVariable(), n);
                return super.visitAssignment(assignment, n);
            }

            @Override
            public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, Set<String> n) {
                addName(assignOp.getVariable(), n);
                return super.visitAssignmentOperation(assignOp, n);
            }

            @Override
            public J.Unary visitUnary(J.Unary unary, Set<String> n) {
                if (unary.getOperator().isModifying()) {
                    addName(unary.getExpression(), n);
                }
                return super.visitUnary(unary, n);
            }

            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable,
                                                                      Set<String> n) {
                n.add(variable.getSimpleName());
                return super.visitVariable(variable, n);
            }

            private void addName(Expression variable, Set<String> n) {
                if (variable instanceof J.Identifier) {
                    n.add(((J.Identifier) variable).getSimpleName());
                } else if (variable instanceof J.FieldAccess) {
                    n.add(((J.FieldAccess) variable).getSimpleName());
                }
            }
        };
        for (Statement statement : statements) {
            visitor.visit(statement, names);
        }
        return names;
    }

    /**
     * @return whether the expression has the same value where the client is created as where Jersey 1 evaluated it:
     * literals, lambdas, anonymous classes, local variables and parameters that are not assigned in between, and
     * final fields.
     */
    private static boolean stable(Expression e, Set<String> changed) {
        if (e instanceof J.Literal || e instanceof J.Lambda) {
            return true;
        }
        if (e instanceof J.Parentheses && ((J.Parentheses<?>) e).getTree() instanceof Expression) {
            return stable((Expression) ((J.Parentheses<?>) e).getTree(), changed);
        }
        if (e instanceof J.TypeCast) {
            return stable(((J.TypeCast) e).getExpression(), changed);
        }
        if (e instanceof J.MemberReference) {
            return stable(((J.MemberReference) e).getContaining(), changed);
        }
        if (e instanceof J.NewClass) {
            J.NewClass n = (J.NewClass) e;
            if (n.getBody() == null || n.getEnclosing() != null) {
                return false;
            }
            for (Expression argument : n.getArguments()) {
                if (!(argument instanceof J.Empty) && !stable(argument, changed)) {
                    return false;
                }
            }
            return true;
        }
        if (e instanceof J.Identifier) {
            JavaType.Variable variable = ((J.Identifier) e).getFieldType();
            if (variable == null) {
                // a type or `this`
                return true;
            }
            return !changed.contains(variable.getName()) &&
                   (variable.getOwner() instanceof JavaType.Method || variable.hasFlags(Flag.Final));
        }
        if (e instanceof J.FieldAccess) {
            J.FieldAccess f = (J.FieldAccess) e;
            JavaType.Variable variable = f.getName().getFieldType();
            if (variable == null) {
                // a type
                return true;
            }
            return !changed.contains(variable.getName()) && variable.hasFlags(Flag.Final) &&
                   stable(f.getTarget(), changed);
        }
        return false;
    }

    private static int newlines(Space space) {
        int count = 0;
        for (char c : space.getWhitespace().toCharArray()) {
            if (c == '\n') {
                count++;
            }
        }
        return count;
    }

    /**
     * @return whether the statement is a call on the variable, as {@code config.getClasses().add(..)}.
     */
    private static boolean isCallOn(Statement statement, String variable) {
        Expression e = statement instanceof J.MethodInvocation ? (J.MethodInvocation) statement : null;
        while (e instanceof J.MethodInvocation) {
            e = ((J.MethodInvocation) e).getSelect();
        }
        return e instanceof J.Identifier && variable.equals(((J.Identifier) e).getSimpleName());
    }

    @Value
    private static class Migration {
        List<UUID> removed;
        UUID create;
        @Nullable Tls tls;
    }

    @Value
    private static class Tls {
        @Nullable Expression hostnameVerifier;
        @Nullable Expression sslContext;
    }
}
