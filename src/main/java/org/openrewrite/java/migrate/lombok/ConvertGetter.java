/*
 * Copyright 2021 the original author or authors.
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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import static java.util.Comparator.comparing;
import static org.openrewrite.java.tree.JavaType.Variable;

@Value
@EqualsAndHashCode(callSuper = false)
public class ConvertGetter extends Recipe {

    @Override
    public String getDisplayName() {
        //language=markdown
        return "Convert getter methods to annotations";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return new StringJoiner("\n")
                .add("Convert trivial getter methods to `@Getter` annotations on their respective fields.")
                .add("")
                .add("Limitations:")
                .add("")
                .add(" - Does not add a dependency to Lombok, users need to do that manually")
                .add(" - Ignores fields that are declared on the same line as others, e.g. `private int foo, bar;" +
                        "Users who have such fields are advised to separate them beforehand with " +
                        "[org.openrewrite.staticanalysis.MultipleVariableDeclaration]" +
                        "(https://docs.openrewrite.org/recipes/staticanalysis/multiplevariabledeclarations).")
                .add(" - Does not offer any of the configuration keys listed in https://projectlombok.org/features/GetterSetter.")
                .toString();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MethodRemover();
    }


    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class MethodRemover extends JavaIsoVisitor<ExecutionContext> {
        private static final String FIELDS_TO_DECORATE_KEY = "FIELDS_TO_DECORATE";

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {

            //initialize set of fields to annotate
            getCursor().putMessage(FIELDS_TO_DECORATE_KEY, new HashSet<Finding>());

            //delete methods, note down corresponding fields
            J.ClassDeclaration classDeclAfterVisit = super.visitClassDeclaration(classDecl, ctx);

            //only thing that can have changed is removal of getter methods
            if (classDeclAfterVisit != classDecl) {
                //this set collects the fields for which existing methods have already been removed
                Set<Finding> fieldsToDecorate = getCursor().pollNearestMessage(FIELDS_TO_DECORATE_KEY);
                doAfterVisit(new FieldAnnotator(fieldsToDecorate));
            }
            return classDeclAfterVisit;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            assert method.getMethodType() != null;

            if (LombokUtils.isEffectivelyGetter(method)) {
                J.Return return_ = (J.Return) method.getBody().getStatements().get(0);
                Variable fieldType = ((J.Identifier) return_.getExpression()).getFieldType();
                boolean nameMatch = method.getSimpleName().equals(LombokUtils.deriveGetterMethodName(fieldType));
                if (nameMatch){
                    ((Set<Finding>) getCursor().getNearestMessage(FIELDS_TO_DECORATE_KEY))
                            .add(new Finding(fieldType.getName(), LombokUtils.getAccessLevel(method.getModifiers())));
                    return null; //delete
                }
            }
            return method;
        }
    }

    @Value
    private static class Finding {
        String fieldName;
        AccessLevel accessLevel;
    }


    @Value
    @EqualsAndHashCode(callSuper = false)
    static class FieldAnnotator extends JavaIsoVisitor<ExecutionContext>{

        Set<Finding> fieldsToDecorate;

        private JavaTemplate getAnnotation(AccessLevel accessLevel) {
            JavaTemplate.Builder builder = AccessLevel.PUBLIC.equals(accessLevel)
                    ? JavaTemplate.builder("@Getter\n")
                    : JavaTemplate.builder("@Getter(AccessLevel." + accessLevel.name() + ")\n")
                    .imports("lombok.AccessLevel");

            return builder
                    .imports("lombok.Getter")
                    .javaParser(JavaParser.fromJavaVersion()
                            .classpath("lombok"))
                    .build();
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {

            //we accept only one var decl per line, see description
            if (multiVariable.getVariables().size() > 1) {
                return multiVariable;
            }

            J.VariableDeclarations.NamedVariable variable = multiVariable.getVariables().get(0);
            Optional<Finding> field = fieldsToDecorate.stream()
                    .filter(f -> f.fieldName.equals(variable.getSimpleName()))
                    .findFirst();

            if (!field.isPresent()) {
                return multiVariable; //not the field we are looking for
            }

            J.VariableDeclarations annotated = getAnnotation(field.get().getAccessLevel()).apply(
                    getCursor(),
                    multiVariable.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
            maybeAddImport("lombok.Getter");
            maybeAddImport("lombok.AccessLevel");
            return annotated;
        }
    }
}
