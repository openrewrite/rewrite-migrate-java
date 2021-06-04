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

import org.openrewrite.Recipe;

/**
 * This imperative recipe will add the jdeprsacn plugin to a maven project. In the case of a multi-module project,
 * this recipe will attempt to add the plugin to only the top level project.
 */
public class AddJdeprscanPlugin extends Recipe {

    @Override
    public String getDisplayName() {
        return "Add JDeprScan Maven Plug-in";
    }

    /*
          TODO Implement visitor to add the following plugin (favoring the top-level project of a multi-module project:

      groupId: org.apache.maven.plugins
      artifactId: maven-jdeprscan-plugin
      version: 3.0.0-alpha-1
      configuration: |-
        <configuration>
          <release>11</release>
        </configuration>

     */
}
