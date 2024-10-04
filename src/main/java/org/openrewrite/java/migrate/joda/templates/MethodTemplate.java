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

import lombok.Value;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.MethodCall;

import java.util.function.Function;

@Value
public class MethodTemplate {
  MethodMatcher matcher;
  JavaTemplate template;
  Function<MethodCall, Expression[]> templateArgsFunc;

  public MethodTemplate(MethodMatcher matcher, JavaTemplate template) {
    this(matcher, template, m -> {
      Expression select = isInstanceCall(m) ? ((J.MethodInvocation) m).getSelect() : null;

      if (m.getArguments().isEmpty() || m.getArguments().get(0) instanceof J.Empty) {
        return select != null ? new Expression[]{select} : new Expression[0];
      }

      Expression[] args = m.getArguments().toArray(new Expression[0]);
      if (select != null) {
        Expression[] newArgs = new Expression[args.length + 1];
        newArgs[0] = select;
        System.arraycopy(args, 0, newArgs, 1, args.length);
        return newArgs;
      }
      return args;
    });
  }

  public MethodTemplate(MethodMatcher matcher, JavaTemplate template, Function<MethodCall, Expression[]> templateArgsFunc) {
    this.matcher = matcher;
    this.template = template;
    this.templateArgsFunc = templateArgsFunc;
  }

  private static boolean isInstanceCall(MethodCall m) {
    if (!(m instanceof J.MethodInvocation)) {
      return false;
    }
    J.MethodInvocation mi = (J.MethodInvocation) m;
    if (mi.getSelect() instanceof J.FieldAccess) {
      return ((J.FieldAccess) mi.getSelect()).getName().getFieldType() != null;
    }
    if (mi.getSelect() instanceof J.Identifier) {
      return ((J.Identifier) mi.getSelect()).getFieldType() != null;
    }
    return true;
  }
}
