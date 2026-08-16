/*
 * Copyright 2025 the original author or authors.
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
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RenameVariable;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.staticanalysis.groovy.GroovyFileChecker;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptySet;

@EqualsAndHashCode(callSuper = false)
@Value
public class RenameUnderscoreIdentifier extends ScanningRecipe<Set<Path>> {

    String displayName = "Rename `_` identifier to `__`";

    String description = "Renames single-underscore identifiers to double-underscore " +
                          "in Java source files with source compatibility of Java 8 or below. " +
                          "In Java 9+, `_` is a reserved keyword and causes a compile error. " +
                          "Further underscores are appended when `__` is already taken, so that " +
                          "the rename does not collide with any declaration the recipe can see: " +
                          "a variable or class rename avoids every name in its source file, and " +
                          "a method rename avoids the method names of the type hierarchy that " +
                          "declares the overridden method. When a class rename also renames the " +
                          "source file, names whose `.java` file already exists next to it are " +
                          "skipped as well, so the rename never overwrites another source file. " +
                          "A collision with a declaration the recipe cannot see, such as one " +
                          "in a subclass from another source file, is still possible.";

    @Override
    public Set<Path> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<Path> acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    acc.add(((SourceFile) tree).getSourcePath());
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Set<Path> acc) {
        return Preconditions.check(
                Preconditions.and(
                        new UsesJavaVersion<>(1, 8),
                        Preconditions.not(new KotlinFileChecker<>()),
                        Preconditions.not(new GroovyFileChecker<>())
                ),
                new RenameIdentifierVisitor("_", "__", acc)
        );
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    static class RenameIdentifierVisitor extends JavaIsoVisitor<ExecutionContext> {

        private static final String NAMES_IN_USE = "namesInUse";

        String oldName;
        String newName;

        /// The paths of every source file, so a class rename never picks a name whose file is taken.
        Set<Path> sourcePaths;

        RenameIdentifierVisitor(String oldName, String newName) {
            this(oldName, newName, emptySet());
        }

        RenameIdentifierVisitor(String oldName, String newName, Set<Path> sourcePaths) {
            this.oldName = oldName;
            this.newName = newName;
            this.sourcePaths = sourcePaths;
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(
                J.VariableDeclarations multiVariable, ExecutionContext ctx) {
            for (J.VariableDeclarations.NamedVariable v : multiVariable.getVariables()) {
                if (oldName.equals(v.getSimpleName())) {
                    doAfterVisit(new RenameVariable<>(v, availableName()));
                }
            }
            return super.visitVariableDeclarations(multiVariable, ctx);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(
                J.MethodDeclaration method, ExecutionContext ctx) {
            method = super.visitMethodDeclaration(method, ctx);
            // A constructor carries its class's name, so it is renamed with the class declaration
            JavaType.Method type = method.getMethodType();
            if (oldName.equals(method.getSimpleName()) && !method.isConstructor() && type != null) {
                type = type.withName(availableName(type));
                method = method.withName(method.getName().withSimpleName(type.getName())
                                .withType(type))
                        .withMethodType(type);
            }
            return method;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(
                J.MethodInvocation method, ExecutionContext ctx) {
            method = super.visitMethodInvocation(method, ctx);
            JavaType.Method type = method.getMethodType();
            if (oldName.equals(method.getSimpleName()) && type != null) {
                type = type.withName(availableName(type));
                method = method.withName(method.getName().withSimpleName(type.getName())
                                .withType(type))
                        .withMethodType(type);
            }
            return method;
        }

        @Override
        public J.MemberReference visitMemberReference(
                J.MemberReference memberRef, ExecutionContext ctx) {
            memberRef = super.visitMemberReference(memberRef, ctx);
            JavaType.Method type = memberRef.getMethodType();
            if (oldName.equals(memberRef.getReference().getSimpleName()) && type != null) {
                type = type.withName(availableName(type));
                memberRef = memberRef.withReference(memberRef.getReference()
                                .withSimpleName(type.getName()))
                        .withMethodType(type);
            }
            return memberRef;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(
                J.ClassDeclaration classDecl, ExecutionContext ctx) {
            classDecl = super.visitClassDeclaration(classDecl, ctx);
            JavaType.FullyQualified type = classDecl.getType();
            if (oldName.equals(classDecl.getSimpleName()) && type != null) {
                String availableName = availableName(type);
                classDecl = classDecl.withName(classDecl.getName().withSimpleName(availableName));
                doAfterVisit(new RenameTypeVisitor(type.getFullyQualifiedName(), oldName, availableName));
            }
            return classDecl;
        }

        /// The first name of the form `__`, `___`, ... free anywhere in the file and uninherited by any type it
        /// declares. Stricter than scoping the search to the declaration, but deterministic and it never merges
        /// two declarations into one.
        private String availableName() {
            // The root cursor is shared by every source file, so the message hangs off this one
            Cursor sourceFile = getCursor().dropParentUntil(JavaSourceFile.class::isInstance);
            return firstAvailableName(sourceFile.computeMessageIfAbsent(NAMES_IN_USE,
                    k -> namesInUse(sourceFile.getValue())));
        }

        /// Like `availableName()`, but for a class whose rename may also rename the file, so candidates whose
        /// target path another source file occupies are skipped rather than silently overwriting it.
        private String availableName(JavaType.FullyQualified type) {
            Cursor cursor = getCursor().dropParentUntil(JavaSourceFile.class::isInstance);
            JavaSourceFile sourceFile = cursor.getValue();
            Set<String> namesInUse = cursor.computeMessageIfAbsent(NAMES_IN_USE,
                    k -> namesInUse(sourceFile));
            Path sourcePath = sourceFile.getSourcePath();
            // Mirrors `RenameTypeVisitor#visitCompilationUnit`
            boolean renamesFile = type.getFullyQualifiedName().indexOf('$') < 0 &&
                    (oldName + ".java").equals(sourcePath.getFileName().toString());
            StringBuilder availableName = new StringBuilder(newName);
            while (namesInUse.contains(availableName.toString()) ||
                    (renamesFile && sourcePaths.contains(sourcePath.resolveSibling(availableName + ".java")))) {
                availableName.append('_');
            }
            return availableName.toString();
        }

        /// The first name of the form `__`, `___`, ... unused by the type rooting the override chain or its
        /// supertypes. Deriving it from that root rather than from the file being visited makes every override
        /// and caller arrive at the same name, so the override links survive. Fields are a separate namespace.
        private String availableName(JavaType.Method methodType) {
            JavaType.Method root = methodType;
            Set<String> visited = new HashSet<>();
            while (visited.add(root.getDeclaringType().getFullyQualifiedName())) {
                Optional<JavaType.Method> overridden = TypeUtils.findOverriddenMethod(root);
                if (!overridden.isPresent()) {
                    break;
                }
                root = overridden.get();
            }
            Set<String> namesInUse = new HashSet<>();
            addInheritedNames(root.getDeclaringType(), namesInUse, new HashSet<>(), false);
            return firstAvailableName(namesInUse);
        }

        private String firstAvailableName(Set<String> namesInUse) {
            StringBuilder availableName = new StringBuilder(newName);
            while (namesInUse.contains(availableName.toString())) {
                availableName.append('_');
            }
            return availableName.toString();
        }

        private static Set<String> namesInUse(JavaSourceFile cu) {
            Set<String> namesInUse = new HashSet<>();
            new JavaIsoVisitor<Set<String>>() {
                @Override
                public J.Identifier visitIdentifier(J.Identifier identifier, Set<String> names) {
                    names.add(identifier.getSimpleName());
                    return super.visitIdentifier(identifier, names);
                }

                @Override
                public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Set<String> names) {
                    addInheritedNames(classDecl.getType(), names, new HashSet<>(), true);
                    return super.visitClassDeclaration(classDecl, names);
                }
            }.visit(cu, namesInUse);
            return namesInUse;
        }

        private static void addInheritedNames(JavaType.@Nullable FullyQualified type,
                                              Set<String> names, Set<String> seen, boolean includeFields) {
            if (type == null || !seen.add(type.getFullyQualifiedName())) {
                return;
            }
            if (includeFields) {
                for (JavaType.Variable member : type.getMembers()) {
                    names.add(member.getName());
                }
            }
            for (JavaType.Method method : type.getMethods()) {
                names.add(method.getName());
            }
            addInheritedNames(type.getSupertype(), names, seen, includeFields);
            for (JavaType.FullyQualified anInterface : type.getInterfaces()) {
                addInheritedNames(anInterface, names, seen, includeFields);
            }
        }
    }

    /// Renames the references bound to a just-renamed type — declared types, casts, `instanceof`, class
    /// literals, `new` and its explicit constructors — within the declaring compilation unit only. An identifier
    /// must both spell the old name and resolve to that type, so unrelated same-named ones are left alone.
    @Value
    @EqualsAndHashCode(callSuper = false)
    static class RenameTypeVisitor extends JavaIsoVisitor<ExecutionContext> {

        String fullyQualifiedName;
        String oldName;
        String newName;

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            J.CompilationUnit compilationUnit = super.visitCompilationUnit(cu, ctx);
            // A public top level type must live in a file named after it, so rename the file too
            Path sourcePath = compilationUnit.getSourcePath();
            if (fullyQualifiedName.indexOf('$') < 0 &&
                    (oldName + ".java").equals(sourcePath.getFileName().toString())) {
                return compilationUnit.withSourcePath(sourcePath.resolveSibling(newName + ".java"));
            }
            return compilationUnit;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            J.MethodDeclaration visitedMethod = super.visitMethodDeclaration(method, ctx);
            if (visitedMethod.isConstructor() && oldName.equals(visitedMethod.getSimpleName()) &&
                    visitedMethod.getMethodType() != null &&
                    TypeUtils.isOfClassType(visitedMethod.getMethodType().getDeclaringType(), fullyQualifiedName)) {
                // Only the printed name changes; the method type keeps its `<constructor>` identity
                return visitedMethod.withName(visitedMethod.getName().withSimpleName(newName));
            }
            return visitedMethod;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
            J.Identifier visitedIdentifier = super.visitIdentifier(identifier, ctx);
            if (oldName.equals(visitedIdentifier.getSimpleName()) &&
                    TypeUtils.isOfClassType(visitedIdentifier.getType(), fullyQualifiedName)) {
                return visitedIdentifier.withSimpleName(newName);
            }
            return visitedIdentifier;
        }
    }
}
