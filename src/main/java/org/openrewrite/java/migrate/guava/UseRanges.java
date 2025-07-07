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
package org.openrewrite.java.migrate.guava;

import com.google.common.collect.Range;
import com.google.errorprone.refaster.ImportPolicy;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.UseImportPolicy;
import org.openrewrite.java.template.RecipeDescriptor;

@RecipeDescriptor(
        name = "Use Guava Ranges",
        description = "Simplifies hand crafted range checks."
)
public class UseRanges {
    @RecipeDescriptor(
            name = "Replace `from.compareTo(candidate) <= 0 && candidate.compareTo(to) <= 0` with a guava `Range.closed(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in a closed interval ( candidate ∈ [from, to] ) with a guava range expression`."
    )
    public static class RangeClosed<T extends Comparable<T>> {
        @BeforeTemplate
        boolean simple(T from, T candidate, T to) {
            return from.compareTo(candidate) <= 0 &&
                    candidate.compareTo(to) <= 0;
        }

        @BeforeTemplate
        boolean candidateAsArgument(T from, T candidate, T to) {
            return from.compareTo(candidate) <= 0 &&
                    to.compareTo(candidate) >= 0;
        }

        @BeforeTemplate
        boolean candidateAsBase(T from, T candidate, T to) {
            return candidate.compareTo(from) >= 0 &&
                    candidate.compareTo(to) <= 0;
        }

