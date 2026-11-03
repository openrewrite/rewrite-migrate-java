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
package org.openrewrite.java.migrate.guava;

import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.errorprone.refaster.ImportPolicy;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import com.google.errorprone.refaster.annotation.UseImportPolicy;
import org.openrewrite.java.template.RecipeDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@RecipeDescriptor(
        name = "Refaster style Guava to Java migration recipes",
        description = "Recipes that migrate from Guava to Java, using Refaster style templates for cases beyond what declarative recipes can cover."
)
public class NoGuavaRefaster {

    @RecipeDescriptor(
            name = "Replace `Preconditions.checkNotNull` with `Objects.requireNonNull`",
            description = "Migrate from Guava `Preconditions.checkNotNull` to Java 8 `java.util.Objects.requireNonNull`."
    )
    public static class PreconditionsCheckNotNullToObjectsRequireNonNull {
        @BeforeTemplate
        Object before(Object object) {
            return com.google.common.base.Preconditions.checkNotNull(object);
        }

        @AfterTemplate
        @UseImportPolicy(ImportPolicy.STATIC_IMPORT_ALWAYS)
        Object after(Object object) {
            return java.util.Objects.requireNonNull(object);
        }
    }

    // Note: Order of the below two recipes are important.
    @RecipeDescriptor(
            name = "Replace `Preconditions.checkNotNull` taking a `String` message with `Objects.requireNonNull`",
            description = "Migrate from Guava `Preconditions.checkNotNull` to Java 8 `java.util.Objects.requireNonNull`."
    )
    public static class PreconditionsCheckNotNullWithMessageToObjectsRequireNonNull {
        @BeforeTemplate
        Object before(Object object, String message) {
            return com.google.common.base.Preconditions.checkNotNull(object, message);
        }

        @AfterTemplate
        @UseImportPolicy(ImportPolicy.STATIC_IMPORT_ALWAYS)
        Object after(Object object, String message) {
            return java.util.Objects.requireNonNull(object, message);
        }
    }

    @RecipeDescriptor(
        name = "Replace `Preconditions.checkNotNull` taking an `Object` message with `Objects.requireNonNull` and `String.valueOf`",
        description = "Migrate from Guava `Preconditions.checkNotNull` to Java 8 `java.util.Objects.requireNonNull`."
    )
    public static class PreconditionsCheckNotNullWithMessageToObjectsRequireNonNullMessageTypeObject {
        @BeforeTemplate
        Object before(Object object, Object message) {
            return com.google.common.base.Preconditions.checkNotNull(object, message);
        }

        @AfterTemplate
        @UseImportPolicy(ImportPolicy.STATIC_IMPORT_ALWAYS)
        Object after(Object object, Object message) {
            return java.util.Objects.requireNonNull(object, String.valueOf(message));
        }
    }

    @RecipeDescriptor(
            name = "Prefer `new ArrayList<>()`",
            description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this."
    )
    public static class ListsNewArrayList<T> {
        @BeforeTemplate
        ArrayList<T> before() {
            return Lists.newArrayList();
        }

        @AfterTemplate
        ArrayList<T> after() {
            return new ArrayList<>();
        }
    }

    @RecipeDescriptor(
            name = "Prefer `new ArrayList<>(Collection)`",
            description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this."
    )
    public static class ListsNewArrayListFromCollection<T> {
        @BeforeTemplate
        ArrayList<T> before(Collection<T> collection) {
            return Lists.newArrayList(collection);
        }

        @AfterTemplate
        ArrayList<T> after(Collection<T> collection) {
            return new ArrayList<>(collection);
        }
    }

    @RecipeDescriptor(
            name = "Prefer `new ArrayList<>(int)`",
            description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this."
    )
    public static class ListsNewArrayListWithCapacity<T> {
        @BeforeTemplate
        ArrayList<T> before(int initialCapacity) {
            return Lists.newArrayListWithCapacity(initialCapacity);
        }

        @AfterTemplate
        ArrayList<T> after(int initialCapacity) {
            return new ArrayList<>(initialCapacity);
        }
    }

