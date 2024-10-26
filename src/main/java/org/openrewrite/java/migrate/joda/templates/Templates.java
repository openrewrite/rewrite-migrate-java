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

import org.openrewrite.java.tree.MethodCall;

import java.util.List;

public interface Templates {
    List<MethodTemplate> getTemplates();

    /**
     * This method is used to disambiguate between multiple potential template matches for a given methodCall.
     * This should be overridden by Templates classes where methodMatcher.matches() may return more than one template.
     **/
    default boolean matchesMethodCall(MethodCall method, MethodTemplate template) {
        return true;
    }
}
