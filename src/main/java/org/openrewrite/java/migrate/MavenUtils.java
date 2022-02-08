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
package org.openrewrite.java.migrate;

import org.openrewrite.SourceFile;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.xml.tree.Xml;

public class MavenUtils {
    private MavenUtils() {
    }

    public static boolean isMavenSource(SourceFile s) {
        return s instanceof Xml.Document && s.getMarkers().findFirst(MavenResolutionResult.class).isPresent();
    }

    public static MavenResolutionResult getMavenModel(SourceFile s) {
        return s.getMarkers().findFirst(MavenResolutionResult.class)
                .orElseThrow(() -> new IllegalStateException("Source file does not have a maven model."));
    }
}