        @BeforeTemplate
        boolean flipped(T from, T candidate, T to) {
            return candidate.compareTo(to) <= 0 &&
                    from.compareTo(candidate) <= 0;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(T from, T candidate, T to) {
            return Range.closed(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from.compareTo(candidate) < 0 && candidate.compareTo(to) < 0` with a guava `Range.open(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in an open interval ( candidate ∈ (from, to) ) with a guava range expression`."
    )
    public static class RangeOpen<T extends Comparable<T>> {
        @BeforeTemplate
        boolean simple(T from, T candidate, T to) {
            return from.compareTo(candidate) < 0 &&
                    candidate.compareTo(to) < 0;
        }

        @BeforeTemplate
        boolean candidateAsArgument(T from, T candidate, T to) {
            return from.compareTo(candidate) < 0 &&
                    to.compareTo(candidate) > 0;
        }

        @BeforeTemplate
        boolean candidateAsBase(T from, T candidate, T to) {
            return candidate.compareTo(from) > 0 &&
                    candidate.compareTo(to) < 0;
        }

        @BeforeTemplate
        boolean flipped(T from, T candidate, T to) {
            return candidate.compareTo(to) < 0 &&
                    from.compareTo(candidate) < 0;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(T from, T candidate, T to) {
            return Range.open(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from.compareTo(candidate) <= 0 && candidate.compareTo(to) < 0` with a guava `Range.closedOpen(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in an interval that is open to the right ( candidate ∈ [from, to) ) with a guava range expression`."
    )
    public static class RangeClosedOpen<T extends Comparable<T>> {
        @BeforeTemplate
        boolean simple(T from, T candidate, T to) {
            return from.compareTo(candidate) <= 0 &&
                    candidate.compareTo(to) < 0;
        }

        @BeforeTemplate
        boolean candidateAsArgument(T from, T candidate, T to) {
            return from.compareTo(candidate) <= 0 &&
                    to.compareTo(candidate) > 0;
        }

        @BeforeTemplate
        boolean candidateAsBase(T from, T candidate, T to) {
            return candidate.compareTo(from) >= 0 &&
                    candidate.compareTo(to) < 0;
        }

        @BeforeTemplate
        boolean flipped(T from, T candidate, T to) {
            return candidate.compareTo(to) < 0 &&
                    from.compareTo(candidate) <= 0;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(T from, T candidate, T to) {
            return Range.closedOpen(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from.compareTo(candidate) < 0 && candidate.compareTo(to) <= 0` with a guava `Range.openClosed(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in an interval that is open to the left ( candidate ∈ (from, to] ) with a guava range expression`."
    )
    public static class RangeOpenClosed<T extends Comparable<T>> {
        @BeforeTemplate
        boolean simple(T from, T candidate, T to) {
            return from.compareTo(candidate) < 0 &&
                    candidate.compareTo(to) <= 0;
        }

        @BeforeTemplate
        boolean candidateAsArgument(T from, T candidate, T to) {
            return from.compareTo(candidate) < 0 &&
                    to.compareTo(candidate) >= 0;
        }

        @BeforeTemplate
        boolean candidateAsBase(T from, T candidate, T to) {
            return candidate.compareTo(from) > 0 &&
                    candidate.compareTo(to) <= 0;
        }

        @BeforeTemplate
        boolean flipped(T from, T candidate, T to) {
            return candidate.compareTo(to) <= 0 &&
                    from.compareTo(candidate) < 0;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(T from, T candidate, T to) {
            return Range.openClosed(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from <= candidate && candidate <= to` with a guava `Range.closed(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in a closed interval ( candidate ∈ [from, to] ) with a guava range expression`."
    )
    public static class RangeClosedPrimitiveInt {
        @BeforeTemplate
        boolean simple(int from, int candidate, int to) {
            return from <= candidate && candidate <= to;
        }

        @BeforeTemplate
        boolean candidateOnTheRight(int from, int candidate, int to) {
            return from <= candidate && to >= candidate;
        }

        @BeforeTemplate
        boolean candidateOnTheLeft(int from, int candidate, int to) {
            return candidate >= from && candidate <= to;
        }

        @BeforeTemplate
        boolean candidateOutside(int from, int candidate, int to) {
            return candidate <= to && from <= candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(int from, int candidate, int to) {
            return Range.closed(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from < candidate && candidate < to` with a guava `Range.open(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in an open interval ( candidate ∈ (from, to) ) with a guava range expression`."
    )
    public static class RangeOpenPrimitiveInt {
        @BeforeTemplate
        boolean simple(int from, int candidate, int to) {
            return from < candidate && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOnTheRight(int from, int candidate, int to) {
            return from < candidate && to > candidate;
        }

        @BeforeTemplate
        boolean candidateOnTheLeft(int from, int candidate, int to) {
            return candidate > from && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOutside(int from, int candidate, int to) {
            return candidate < to && from < candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(int from, int candidate, int to) {
            return Range.open(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from <= candidate && candidate < to` with a guava `Range.closedOpen(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in an interval that is open to the right ( candidate ∈ [from, to) ) with a guava range expression`."
    )
    public static class RangeClosedOpenPrimitiveInt {
        @BeforeTemplate
        boolean simple(int from, int candidate, int to) {
            return from <= candidate && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOnTheRight(int from, int candidate, int to) {
            return from <= candidate && to > candidate;
        }

        @BeforeTemplate
        boolean candidateOnTheLeft(int from, int candidate, int to) {
            return candidate >= from && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOutside(int from, int candidate, int to) {
            return candidate < to && from <= candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(int from, int candidate, int to) {
            return Range.closedOpen(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from < candidate && candidate <= to` with a guava `Range.openClosed(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in an interval that is open to the left ( candidate ∈ (from, to] ) with a guava range expression`."
    )
    public static class RangeOpenClosedPrimitiveInt {
        @BeforeTemplate
        boolean simple(int from, int candidate, int to) {
            return from < candidate && candidate <= to;
        }

        @BeforeTemplate
        boolean candidateAsArgument(int from, int candidate, int to) {
            return from < candidate && to >= candidate;
        }

        @BeforeTemplate
        boolean candidateAsBase(int from, int candidate, int to) {
            return candidate > from && candidate <= to;
        }

        @BeforeTemplate
        boolean flipped(int from, int candidate, int to) {
            return candidate <= to && from < candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(int from, int candidate, int to) {
            return Range.openClosed(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from <= candidate && candidate <= to` with a guava `Range.closed(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in a closed interval ( candidate ∈ [from, to] ) with a guava range expression`."
    )
    public static class RangeClosedPrimitiveDouble {
        @BeforeTemplate
        boolean simple(double from, double candidate, double to) {
            return from <= candidate && candidate <= to;
        }

        @BeforeTemplate
        boolean candidateOnTheRight(double from, double candidate, double to) {
            return from <= candidate && to >= candidate;
        }

        @BeforeTemplate
        boolean candidateOnTheLeft(double from, double candidate, double to) {
            return candidate >= from && candidate <= to;
        }

        @BeforeTemplate
        boolean candidateOutside(double from, double candidate, double to) {
            return candidate <= to && from <= candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(double from, double candidate, double to) {
            return Range.closed(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from < candidate && candidate < to` with a guava `Range.open(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in an open interval ( candidate ∈ (from, to) ) with a guava range expression`."
    )
    public static class RangeOpenPrimitiveDouble {
        @BeforeTemplate
        boolean simple(double from, double candidate, double to) {
            return from < candidate && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOnTheRight(double from, double candidate, double to) {
            return from < candidate && to > candidate;
        }

        @BeforeTemplate
        boolean candidateOnTheLeft(double from, double candidate, double to) {
            return candidate > from && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOutside(double from, double candidate, double to) {
            return candidate < to && from < candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(double from, double candidate, double to) {
            return Range.open(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from <= candidate && candidate < to` with a guava `Range.closedOpen(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in an interval that is open to the right ( candidate ∈ [from, to) ) with a guava range expression`."
    )
    public static class RangeClosedOpenPrimitiveDouble {
        @BeforeTemplate
        boolean simple(double from, double candidate, double to) {
            return from <= candidate && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOnTheRight(double from, double candidate, double to) {
            return from <= candidate && to > candidate;
        }

        @BeforeTemplate
        boolean candidateOnTheLeft(double from, double candidate, double to) {
            return candidate >= from && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOutside(double from, double candidate, double to) {
            return candidate < to && from <= candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(double from, double candidate, double to) {
            return Range.closedOpen(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from < candidate && candidate <= to` with a guava `Range.openClosed(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in an interval that is open to the left ( candidate ∈ (from, to] ) with a guava range expression`."
    )
    public static class RangeOpenClosedPrimitiveDouble {
        @BeforeTemplate
        boolean simple(double from, double candidate, double to) {
            return from < candidate && candidate <= to;
        }

        @BeforeTemplate
        boolean candidateAsArgument(double from, double candidate, double to) {
            return from < candidate && to >= candidate;
        }

        @BeforeTemplate
        boolean candidateAsBase(double from, double candidate, double to) {
            return candidate > from && candidate <= to;
        }

        @BeforeTemplate
        boolean flipped(double from, double candidate, double to) {
            return candidate <= to && from < candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(double from, double candidate, double to) {
            return Range.openClosed(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from <= candidate && candidate <= to` with a guava `Range.closed(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in a closed interval ( candidate ∈ [from, to] ) with a guava range expression`."
    )
    public static class RangeClosedPrimitiveFloat {
        @BeforeTemplate
        boolean simple(float from, float candidate, float to) {
            return from <= candidate && candidate <= to;
        }

        @BeforeTemplate
        boolean candidateOnTheRight(float from, float candidate, float to) {
            return from <= candidate && to >= candidate;
        }

        @BeforeTemplate
        boolean candidateOnTheLeft(float from, float candidate, float to) {
            return candidate >= from && candidate <= to;
        }

        @BeforeTemplate
        boolean candidateOutside(float from, float candidate, float to) {
            return candidate <= to && from <= candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(float from, float candidate, float to) {
            return Range.closed(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from < candidate && candidate < to` with a guava `Range.open(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in an open interval ( candidate ∈ (from, to) ) with a guava range expression`."
    )
    public static class RangeOpenPrimitiveFloat {
        @BeforeTemplate
        boolean simple(float from, float candidate, float to) {
            return from < candidate && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOnTheRight(float from, float candidate, float to) {
            return from < candidate && to > candidate;
        }

        @BeforeTemplate
        boolean candidateOnTheLeft(float from, float candidate, float to) {
            return candidate > from && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOutside(float from, float candidate, float to) {
            return candidate < to && from < candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(float from, float candidate, float to) {
            return Range.open(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from <= candidate && candidate < to` with a guava `Range.closedOpen(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in an interval that is open to the right ( candidate ∈ [from, to) ) with a guava range expression`."
    )
    public static class RangeClosedOpenPrimitiveFloat {
        @BeforeTemplate
        boolean simple(float from, float candidate, float to) {
            return from <= candidate && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOnTheRight(float from, float candidate, float to) {
            return from <= candidate && to > candidate;
        }

        @BeforeTemplate
        boolean candidateOnTheLeft(float from, float candidate, float to) {
            return candidate >= from && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOutside(float from, float candidate, float to) {
            return candidate < to && from <= candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(float from, float candidate, float to) {
            return Range.closedOpen(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from < candidate && candidate <= to` with a guava `Range.openClosed(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in an interval that is open to the left ( candidate ∈ (from, to] ) with a guava range expression`."
    )
    public static class RangeOpenClosedPrimitiveFloat {
        @BeforeTemplate
        boolean simple(float from, float candidate, float to) {
            return from < candidate && candidate <= to;
        }

        @BeforeTemplate
        boolean candidateAsArgument(float from, float candidate, float to) {
            return from < candidate && to >= candidate;
        }

        @BeforeTemplate
        boolean candidateAsBase(float from, float candidate, float to) {
            return candidate > from && candidate <= to;
        }

        @BeforeTemplate
        boolean flipped(float from, float candidate, float to) {
            return candidate <= to && from < candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(float from, float candidate, float to) {
            return Range.openClosed(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from <= candidate && candidate <= to` with a guava `Range.closed(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in a closed interval ( candidate ∈ [from, to] ) with a guava range expression`."
    )
    public static class RangeClosedPrimitiveShort {
        @BeforeTemplate
        boolean simple(short from, short candidate, short to) {
            return from <= candidate && candidate <= to;
        }

        @BeforeTemplate
        boolean candidateOnTheRight(short from, short candidate, short to) {
            return from <= candidate && to >= candidate;
        }

        @BeforeTemplate
        boolean candidateOnTheLeft(short from, short candidate, short to) {
            return candidate >= from && candidate <= to;
        }

        @BeforeTemplate
        boolean candidateOutside(short from, short candidate, short to) {
            return candidate <= to && from <= candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(short from, short candidate, short to) {
            return Range.closed(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from < candidate && candidate < to` with a guava `Range.open(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in an open interval ( candidate ∈ (from, to) ) with a guava range expression`."
    )
    public static class RangeOpenPrimitiveShort {
        @BeforeTemplate
        boolean simple(short from, short candidate, short to) {
            return from < candidate && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOnTheRight(short from, short candidate, short to) {
            return from < candidate && to > candidate;
        }

        @BeforeTemplate
        boolean candidateOnTheLeft(short from, short candidate, short to) {
            return candidate > from && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOutside(short from, short candidate, short to) {
            return candidate < to && from < candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(short from, short candidate, short to) {
            return Range.open(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from <= candidate && candidate < to` with a guava `Range.closedOpen(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in an interval that is open to the right ( candidate ∈ [from, to) ) with a guava range expression`."
    )
    public static class RangeClosedOpenPrimitiveShort {
        @BeforeTemplate
        boolean simple(short from, short candidate, short to) {
            return from <= candidate && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOnTheRight(short from, short candidate, short to) {
            return from <= candidate && to > candidate;
        }

        @BeforeTemplate
        boolean candidateOnTheLeft(short from, short candidate, short to) {
            return candidate >= from && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOutside(short from, short candidate, short to) {
            return candidate < to && from <= candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(short from, short candidate, short to) {
            return Range.closedOpen(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from < candidate && candidate <= to` with a guava `Range.openClosed(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in an interval that is open to the left ( candidate ∈ (from, to] ) with a guava range expression`."
    )
    public static class RangeOpenClosedPrimitiveShort {
        @BeforeTemplate
        boolean simple(short from, short candidate, short to) {
            return from < candidate && candidate <= to;
        }

        @BeforeTemplate
        boolean candidateAsArgument(short from, short candidate, short to) {
            return from < candidate && to >= candidate;
        }

        @BeforeTemplate
        boolean candidateAsBase(short from, short candidate, short to) {
            return candidate > from && candidate <= to;
        }

        @BeforeTemplate
        boolean flipped(short from, short candidate, short to) {
            return candidate <= to && from < candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(short from, short candidate, short to) {
            return Range.openClosed(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from <= candidate && candidate <= to` with a guava `Range.closed(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in a closed interval ( candidate ∈ [from, to] ) with a guava range expression`."
    )
    public static class RangeClosedPrimitiveLong {
        @BeforeTemplate
        boolean simple(long from, long candidate, long to) {
            return from <= candidate && candidate <= to;
        }

        @BeforeTemplate
        boolean candidateOnTheRight(long from, long candidate, long to) {
            return from <= candidate && to >= candidate;
        }

        @BeforeTemplate
        boolean candidateOnTheLeft(long from, long candidate, long to) {
            return candidate >= from && candidate <= to;
        }

        @BeforeTemplate
        boolean candidateOutside(long from, long candidate, long to) {
            return candidate <= to && from <= candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(long from, long candidate, long to) {
            return Range.closed(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from < candidate && candidate < to` with a guava `Range.open(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in an open interval ( candidate ∈ (from, to) ) with a guava range expression`."
    )
    public static class RangeOpenPrimitiveLong {
        @BeforeTemplate
        boolean simple(long from, long candidate, long to) {
            return from < candidate && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOnTheRight(long from, long candidate, long to) {
            return from < candidate && to > candidate;
        }

        @BeforeTemplate
        boolean candidateOnTheLeft(long from, long candidate, long to) {
            return candidate > from && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOutside(long from, long candidate, long to) {
            return candidate < to && from < candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(long from, long candidate, long to) {
            return Range.open(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from <= candidate && candidate < to` with a guava `Range.closedOpen(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in an interval that is open to the right ( candidate ∈ [from, to) ) with a guava range expression`."
    )
    public static class RangeClosedOpenPrimitiveLong {
        @BeforeTemplate
        boolean simple(long from, long candidate, long to) {
            return from <= candidate && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOnTheRight(long from, long candidate, long to) {
            return from <= candidate && to > candidate;
        }

        @BeforeTemplate
        boolean candidateOnTheLeft(long from, long candidate, long to) {
            return candidate >= from && candidate < to;
        }

        @BeforeTemplate
        boolean candidateOutside(long from, long candidate, long to) {
            return candidate < to && from <= candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(long from, long candidate, long to) {
            return Range.closedOpen(from, to).contains(candidate);
        }
    }

    @RecipeDescriptor(
            name = "Replace `from < candidate && candidate <= to` with a guava `Range.openClosed(from, to).contains(candidate)`",
            description = "Replace a hand crafted range check for membership in an interval that is open to the left ( candidate ∈ (from, to] ) with a guava range expression`."
    )
    public static class RangeOpenClosedPrimitiveLong {
        @BeforeTemplate
        boolean simple(long from, long candidate, long to) {
            return from < candidate && candidate <= to;
        }

        @BeforeTemplate
        boolean candidateAsArgument(long from, long candidate, long to) {
            return from < candidate && to >= candidate;
        }

        @BeforeTemplate
        boolean candidateAsBase(long from, long candidate, long to) {
            return candidate > from && candidate <= to;
        }

        @BeforeTemplate
        boolean flipped(long from, long candidate, long to) {
            return candidate <= to && from < candidate;
        }

        @UseImportPolicy(ImportPolicy.IMPORT_TOP_LEVEL)
        @AfterTemplate
        boolean after(long from, long candidate, long to) {
            return Range.openClosed(from, to).contains(candidate);
        }
    }
}