    @RecipeDescriptor(
            name = "Prefer `new CopyOnWriteArrayList<>()`",
            description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this."
    )
    public static class ListsNewCopyOnWriteArrayList<T> {
        @BeforeTemplate
        CopyOnWriteArrayList<T> before() {
            return Lists.newCopyOnWriteArrayList();
        }

        @AfterTemplate
        CopyOnWriteArrayList<T> after() {
            return new CopyOnWriteArrayList<>();
        }
    }

    @RecipeDescriptor(
            name = "Prefer `new CopyOnWriteArrayList<>(Collection)`",
            description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this."
    )
    public static class ListsNewCopyOnWriteArrayListFromCollection<T> {
        @BeforeTemplate
        CopyOnWriteArrayList<T> before(Collection<T> collection) {
            return Lists.newCopyOnWriteArrayList(collection);
        }

        @AfterTemplate
        CopyOnWriteArrayList<T> after(Collection<T> collection) {
            return new CopyOnWriteArrayList<>(collection);
        }
    }

    @RecipeDescriptor(
            name = "Prefer `new LinkedList<>()`",
            description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this."
    )
    public static class ListsNewLinkedList<T> {
        @BeforeTemplate
        LinkedList<T> before() {
            return Lists.newLinkedList();
        }

        @AfterTemplate
        LinkedList<T> after() {
            return new LinkedList<>();
        }
    }

    @RecipeDescriptor(
            name = "Prefer `new LinkedList<>(Collection)`",
            description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this."
    )
    public static class ListsNewLinkedListFromCollection<T> {
        @BeforeTemplate
        LinkedList<T> before(Collection<T> collection) {
            return Lists.newLinkedList(collection);
        }

        @AfterTemplate
        LinkedList<T> after(Collection<T> collection) {
            return new LinkedList<>(collection);
        }
    }

    @RecipeDescriptor(
            name = "Prefer `new HashMap<>()`",
            description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this."
    )
    public static class MapsNewHashMap<K, V> {
        @BeforeTemplate
        HashMap<K, V> before() {
            return Maps.newHashMap();
        }

        @AfterTemplate
        HashMap<K, V> after() {
            return new HashMap<>();
        }
    }

    @RecipeDescriptor(
            name = "Prefer `new HashMap<>(Map)`",
            description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this."
    )
    public static class MapsNewHashMapFromMap<K, V> {
        @BeforeTemplate
        HashMap<K, V> before(Map<? extends K, ? extends V> map) {
            return Maps.newHashMap(map);
        }

        @AfterTemplate
        HashMap<K, V> after(Map<? extends K, ? extends V> map) {
            return new HashMap<>(map);
        }
    }

    @RecipeDescriptor(
            name = "Prefer `new LinkedHashMap<>()`",
            description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this."
    )
    public static class MapsNewLinkedHashMap<K, V> {
        @BeforeTemplate
        LinkedHashMap<K, V> before() {
            return Maps.newLinkedHashMap();
        }

        @AfterTemplate
        LinkedHashMap<K, V> after() {
            return new LinkedHashMap<>();
        }
    }

    @RecipeDescriptor(
            name = "Prefer `new LinkedHashMap<>(Map)`",
            description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this."
    )
    public static class MapsNewLinkedHashMapFromMap<K, V> {
        @BeforeTemplate
        LinkedHashMap<K, V> before(Map<? extends K, ? extends V> map) {
            return Maps.newLinkedHashMap(map);
        }

        @AfterTemplate
        LinkedHashMap<K, V> after(Map<? extends K, ? extends V> map) {
            return new LinkedHashMap<>(map);
        }
    }

    @RecipeDescriptor(
            name = "Prefer `new TreeMap<>()`",
            description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this."
    )
    public static class MapsNewTreeMap<K extends Comparable<?>, V> {
        @BeforeTemplate
        TreeMap<K, V> before() {
            return Maps.newTreeMap();
        }

        @AfterTemplate
        TreeMap<K, V> after() {
            return new TreeMap<>();
        }
    }

