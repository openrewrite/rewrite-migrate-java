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
package org.openrewrite.java.migrate.joda.templates;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.JavaType;

import java.util.HashMap;
import java.util.Map;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

public class TimeClassMap {

    private static final JavaType.Class object = JavaType.ShallowClass.build("java.lang.Object");

    private final Map<String, JavaType.Class> jodaToJavaTimeMap;
    {
        jodaToJavaTimeMap = new HashMap<>();
        jodaToJavaTimeMap.put(JODA_DATE_TIME, jodaToJavaTimeMap.javaTypeClass(JAVA_DATE_TIME, object));
        jodaToJavaTimeMap.put(JODA_BASE_DATE_TIME, jodaToJavaTimeMap.javaTypeClass(JAVA_DATE_TIME, object));
        jodaToJavaTimeMap.put(JODA_DATE_TIME_ZONE, jodaToJavaTimeMap.javaTypeClass(JAVA_ZONE_ID, object));
        jodaToJavaTimeMap.put(JODA_TIME_FORMATTER, jodaToJavaTimeMap.javaTypeClass(JAVA_TIME_FORMATTER, object));
        jodaToJavaTimeMap.put(JODA_DURATION, jodaToJavaTimeMap.javaTypeClass(JAVA_DURATION, object));
        jodaToJavaTimeMap.put(JODA_READABLE_DURATION, jodaToJavaTimeMap.javaTypeClass(JAVA_DURATION, object));
        jodaToJavaTimeMap.put(JODA_INTERVAL, jodaToJavaTimeMap.javaTypeClass(THREE_TEN_EXTRA_INTERVAL, object));
    }

    private final Map<String, String> jodaToJavaTimeShortName;
    {
        jodaToJavaTimeShortName = new HashMap<>();
        jodaToJavaTimeShortName.put(JODA_DATE_TIME, "ZonedDateTime");
    }

    private static JavaType.Class javaTypeClass(String fqn, JavaType.Class superType) {
        return new JavaType.Class(null, 0, fqn, JavaType.FullyQualified.Kind.Class, null, superType,
                null, null, null, null, null);
    }

    public static JavaType.@Nullable Class getJavaTimeType(String typeFqn) {
        return new TimeClassMap().jodaToJavaTimeMap.get(typeFqn);
    }

    public static @Nullable String getJavaTimeShortName(String typeFqn) {
        return new TimeClassMap().jodaToJavaTimeShortName.get(typeFqn);
    }
}
