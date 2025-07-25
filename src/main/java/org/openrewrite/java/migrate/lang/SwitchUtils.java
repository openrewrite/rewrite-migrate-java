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

import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import static java.util.stream.Collectors.toSet;

class SwitchUtils {
    /**
     * Checks if a switch statement covers all possible values of its selector.
     * This is typically used to determine if a switch statement is "exhaustive" as per the Java language specification.
     * <p>
     * NOTE: Missing support for sealed classes/interfaces.
     *
     * @param switch_ the switch statement to check
     * @return true if the switch covers all possible values, false otherwise
     * @see <a href="https://docs.oracle.com/en/java/javase/21/language/switch-expressions-and-statements.html">Switch Expressions in Java 21</a>
     */
    public static boolean coversAllPossibleValues(J.Switch switch_) {
        List<J> labels = new ArrayList<>();
        for (Statement statement : switch_.getCases().getStatements()) {
            for (J j : ((J.Case) statement).getCaseLabels()) {
                if (j instanceof J.Identifier && "default".equals(((J.Identifier) j).getSimpleName())) {
                    return true;
                }
                labels.add(j);
            }
        }
        JavaType javaType = switch_.getSelector().getTree().getType();
        if (javaType instanceof JavaType.Class) {
            JavaType.Class javaTypeClass = (JavaType.Class) javaType;
            if (javaTypeClass.hasFlags(Flag.Enum)) {
                Collection<String> labelValues = labels.stream()
                        .filter(label -> label instanceof J.Identifier || label instanceof J.FieldAccess)
                        .filter(label -> TypeUtils.isOfType(((TypeTree) label).getType(), javaType))
                        .map(label -> label instanceof J.Identifier ?
                                ((J.Identifier) label).getSimpleName() :
                                ((J.FieldAccess) label).getName().getSimpleName())
                        .collect(toSet());
                if (labelValues.isEmpty()) {
                    return false;
                }
                Collection<String> enumValues = javaTypeClass.getMembers().stream()
                        .filter(member -> member.hasFlags(Flag.Enum))
                        .map(JavaType.Variable::getName)
                        .collect(toSet());
                // Every enum value must be present in the switch
                return !enumValues.isEmpty() && labelValues.containsAll(enumValues);
            }
        }
        return false;
    }
}