    @RecipeDescriptor(
            name = "Prefer `new TreeMap<>(Comparator)`",
            description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this."
    )
    public static class MapsNewTreeMapWithComparator<K, V> {
        @BeforeTemplate
        TreeMap<K, V> before(Comparator<? super K> comparator) {
            return Maps.newTreeMap(comparator);
        }

        @AfterTemplate
        TreeMap<K, V> after(Comparator<? super K> comparator) {
            return new TreeMap<>(comparator);
        }
    }

    @RecipeDescriptor(
            name = "Prefer `new TreeMap<>(SortedMap)`",
            description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this."
    )
    public static class MapsNewTreeMapFromSortedMap<K, V> {
        @BeforeTemplate
        TreeMap<K, V> before(SortedMap<K, ? extends V> map) {
            return Maps.newTreeMap(map);
        }

        @AfterTemplate
        TreeMap<K, V> after(SortedMap<K, ? extends V> map) {
            return new TreeMap<>(map);
        }
    }

    @RecipeDescriptor(
            name = "Prefer `Collections.newSetFromMap(new ConcurrentHashMap<>())`",
            description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this."
    )
    public static class SetsNewConcurrentHashSet<T> {
        @BeforeTemplate
        Set<T> before() {
            return Sets.newConcurrentHashSet();
        }

        @AfterTemplate
        Set<T> after() {
            return Collections.newSetFromMap(new ConcurrentHashMap<>());
        }
    }

    @RecipeDescriptor(
            name = "Prefer `new LinkedHashSet<>()`",
            description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this."
    )
    public static class SetsNewLinkedHashSet<T> {
        @BeforeTemplate
        LinkedHashSet<T> before() {
            return Sets.newLinkedHashSet();
        }

        @AfterTemplate
        LinkedHashSet<T> after() {
            return new LinkedHashSet<>();
        }
    }

    @RecipeDescriptor(
            name = "Prefer `new LinkedHashSet<>(Collection)`",
            description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this."
    )
    public static class SetsNewLinkedHashSetFromCollection<T> {
        @BeforeTemplate
        LinkedHashSet<T> before(Collection<T> collection) {
            return Sets.newLinkedHashSet(collection);
        }

        @AfterTemplate
        LinkedHashSet<T> after(Collection<T> collection) {
            return new LinkedHashSet<>(collection);
        }
    }

    @RecipeDescriptor(
            name = "Prefer `new LinkedHashSet<>(int)`",
            description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this."
    )
    public static class SetsNewLinkedHashSetWithExpectedSize<T> {
        @BeforeTemplate
        LinkedHashSet<T> before(int expectedSize) {
            return Sets.newLinkedHashSetWithExpectedSize(expectedSize);
        }

        @AfterTemplate
        LinkedHashSet<T> after(int expectedSize) {
            return new LinkedHashSet<>(expectedSize);
        }
    }

    @RecipeDescriptor(
            name = "Prefer `Collection.stream().allMatch(Predicate)`",
            description = "Prefer `Collection.stream().allMatch(Predicate)` over `Iterables.all(Collection, Predicate)`."
    )
    public static class IterablesAll<T> {
        @BeforeTemplate
        boolean before(Collection<T> collection, com.google.common.base.Predicate<? super T> predicate) {
            return Iterables.all(collection, predicate);
        }

        @AfterTemplate
        boolean after(Collection<T> collection, com.google.common.base.Predicate<? super T> predicate) {
            return collection.stream().allMatch(predicate);
        }
    }

    @RecipeDescriptor(
            name = "Prefer `Collection.stream().map(Function)` over `Collections2.transform`",
            description = "Prefer `Collection.stream().map(Function)` over `Collections2.transform(Collection, Function)`."
    )
    public static class Collections2Transform<F, T> {
        @BeforeTemplate
        Collection<T> before(Collection<F> collection, com.google.common.base.Function<? super F, T> function) {
            return Collections2.transform(collection, function);
        }

        @AfterTemplate
        Collection<T> after(Collection<F> collection, com.google.common.base.Function<? super F, T> function) {
            return collection.stream().map(function).collect(Collectors.toList());
        }
    }
}
