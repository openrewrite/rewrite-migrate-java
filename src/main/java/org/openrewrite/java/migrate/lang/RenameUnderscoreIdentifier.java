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

        /// The paths of every source file in the run, so that renaming a class, which may also
        /// rename its file, never picks a name whose file another source file already occupies.
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
            // A constructor carries the name of the class it belongs to, so it is renamed along with
            // the class declaration, which keeps its `<constructor>` method type intact.
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

        /// The first name of the form `__`, `___`, ... that is not already taken anywhere in the
        /// source file, nor inherited by any of the types it declares. Picking a name that is free
        /// in the whole file is stricter than picking one that is free in the declaration's own
        /// scope, but it is deterministic and never merges two declarations into one. Used for
        /// variable declaration renames; references to a renamed type from other compilation
        /// units are deliberately not followed.
        private String availableName() {
            // The message has to hang off the source file rather than off the root cursor, which is
            // shared by every source file in the run.
            Cursor sourceFile = getCursor().dropParentUntil(JavaSourceFile.class::isInstance);
            return firstAvailableName(sourceFile.computeMessageIfAbsent(NAMES_IN_USE,
                    k -> namesInUse(sourceFile.getValue())));
        }

        /// Like `availableName()`, but for a class declaration, whose rename may also rename the
        /// source file: when it does, every candidate whose target file path is already occupied
        /// by another source file is skipped as well, so the rename never moves this compilation
        /// unit onto an existing one, which would silently overwrite it on write.
        private String availableName(JavaType.FullyQualified type) {
            Cursor cursor = getCursor().dropParentUntil(JavaSourceFile.class::isInstance);
            JavaSourceFile sourceFile = cursor.getValue();
            Set<String> namesInUse = cursor.computeMessageIfAbsent(NAMES_IN_USE,
                    k -> namesInUse(sourceFile));
            Path sourcePath = sourceFile.getSourcePath();
            // Mirrors the file rename condition in RenameTypeVisitor#visitCompilationUnit.
            boolean renamesFile = type.getFullyQualifiedName().indexOf('$') < 0 &&
                    (oldName + ".java").equals(sourcePath.getFileName().toString());
            StringBuilder availableName = new StringBuilder(newName);
            while (namesInUse.contains(availableName.toString()) ||
                    (renamesFile && sourcePaths.contains(sourcePath.resolveSibling(availableName + ".java")))) {
                availableName.append('_');
            }
            return availableName.toString();
        }

        /// The first name of the form `__`, `___`, ... that no method of the type declaring the
        /// root of the override chain, or of any of its supertypes, already uses. The name is
        /// derived from that root rather than from the file being visited, so that a method, every
        /// declaration that overrides or implements it, and every compilation unit calling it all
        /// arrive at the same name and the override links survive the rename. Only method names
        /// are considered: fields occupy a separate namespace and cannot collide with a method.
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

    /// Renames the references bound to a type that was just renamed: field, return, parameter and
    /// local variable types, casts, `instanceof`, class literals and `new` expressions, plus the
    /// explicit constructors declared on it, all within the compilation unit that declares the
    /// type; references from other compilation units are not updated. Only identifiers that both
    /// spell the old name and resolve to that very type are touched, so unrelated same-named
    /// identifiers are left alone.
    @Value
    @EqualsAndHashCode(callSuper = false)
    static class RenameTypeVisitor extends JavaIsoVisitor<ExecutionContext> {

        String fullyQualifiedName;
        String oldName;
        String newName;

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            J.CompilationUnit c = super.visitCompilationUnit(cu, ctx);
            // A public top level type must live in a file named after it, so when the file is
            // named after the type, rename the file too.
            Path sourcePath = c.getSourcePath();
            if (fullyQualifiedName.indexOf('$') < 0 &&
                    (oldName + ".java").equals(sourcePath.getFileName().toString())) {
                return c.withSourcePath(sourcePath.resolveSibling(newName + ".java"));
            }
            return c;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
            if (m.isConstructor() && oldName.equals(m.getSimpleName()) && m.getMethodType() != null &&
                    TypeUtils.isOfClassType(m.getMethodType().getDeclaringType(), fullyQualifiedName)) {
                // Only the printed name changes; the method type keeps its `<constructor>` identity.
                return m.withName(m.getName().withSimpleName(newName));
            }
            return m;
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
            J.Identifier i = super.visitIdentifier(identifier, ctx);
            if (oldName.equals(i.getSimpleName()) && TypeUtils.isOfClassType(i.getType(), fullyQualifiedName)) {
                return i.withSimpleName(newName);
            }
            return i;
        }
    }
}
