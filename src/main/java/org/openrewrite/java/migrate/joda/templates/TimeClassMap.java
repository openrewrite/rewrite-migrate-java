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

import org.openrewrite.java.tree.JavaType;

import java.util.HashMap;
import java.util.Map;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

public class TimeClassMap {

    private static final JavaType.Class object = JavaType.ShallowClass.build("java.lang.Object");

    private final Map<String, JavaType.Class> jodaToJavaTimeMap = new HashMap<String, JavaType.Class>() {
        {
            put(JODA_DATE_TIME, javaTypeClass(JAVA_DATE_TIME, object));
            put(JODA_BASE_DATE_TIME, javaTypeClass(JAVA_DATE_TIME, object));
            put(JODA_DATE_TIME_ZONE, javaTypeClass(JAVA_ZONE_ID, object));
            put(JODA_TIME_FORMATTER, javaTypeClass(JAVA_TIME_FORMATTER, object));
            put(JODA_DURATION, javaTypeClass(JAVA_DURATION, object));
            put(JODA_READABLE_DURATION, javaTypeClass(JAVA_DURATION, object));
        }
    };

    private static JavaType.Class javaTypeClass(String fqn, JavaType.Class superType) {
        return new JavaType.Class(null, 0, fqn, JavaType.FullyQualified.Kind.Class, null, superType,
                null, null, null, null, null);
    }

    public static JavaType.Class getJavaTimeType(String typeFqn) {
        return new TimeClassMap().jodaToJavaTimeMap.get(typeFqn);
    }
}
