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
