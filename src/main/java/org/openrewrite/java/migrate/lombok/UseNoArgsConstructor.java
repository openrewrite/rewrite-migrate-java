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
package org.openrewrite.java.migrate.lombok;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

import static java.util.Comparator.comparing;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseNoArgsConstructor extends Recipe {

    @Override
    public String getDisplayName() {
        //language=markdown
        return "Use `@NoArgsConstructor` where applicable";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Prefer the lombok annotation `@NoArgsConstructor` over explicitly written out constructors.\n" +
                "This recipe does not create annotations for implicit constructors.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            public static final String FOUND_EMPTY_CONSTRUCTOR = "FOUND_EMPTY_CONSTRUCTOR";


            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration classDeclAfterVisit = super.visitClassDeclaration(classDecl, ctx);

                J.MethodDeclaration message = getCursor().pollMessage(FOUND_EMPTY_CONSTRUCTOR);

                //if no constructor is found return immediately
                if (message == null) {
                    return classDecl;//since nothing changed the original can be returned
                }

                maybeAddImport("lombok.NoArgsConstructor");

                AccessLevel accessLevel = LombokUtils.getAccessLevel(message);

                return getAnnotation(accessLevel).apply(
                        updateCursor(classDeclAfterVisit),
                        classDeclAfterVisit.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
            }

            private JavaTemplate getAnnotation(AccessLevel accessLevel) {

                JavaTemplate.Builder builder = AccessLevel.PUBLIC.equals(accessLevel)
                        ? JavaTemplate.builder("@NoArgsConstructor()\n")
                        : JavaTemplate.builder("@NoArgsConstructor(access = AccessLevel." + accessLevel.name() + ")\n")
                        .imports("lombok.AccessLevel");

                return builder
                        .imports("lombok.NoArgsConstructor")
                        .javaParser(JavaParser.fromJavaVersion()
                                .classpath("lombok"))
                        .build();
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                assert method.getMethodType() != null;
                if (method.getMethodType().getName().equals("<constructor>") //it's a constructor
                        && method.getParameters().get(0) instanceof J.Empty  //no parameters
                        && method.getBody().getStatements().isEmpty()        //no side effects (=> does nothing)
                ) {
                    getCursor().putMessageOnFirstEnclosing(J.ClassDeclaration.class, FOUND_EMPTY_CONSTRUCTOR, method);
                    return null;
                }
                return super.visitMethodDeclaration(method, ctx);
            }

        };
    }

}
