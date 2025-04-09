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
package org.openrewrite.java.migrate.util;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import org.openrewrite.java.template.RecipeDescriptor;

import java.util.concurrent.ThreadLocalRandom;

@RecipeDescriptor(
        name = "Replace `java.lang.Math random()` with `ThreadLocalRandom nextDouble()`",
        description = "Replace `java.lang.Math random()` with `ThreadLocalRandom nextDouble()` to reduce contention."
)
public class ReplaceMathRandomWithThreadLocalRandom {
    @BeforeTemplate
    double javaMathRandom() {
        return Math.random();
    }

    @AfterTemplate
    double threadLocalRandomNextDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }
}
