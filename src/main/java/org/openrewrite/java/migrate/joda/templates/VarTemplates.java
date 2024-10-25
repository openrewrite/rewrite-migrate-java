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
package org.openrewrite.java.migrate.joda.templates;

import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.HashMap;
import java.util.Map;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

public class VarTemplates {

    private static Map<String, String> JodaToJavaTimeType = new HashMap<String, String>() {
        {
            put(JODA_DATE_TIME, JAVA_DATE_TIME);
            put(JODA_TIME_FORMATTER, JAVA_TIME_FORMATTER);
            put(JODA_LOCAL_DATE, JAVA_LOCAL_DATE);
            put(JODA_LOCAL_TIME, JAVA_LOCAL_TIME);
            put(JODA_DATE_TIME_ZONE, JAVA_ZONE_ID);
            put(JODA_DURATION, JAVA_DURATION);
        }
    };

    public static JavaTemplate getTemplate(J.VariableDeclarations variable) {
        JavaType.Class type = (JavaType.Class) variable.getTypeExpression().getType();
        String typeName = JodaToJavaTimeType.get(type.getFullyQualifiedName());
        StringBuilder template = new StringBuilder();
        String varName;
        try {
            varName = Class.forName(typeName).getSimpleName();
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown type " + typeName);
        }
        template.append(varName);
        template.append(" ");
        for (int i = 0; i < variable.getVariables().size(); i++) {
            if (i > 0) {
                template.append(", ");
            }
            template.append("#{}");
            if (variable.getVariables().get(i).getInitializer() != null) {
                template.append(" = #{any(");
                template.append(typeName);
                template.append(")}");
            }
        }
        return JavaTemplate.builder(template.toString())
                .imports(typeName)
                .build();
    }

    public static JavaTemplate getTemplate(J.Assignment assignment) {
        JavaType.Class type = (JavaType.Class) assignment.getAssignment().getType();
        String typeName = JodaToJavaTimeType.get(type.getFullyQualifiedName());
        StringBuilder template = new StringBuilder();
        assert assignment.getVariable() instanceof J.Identifier;
        J.Identifier varName = (J.Identifier) assignment.getVariable();
        template.append(varName.getSimpleName());
        template.append(" = #{any(");
        template.append(typeName);
        template.append(")}");
        return JavaTemplate.builder(template.toString())
                .build();
    }

    public static Object[] getTemplateArgs(J.VariableDeclarations variable) {
        Object[] args = new Object[variable.getVariables().size() * 2];
        int i = 0;
        for (J.VariableDeclarations.NamedVariable var : variable.getVariables()) {
            args[i++] = var.getSimpleName();
            if (var.getInitializer() != null) {
                args[i++] = var.getInitializer();
            }
        }
        Object[] args2 = new Object[i];
        System.arraycopy(args, 0, args2, 0, i);
        return args2;
    }
}
