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
package org.openrewrite.java.migrate.lombok;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import org.openrewrite.java.tree.J;

import java.lang.annotation.Annotation;
import java.util.Set;

import static java.util.Collections.singleton;

@EqualsAndHashCode(callSuper = false)
@Value
public class UseLombokGetterOnType extends LombokAccessorOnType {

    String displayName = "Use class-level Lombok `@Getter` annotation";

    String description = "Replace default field-level Lombok `@Getter` annotations with a class-level annotation when they apply to every eligible field.";

    Set<String> tags = singleton("lombok");

    @Override
    protected Class<? extends Annotation> accessorAnnotation() {
        return Getter.class;
    }

    @Override
    protected boolean isEligibleForTypeLevelAccessor(J.VariableDeclarations field,
                                                      J.VariableDeclarations.NamedVariable variable) {
        return !isStaticField(field, variable) && !hasSyntheticFieldName(variable);
    }
}
