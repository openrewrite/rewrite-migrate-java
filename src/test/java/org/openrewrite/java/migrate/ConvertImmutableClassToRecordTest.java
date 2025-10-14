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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ConvertImmutableClassToRecordTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ConvertImmutableClassToRecord(null));
    }

    @DocumentExample
    @Test
    void simpleImmutableClass() {
        rewriteRun(
          //language=java
          java(
            """
              class Person {
                  private final String name;
                  private final int age;

                  public Person(String name, int age) {
                      this.name = name;
                      this.age = age;
                  }

                  public String getName() {
                      return name;
                  }

                  public int getAge() {
                      return age;
                  }
              }
              """,
            """
              record Person(String name, int age) {
              }
              """
          )
        );
    }

    @Test
    void completeClassToRecordTransformationWithMethodCallUpdates() {
        rewriteRun(
          //language=java
          java(
            """
              class Person {
                  private final String name;
                  private final int age;

                  public Person(String name, int age) {
                      this.name = name;
                      this.age = age;
                  }

                  public String getName() {
                      return name;
                  }

                  public int getAge() {
                      return age;
                  }

                  public String getDisplayName() {
                      return name + " (" + age + ")";
                  }
              }
              """,
            """
              record Person(String name, int age) {
                  public String getDisplayName() {
                      return name + " (" + age + ")";
                  }
              }
              """
          ),
          //language=java
          java(
            """
              class PersonService {
                  public void processPerson(Person person) {
                      String name = person.getName();
                      int age = person.getAge();
                      String display = person.getDisplayName();
                      System.out.println("Processing: " + display);
                  }
              }
              """,
            """
              class PersonService {
                  public void processPerson(Person person) {
                      String name = person.name();
                      int age = person.age();
                      String display = person.getDisplayName();
                      System.out.println("Processing: " + display);
                  }
              }
              """
          )
        );
    }

    @Test
    void packagePatternRestriction() {
        rewriteRun(
          spec -> spec.recipe(new ConvertImmutableClassToRecord("com.example.model.**")),
          //language=java
          java(
            """
              package com.example.model;
              
              class Person {
                  private final String name;
              
                  public Person(String name) {
                      this.name = name;
                  }
              
                  public String getName() {
                      return name;
                  }
              }
              """,
            """
              package com.example.model;
              
              record Person(String name) {
              }
              """
          ),
          //language=java
          java(
            """
              package com.example.service;
              
              class User {
                  private final String username;
              
                  public User(String username) {
                      this.username = username;
                  }
              
                  public String getUsername() {
                      return username;
                  }
              }
              """
              // Should not be transformed due to package pattern restriction
          )
        );
    }

    @Test
    void multipleClassesTransformation() {
        rewriteRun(
          //language=java
          java(
            """
              class Address {
                  private final String street;
                  private final String city;

                  public Address(String street, String city) {
                      this.street = street;
                      this.city = city;
                  }

                  public String getStreet() {
                      return street;
                  }

                  public String getCity() {
                      return city;
                  }
              }

              class Person {
                  private final String name;
                  private final Address address;

                  public Person(String name, Address address) {
                      this.name = name;
                      this.address = address;
                  }

                  public String getName() {
                      return name;
                  }

                  public Address getAddress() {
                      return address;
                  }
              }
              """,
            """
              record Address(String street, String city) {
              }

              record Person(String name, Address address) {
              }
              """
          ),
          //language=java
          java(
            """
              class AddressService {
                  public void printFullAddress(Person person) {
                      Address addr = person.getAddress();
                      String street = addr.getStreet();
                      String city = addr.getCity();
                      System.out.println(street + ", " + city);
                  }
              }
              """,
            """
              class AddressService {
                  public void printFullAddress(Person person) {
                      Address addr = person.address();
                      String street = addr.street();
                      String city = addr.city();
                      System.out.println(street + ", " + city);
                  }
              }
              """
          )
        );
    }

    @Test
    void complexBusinessLogicPreservation() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.Objects;

              class Product {
                  private final String id;
                  private final String name;
                  private final double price;

                  public Product(String id, String name, double price) {
                      this.id = id;
                      this.name = name;
                      this.price = price;
                  }

                  public String getId() {
                      return id;
                  }

                  public String getName() {
                      return name;
                  }

                  public double getPrice() {
                      return price;
                  }

                  public String getFormattedPrice() {
                      return String.format("$%.2f", price);
                  }

                  public boolean isExpensive() {
                      return price > 100.0;
                  }

                  public Product withDiscount(double discountPercent) {
                      double newPrice = price * (1 - discountPercent / 100);
                      return new Product(id, name, newPrice);
                  }
              }
              """,
            """
              import java.util.Objects;

              record Product(String id, String name, double price) {
                  public String getFormattedPrice() {
                      return String.format("$%.2f", price);
                  }

                  public boolean isExpensive() {
                      return price > 100.0;
                  }

                  public Product withDiscount(double discountPercent) {
                      double newPrice = price * (1 - discountPercent / 100);
                      return new Product(id, name, newPrice);
                  }
              }
              """
          )
        );
    }

    @Test
    void nestedClassesHandling() {
        rewriteRun(
          //language=java
          java(
            """
              class OuterClass {
                  private String value;

                  static class InnerData {
                      private final String data;
                      private final int count;

                      public InnerData(String data, int count) {
                          this.data = data;
                          this.count = count;
                      }

                      public String getData() {
                          return data;
                      }

                      public int getCount() {
                          return count;
                      }
                  }

                  public void process(InnerData inner) {
                      String data = inner.getData();
                      int count = inner.getCount();
                      System.out.println(data + ": " + count);
                  }
              }
              """,
            """
              class OuterClass {
                  private String value;

                  static record InnerData(String data, int count) {
                  }

                  public void process(InnerData inner) {
                      String data = inner.data();
                      int count = inner.count();
                      System.out.println(data + ": " + count);
                  }
              }
              """
          )
        );
    }

    @Test
    void genericTypesSupport() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import java.util.Optional;

              class Container<T> {
                  private final T value;
                  private final List<T> items;

                  public Container(T value, List<T> items) {
                      this.value = value;
                      this.items = items;
                  }

                  public T getValue() {
                      return value;
                  }

                  public List<T> getItems() {
                      return items;
                  }

                  public Optional<T> findFirst() {
                      return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
                  }
              }
              """,
            """
              import java.util.List;
              import java.util.Optional;

              record Container<T>(T value, List<T> items) {
                  public Optional<T> findFirst() {
                      return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
                  }
              }
              """
          )
        );
    }

    @Test
    void integrationWithStreamOperations() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import java.util.stream.Collectors;

              class Employee {
                  private final String name;
                  private final String department;
                  private final double salary;

                  public Employee(String name, String department, double salary) {
                      this.name = name;
                      this.department = department;
                      this.salary = salary;
                  }

                  public String getName() {
                      return name;
                  }

                  public String getDepartment() {
                      return department;
                  }

                  public double getSalary() {
                      return salary;
                  }
              }

              class EmployeeService {
                  public List<String> getNamesByDepartment(List<Employee> employees, String dept) {
                      return employees.stream()
                              .filter(emp -> emp.getDepartment().equals(dept))
                              .map(Employee::getName)
                              .collect(Collectors.toList());
                  }

                  public double getTotalSalary(List<Employee> employees) {
                      return employees.stream()
                              .mapToDouble(Employee::getSalary)
                              .sum();
                  }
              }
              """,
            """
              import java.util.List;
              import java.util.stream.Collectors;

              record Employee(String name, String department, double salary) {
              }

              class EmployeeService {
                  public List<String> getNamesByDepartment(List<Employee> employees, String dept) {
                      return employees.stream()
                              .filter(emp -> emp.department().equals(dept))
                              .map(Employee::name)
                              .collect(Collectors.toList());
                  }

                  public double getTotalSalary(List<Employee> employees) {
                      return employees.stream()
                              .mapToDouble(Employee::salary)
                              .sum();
                  }
              }
              """
          )
        );
    }

    @Test
    void immutableClassWithAnnotations() {
        rewriteRun(
          //language=java
          java(
            """
              import javax.validation.constraints.NotNull;
              import javax.validation.constraints.Min;

              @Entity
              public class Person {
                  @NotNull
                  private final String name;

                  @Min(0)
                  private final int age;

                  public Person(String name, int age) {
                      this.name = name;
                      this.age = age;
                  }

                  public String getName() {
                      return name;
                  }

                  public int getAge() {
                      return age;
                  }
              }
              """,
            """
              import javax.validation.constraints.NotNull;
              import javax.validation.constraints.Min;

              @Entity
              public record Person(@NotNull String name, @Min(0) int age) {
              }
              """
          )
        );
    }

    @Test
    void booleanGetterWithIsPrefix() {
        rewriteRun(
          //language=java
          java(
            """
              class User {
                  private final String name;
                  private final boolean active;

                  public User(String name, boolean active) {
                      this.name = name;
                      this.active = active;
                  }

                  public String getName() {
                      return name;
                  }

                  public boolean isActive() {
                      return active;
                  }
              }
              """,
            """
              record User(String name, boolean active) {
              }
              """
          )
        );
    }

    @Test
    void classWithCollectionFields() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import java.util.Set;

              class Student {
                  private final String name;
                  private final List<String> courses;
                  private final Set<String> skills;

                  public Student(String name, List<String> courses, Set<String> skills) {
                      this.name = name;
                      this.courses = courses;
                      this.skills = skills;
                  }

                  public String getName() {
                      return name;
                  }

                  public List<String> getCourses() {
                      return courses;
                  }

                  public Set<String> getSkills() {
                      return skills;
                  }
              }
              """,
            """
              import java.util.List;
              import java.util.Set;

              record Student(String name, List<String> courses, Set<String> skills) {
              }
              """
          )
        );
    }

    @Test
    void classWithInterfaceImplementation() {
        rewriteRun(
          //language=java
          java(
            """
              interface Identifiable {
                  String getId();
              }

              class Entity implements Identifiable {
                  private final String id;
                  private final String name;

                  public Entity(String id, String name) {
                      this.id = id;
                      this.name = name;
                  }

                  public String getId() {
                      return id;
                  }

                  public String getName() {
                      return name;
                  }
              }
              """,
            """
              interface Identifiable {
                  String getId();
              }

              record Entity(String id, String name) implements Identifiable {
              }
              """
          )
        );
    }

    @Test
    void shouldNotConvertClassWithSetterMethods() {
        rewriteRun(
          //language=java
          java(
            """
              class MutablePerson {
                  private String name;
                  private int age;

                  public String getName() {
                      return name;
                  }

                  public void setName(String name) {
                      this.name = name;
                  }

                  public int getAge() {
                      return age;
                  }

                  public void setAge(int age) {
                      this.age = age;
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldNotConvertClassThatExtendsAnotherClass() {
        rewriteRun(
          //language=java
          java(
            """
              class BaseEntity {
                  private final String id;

                  public BaseEntity(String id) {
                      this.id = id;
                  }

                  public String getId() {
                      return id;
                  }
              }

              class Person extends BaseEntity {
                  private final String name;

                  public Person(String id, String name) {
                      super(id);
                      this.name = name;
                  }

                  public String getName() {
                      return name;
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldNotConvertClassWithoutGetters() {
        rewriteRun(
          //language=java
          java(
            """
              class DataHolder {
                  private final String data;

                  public DataHolder(String data) {
                      this.data = data;
                  }

                  public void processData() {
                      System.out.println(data);
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldNotConvertClassWithMissingGetters() {
        rewriteRun(
          //language=java
          java(
            """
              class PartialPerson {
                  private final String name;
                  private final int age;

                  public PartialPerson(String name, int age) {
                      this.name = name;
                      this.age = age;
                  }

                  public String getName() {
                      return name;
                  }

                  // Missing getAge() method
              }
              """
          )
        );
    }

    @Test
    void shouldNotConvertEnum() {
        rewriteRun(
          //language=java
          java(
            """
              enum Status {
                  ACTIVE,
                  INACTIVE;

                  public boolean isActive() {
                      return this == ACTIVE;
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldNotConvertInterface() {
        rewriteRun(
          //language=java
          java(
            """
              interface Repository {
                  String getName();
                  int getVersion();
              }
              """
          )
        );
    }

    @Test
    void shouldNotConvertAbstractClass() {
        rewriteRun(
          //language=java
          java(
            """
              abstract class AbstractEntity {
                  private final String id;

                  public AbstractEntity(String id) {
                      this.id = id;
                  }

                  public String getId() {
                      return id;
                  }

                  public abstract void process();
              }
              """
          )
        );
    }
}
