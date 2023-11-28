package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;
import static org.openrewrite.xml.Assertions.xml;

public class JpaCachePropertiesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new JpaCacheProperties());
    }

    @Test
    void jpaCacheProperties() {
        rewriteRun(
          //language=xml
          xml(
            """
               <?xml version="1.0" encoding="UTF-8"?>
               <persistence version="2.0" xmlns="http://java.sun.com/xml/ns/persistence" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd">
               
               	
               	<!-- the name set_set_set corresponds to values in the truth table defined
               	     in JpaCacheProperties.java -->
               	<!-- 1 -->
                   <persistence-unit name="set_set_set1"><!-- flag -->
               		<shared-cache-mode>NONE</shared-cache-mode><!-- leave -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="openjpa.DataCache" value="falSe"/><!-- remove -->
                           <property name="javax.persistence.sharedCache.mode" value="NONE"/><!-- remove -->
               		</properties>
               	</persistence-unit>
               	
                   <persistence-unit name="set_set_set2"><!-- flag -->
               		<shared-cache-mode>ALL</shared-cache-mode><!-- leave -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="openjpa.DataCache" value="tRue"/><!-- remove -->
                           <property name="javax.persistence.sharedCache.mode" value="ALL"/><!-- remove -->
               		</properties>
               	</persistence-unit>	
               	
               	<persistence-unit name="set_set_set3"><!-- flag -->
               		<shared-cache-mode>UNSPECIFIED</shared-cache-mode><!-- leave, change to ALL -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="openjpa.DataCache" value="TRUE"/><!-- remove -->
                           <property name="javax.persistence.sharedCache.mode" value="ALL"/><!-- remove -->
               		</properties>
               	</persistence-unit>	
               	
               	<persistence-unit name="set_set_set4"><!-- flag -->
               		<shared-cache-mode>DISABLE_SELECTIVE</shared-cache-mode><!-- leave -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="openjpa.DataCache" value="truE(ExcludedTypes=foo.bar.Person;foo.bar.Employee)"/>
                           <property name="javax.persistence.sharedCache.mode" value="DISABLE_SELECTIVE"/><!-- remove -->
               		</properties>
               	</persistence-unit>	
               	
               	<!-- 2 -->
               	<persistence-unit name="set_set_notset1"><!-- flag -->
               		<shared-cache-mode>NONE</shared-cache-mode><!-- leave -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="javax.persistence.sharedCache.mode" value="NONE"/><!-- remove -->
               		</properties>
               	</persistence-unit>
               	
                   <persistence-unit name="set_set_notset2"><!-- flag -->
               		<shared-cache-mode>ALL</shared-cache-mode><!-- leave -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="javax.persistence.sharedCache.mode" value="ALL"/><!-- remove -->
               		</properties>
               	</persistence-unit>	
               	
               	<persistence-unit name="set_set_notset3"><!-- flag -->
               		<shared-cache-mode>UNSPECIFIED</shared-cache-mode><!-- change to NONE -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="javax.persistence.sharedCache.mode" value="ALL"/><!-- remove -->
               		</properties>
               	</persistence-unit>	
               	
               	<persistence-unit name="set_set_notset4"><!-- flag -->
               		<shared-cache-mode>ALL</shared-cache-mode><!-- leave -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="javax.persistence.sharedCache.mode" value="UNSPECIFIED"/><!-- remove -->
               		</properties>
               	</persistence-unit>	
               	
               	<persistence-unit name="set_set_notset5"><!-- flag -->
               		<shared-cache-mode>DISABLE_SELECTIVE</shared-cache-mode><!-- leave -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
                           <property name="javax.persistence.sharedCache.mode" value="DISABLE_SELECTIVE"/><!-- remove -->
               		</properties>
               	</persistence-unit>	
               	
               	<!-- 3 -->
               	<persistence-unit name="set_notset_set1"><!-- flag -->
               		<shared-cache-mode>NONE</shared-cache-mode><!-- leave -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
               			<property name="openjpa.DataCache" value="true(ExcludedTypes=foo.bar.Person;foo.bar.Employee)"/>
               		</properties>
               	</persistence-unit>
               	
                   <persistence-unit name="set_notset_set2"><!-- flag -->
               		<shared-cache-mode>ALL</shared-cache-mode><!-- leave -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
               			<property name="openjpa.DataCache" value="True"/><!-- remove -->
               		</properties>
               	</persistence-unit>	
               	
               	<persistence-unit name="set_notset_set3"><!-- flag -->
               		<shared-cache-mode>UNSPECIFIED</shared-cache-mode><!-- change to ALL -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
               			<property name="openjpa.DataCache" value="TRUE"/><!-- remove -->
               		</properties>
               	</persistence-unit>	
               	
               	<persistence-unit name="set_notset_set4"><!-- flag -->
               		<shared-cache-mode>DISABLE_SELECTIVE</shared-cache-mode><!-- leave -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<property name="openjpa.DataCache" value="true"/><!-- remove -->		
               		</properties>
               	</persistence-unit>	
               	
               	<!-- 4 -->
               	<persistence-unit name="set_notset_notset1"><!-- don't flag -->
               		<shared-cache-mode>NONE</shared-cache-mode>
               		<validation-mode>NONE</validation-mode>	
               	</persistence-unit>
               	
                   <persistence-unit name="set_notset_notset2"><!-- don't flag -->
               		<shared-cache-mode>ALL</shared-cache-mode>
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
               		</properties>
               	</persistence-unit>	
               	
               	<persistence-unit name="set_notset_notset3"><!-- flag -->
               		<shared-cache-mode>UNSPECIFIED</shared-cache-mode><!-- change to NONE -->
               		<validation-mode>NONE</validation-mode>	
               	</persistence-unit>	
               	
               	<persistence-unit name="set_notset_notset4"><!-- don't flag -->
               		<shared-cache-mode>DISABLE_SELECTIVE</shared-cache-mode>
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               		</properties>
               	</persistence-unit>		
               	
               	<!-- 5 -->
                   <persistence-unit name="notset_set_set1"><!-- flag -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="openjpa.DataCache" value="false"/><!-- remove -->
                           <property name="javax.persistence.sharedCache.mode" value="NONE"/><!-- leave -->
               		</properties>
               	</persistence-unit>
               	
                   <persistence-unit name="notset_set_set2"><!-- flag -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="openjpa.DataCache" value="true"/><!-- remove -->
                           <property name="javax.persistence.sharedCache.mode" value="ALL"/><!-- leave -->
               		</properties>
               	</persistence-unit>	
               	
               	<persistence-unit name="notset_set_set3"><!-- flag -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="openjpa.DataCache" value="true"/><!-- remove -->
                           <property name="javax.persistence.sharedCache.mode" value="UNSPECIFIED"/><!-- change to ALL -->
               		</properties>
               	</persistence-unit>	
               	
               	<persistence-unit name="notset_set_set4"><!-- flag -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="openjpa.DataCache" value="true(ExcludedTypes=foo.bar.Person;foo.bar.Employee)"/><!-- leave -->
                           <property name="javax.persistence.sharedCache.mode" value="DISABLE_SELECTIVE"/><!-- leave -->
               		</properties>
               	</persistence-unit>	
               	
               	<!-- 6 -->
                   <persistence-unit name="notset_set_notset1"><!-- don't flag -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="javax.persistence.sharedCache.mode" value="NONE"/><!-- leave -->
               		</properties>
               	</persistence-unit>
               	
                   <persistence-unit name="notset_set_notset2"><!-- don't flag -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="javax.persistence.sharedCache.mode" value="ALL"/><!-- leave -->
               		</properties>
               	</persistence-unit>	
               	
               	<persistence-unit name="notset_set_notset3"><!-- flag -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="javax.persistence.sharedCache.mode" value="UNSPECIFIED"/><!-- change to NONE -->
               		</properties>
               	</persistence-unit>	
               	
               	<persistence-unit name="notset_set_notset4"><!-- don't flag -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="javax.persistence.sharedCache.mode" value="DISABLE_SELECTIVE"/><!-- leave -->
               		</properties>
               	</persistence-unit>
               	
               	<!-- 7 -->
                   <persistence-unit name="notset_notset_set1"><!-- flag -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="openjpa.DataCache" value="false"/><!-- remove and insert shared-cache-mode NONE -->
               		</properties>
               	</persistence-unit>
               	
                   <persistence-unit name="notset_notset_set2"><!-- flag -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="openjpa.DataCache" value="true"/><!-- remove and insert shared-cache-mode ALL -->
               		</properties>
               	</persistence-unit>	
               	
               	<persistence-unit name="notset_notset_set3"><!-- flag --> <!-- add shared-cache-mode ENABLE_SELECTIVE -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="openjpa.DataCache" value="truE(Types=foo.bar.Person;foo.bar.Employee)"/><!-- leave - manual fix-->
               		</properties>
               	</persistence-unit>	
               	
               	<persistence-unit name="notset_notset_set4"><!-- flag --> <!-- add shared-cache-mode DISABLE_SELECTIVE -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="openjpa.DataCache" value="TRUE(ExcludedTypes=foo.bar.Person;foo.bar.Employee)"/><!-- leave - manual fix -->
               		</properties>
               	</persistence-unit>	
               	
               	<!-- 8 -->
               	<persistence-unit name="notset_notset_notset1"><!-- flag, insert shared-cache-mode NONE -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
               			<property name="somethingelese" value="junk"></property>
                  		</properties>
               	</persistence-unit>
               	
               	<!-- flag, insert shared-cache-mode NONE -->
                   <persistence-unit name="notset_notset_notset2">
               	</persistence-unit>	
               	
               	<!-- flag, insert shared-cache-mode NONE -->
               	<persistence-unit name="notset_notset_notset3"></persistence-unit>	
               	
               	<persistence-unit name="openjpa_cache1_flagged"><!-- flag -->
               		<shared-cache-mode>UNSPECIFIED</shared-cache-mode><!-- set to NONE -->
               		<validation-mode>NONE</validation-mode>	
               		<properties>
               			<!-- Connection properties -->
                           <property name="openjpa.DataCache" value="false"/><!-- remove -->
               		</properties>
               	</persistence-unit>
                 \s
                   <persistence-unit name="openjpa_cache1_spaces_flagged"><!-- flag -->
                       <shared-cache-mode>
                               UNSPECIFIED
                       </shared-cache-mode><!-- set to NONE -->
                       <validation-mode>NONE</validation-mode>\s
                       <properties>
                           <property name="openjpa.DataCache" value="false"/><!-- remove -->
                       </properties>
                   </persistence-unit>
                 \s
                   <persistence-unit name="openjpa_cache2_flagged"><!-- flag -->
                       <shared-cache-mode>UNSPECIFIED</shared-cache-mode><!-- set to ALL -->
                       <validation-mode>NONE</validation-mode>\s
                       <properties>
                           <property name="openjpa.DataCache" value="truE"/><!-- remove -->
                       </properties>
                   </persistence-unit>
                  \s
                   <persistence-unit name="openjpa_cache3_flagged"><!-- flag --><!-- create shared-cache-mode NONE -->
                       <validation-mode>NONE</validation-mode>\s
                       <properties>
                           <property name="javax.persistence.jdbc.driver" value="com.ibm.db2.jcc.DB2Driver" />
                       </properties>
                   </persistence-unit>
                  \s
                   <persistence-unit name="openjpa_cache4_flagged"> <!-- flag -->
                       <shared-cache-mode>DISABLE_SELECTIVE</shared-cache-mode>
                       <validation-mode>NONE</validation-mode>\s
                       <properties>
                           <property name="openjpa.DataCache" value="tRue"/><!-- remove -->
                       </properties>
                   </persistence-unit>
                  \s
                   <persistence-unit name="openjpa_cache5_flagged"> <!-- flag -->
                       <shared-cache-mode>DISABLE_SELECTIVE</shared-cache-mode>
                       <validation-mode>NONE</validation-mode>\s
                       <properties>
                           <property name="openjpa.DataCache" value="false"/><!-- remove -->
                       </properties>
                   </persistence-unit>
                  \s
                   <persistence-unit name="openjpa_cache6_flagged"><!-- flag -->
                       <shared-cache-mode>UNSPECIFIED</shared-cache-mode><!-- change to NONE -->
                       <validation-mode>NONE</validation-mode>\s
                       <properties>
                          \s
                       </properties>
                   </persistence-unit>
                 \s
                  <!-- don't flag -->
                   <persistence-unit name="openjpa_cache7_notflagged">
                       <shared-cache-mode>NONE</shared-cache-mode>
                       <validation-mode>NONE</validation-mode>\s
                       <properties>
                       </properties>
                   </persistence-unit>
                  \s
                   <!-- don't flag -->
                   <persistence-unit name="openjpa_cache8_notflagged">
                       <shared-cache-mode>ALL</shared-cache-mode>
                       <validation-mode>NONE</validation-mode>\s
                       <properties>
                       </properties>
                   </persistence-unit>
                 \s
                   <persistence-unit name="openjpa_cache9_flagged">  <!-- flag -->
                       <shared-cache-mode>NONE</shared-cache-mode>
                       <validation-mode>NONE</validation-mode>\s
                       <properties>
                           <property name="openjpa.DataCache" value="True"/> <!-- delete -->
                       </properties>
                   </persistence-unit>
                  \s
                   <persistence-unit name="openjpa_cache10_flagged"> <!-- flag and insert shared-cache-mode NONE -->
                       <validation-mode>NONE</validation-mode>\s
                       <properties>
                           <property name="openjpa.DataCache" value="FALSE"/><!-- delete -->
                       </properties>
                   </persistence-unit>
                  \s
                   <persistence-unit name="openjpa_cache11_flagged"><!-- flag and insert shared-cache-mode ALL-->
                       <properties>
                           <property name="openjpa.DataCache" value="tRue"/><!-- delete -->
                       </properties>
                   </persistence-unit>
               </persistence>
               
               """,
            """
               <?xml version="1.0" encoding="UTF-8"?>
               <persistence version="2.0" xmlns="http://java.sun.com/xml/ns/persistence" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd">
               
               
               	<!-- the name set_set_set corresponds to values in the truth table defined
               	     in JpaCacheProperties.java -->
               	<!-- 1 -->
               	<persistence-unit name="set_set_set1">
               		<!-- flag -->
               		<shared-cache-mode>NONE</shared-cache-mode>
               		<!-- leave -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<!-- remove -->
               			<!-- remove -->
               		</properties>
               	</persistence-unit>
               
               	<persistence-unit name="set_set_set2">
               		<!-- flag -->
               		<shared-cache-mode>ALL</shared-cache-mode>
               		<!-- leave -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<!-- remove -->
               			<!-- remove -->
               		</properties>
               	</persistence-unit>
               
               	<persistence-unit name="set_set_set3">
               		<!-- flag -->
               		<shared-cache-mode>ALL</shared-cache-mode>
               		<!-- leave, change to ALL -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<!-- remove -->
               			<!-- remove -->
               		</properties>
               	</persistence-unit>
               
               	<persistence-unit name="set_set_set4">
               		<!-- flag -->
               		<shared-cache-mode>DISABLE_SELECTIVE</shared-cache-mode>
               		<!-- leave -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<property name="openjpa.DataCache" value="truE(ExcludedTypes=foo.bar.Person;foo.bar.Employee)"/>
               			<!-- remove -->
               		</properties>
               	</persistence-unit>
               
               	<!-- 2 -->
               	<persistence-unit name="set_set_notset1">
               		<!-- flag -->
               		<shared-cache-mode>NONE</shared-cache-mode>
               		<!-- leave -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<!-- remove -->
               		</properties>
               	</persistence-unit>
               
               	<persistence-unit name="set_set_notset2">
               		<!-- flag -->
               		<shared-cache-mode>ALL</shared-cache-mode>
               		<!-- leave -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<!-- remove -->
               		</properties>
               	</persistence-unit>
               
               	<persistence-unit name="set_set_notset3">
               		<!-- flag -->
               		<shared-cache-mode>NONE</shared-cache-mode>
               		<!-- change to NONE -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<!-- remove -->
               		</properties>
               	</persistence-unit>
               
               	<persistence-unit name="set_set_notset4">
               		<!-- flag -->
               		<shared-cache-mode>ALL</shared-cache-mode>
               		<!-- leave -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<!-- remove -->
               		</properties>
               	</persistence-unit>
               
               	<persistence-unit name="set_set_notset5">
               		<!-- flag -->
               		<shared-cache-mode>DISABLE_SELECTIVE</shared-cache-mode>
               		<!-- leave -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- remove -->
               		</properties>
               	</persistence-unit>
               
               	<!-- 3 -->
               	<persistence-unit name="set_notset_set1"><!-- flag -->
               		<shared-cache-mode>NONE</shared-cache-mode><!-- leave -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<property name="openjpa.DataCache" value="true(ExcludedTypes=foo.bar.Person;foo.bar.Employee)"/>
               		</properties>
               	</persistence-unit>
               
               	<persistence-unit name="set_notset_set2">
               		<!-- flag -->
               		<shared-cache-mode>ALL</shared-cache-mode>
               		<!-- leave -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<!-- remove -->
               		</properties>
               	</persistence-unit>
               
               	<persistence-unit name="set_notset_set3">
               		<!-- flag -->
               		<shared-cache-mode>ALL</shared-cache-mode>
               		<!-- change to ALL -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<!-- remove -->
               		</properties>
               	</persistence-unit>
               
               	<persistence-unit name="set_notset_set4">
               		<!-- flag -->
               		<shared-cache-mode>DISABLE_SELECTIVE</shared-cache-mode>
               		<!-- leave -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- remove -->
               		</properties>
               	</persistence-unit>
               
               	<!-- 4 -->
               	<persistence-unit name="set_notset_notset1"><!-- don't flag -->
               		<shared-cache-mode>NONE</shared-cache-mode>
               		<validation-mode>NONE</validation-mode>
               	</persistence-unit>
               
                   <persistence-unit name="set_notset_notset2"><!-- don't flag -->
               		<shared-cache-mode>ALL</shared-cache-mode>
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               		</properties>
               	</persistence-unit>
               
               	<persistence-unit name="set_notset_notset3">
               		<!-- flag -->
               		<shared-cache-mode>NONE</shared-cache-mode>
               		<!-- change to NONE -->
               		<validation-mode>NONE</validation-mode>
               	</persistence-unit>
               
               	<persistence-unit name="set_notset_notset4"><!-- don't flag -->
               		<shared-cache-mode>DISABLE_SELECTIVE</shared-cache-mode>
               		<validation-mode>NONE</validation-mode>
               		<properties>
               		</properties>
               	</persistence-unit>
               
               	<!-- 5 -->
               	<persistence-unit name="notset_set_set1">
               		<!-- flag -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<!-- remove -->
               			<property name="javax.persistence.sharedCache.mode" value="NONE"/>
               			<!-- leave -->
               		</properties>
               	</persistence-unit>
               
               	<persistence-unit name="notset_set_set2">
               		<!-- flag -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<!-- remove -->
               			<property name="javax.persistence.sharedCache.mode" value="ALL"/>
               			<!-- leave -->
               		</properties>
               	</persistence-unit>
               
               	<persistence-unit name="notset_set_set3">
               		<!-- flag -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<!-- remove -->
               			<property name="javax.persistence.sharedCache.mode" value="ALL"/>
               			<!-- change to ALL -->
               		</properties>
               	</persistence-unit>
               
               	<persistence-unit name="notset_set_set4"><!-- flag -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
                           <property name="openjpa.DataCache" value="true(ExcludedTypes=foo.bar.Person;foo.bar.Employee)"/><!-- leave -->
                           <property name="javax.persistence.sharedCache.mode" value="DISABLE_SELECTIVE"/><!-- leave -->
               		</properties>
               	</persistence-unit>
               
               	<!-- 6 -->
                   <persistence-unit name="notset_set_notset1"><!-- don't flag -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
                           <property name="javax.persistence.sharedCache.mode" value="NONE"/><!-- leave -->
               		</properties>
               	</persistence-unit>
               
                   <persistence-unit name="notset_set_notset2"><!-- don't flag -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
                           <property name="javax.persistence.sharedCache.mode" value="ALL"/><!-- leave -->
               		</properties>
               	</persistence-unit>
               
               	<persistence-unit name="notset_set_notset3">
               		<!-- flag -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<property name="javax.persistence.sharedCache.mode" value="NONE"/>
               			<!-- change to NONE -->
               		</properties>
               	</persistence-unit>
               
               	<persistence-unit name="notset_set_notset4"><!-- don't flag -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
                           <property name="javax.persistence.sharedCache.mode" value="DISABLE_SELECTIVE"/><!-- leave -->
               		</properties>
               	</persistence-unit>
               
               	<!-- 7 -->
               	<persistence-unit name="notset_notset_set1">
               		<!-- flag -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<!-- remove and insert shared-cache-mode NONE -->
               		</properties>
               		<shared-cache-mode>NONE</shared-cache-mode>
               	</persistence-unit>
               
               	<persistence-unit name="notset_notset_set2">
               		<!-- flag -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<!-- remove and insert shared-cache-mode ALL -->
               		</properties>
               		<shared-cache-mode>ALL</shared-cache-mode>
               	</persistence-unit>
               
               	<persistence-unit name="notset_notset_set3">
               		<!-- flag -->
               		<!-- add shared-cache-mode ENABLE_SELECTIVE -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<property name="openjpa.DataCache" value="truE(Types=foo.bar.Person;foo.bar.Employee)"/>
               			<!-- leave - manual fix-->
               		</properties>
               		<shared-cache-mode>ENABLE_SELECTIVE</shared-cache-mode>
               	</persistence-unit>
               
               	<persistence-unit name="notset_notset_set4">
               		<!-- flag -->
               		<!-- add shared-cache-mode DISABLE_SELECTIVE -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<property name="openjpa.DataCache" value="TRUE(ExcludedTypes=foo.bar.Person;foo.bar.Employee)"/>
               			<!-- leave - manual fix -->
               		</properties>
               		<shared-cache-mode>DISABLE_SELECTIVE</shared-cache-mode>
               	</persistence-unit>
               
               	<!-- 8 -->
               	<persistence-unit name="notset_notset_notset1">
               		<!-- flag, insert shared-cache-mode NONE -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<property name="somethingelese" value="junk"></property>
               		</properties>
               		<shared-cache-mode>NONE</shared-cache-mode>
               	</persistence-unit>
               
               	<!-- flag, insert shared-cache-mode NONE -->
               	<persistence-unit name="notset_notset_notset2">
               		<shared-cache-mode>NONE</shared-cache-mode>
               	</persistence-unit>
               
               	<!-- flag, insert shared-cache-mode NONE -->
               	<persistence-unit name="notset_notset_notset3">
               		<shared-cache-mode>NONE</shared-cache-mode>
               </persistence-unit>
               
               	<persistence-unit name="openjpa_cache1_flagged">
               		<!-- flag -->
               		<shared-cache-mode>NONE</shared-cache-mode>
               		<!-- set to NONE -->
               		<validation-mode>NONE</validation-mode>
               		<properties>
               			<!-- Connection properties -->
               			<!-- remove -->
               		</properties>
               	</persistence-unit>
               	
               	<persistence-unit name="openjpa_cache1_spaces_flagged">
               		<!-- flag -->
               		<shared-cache-mode>
               			NONE
                       </shared-cache-mode>
               		<!-- set to NONE -->
               		<validation-mode>NONE</validation-mode>\s
               		<properties>
               			<!-- remove -->
               		</properties>
               	</persistence-unit>
               	
               	<persistence-unit name="openjpa_cache2_flagged">
               		<!-- flag -->
               		<shared-cache-mode>ALL</shared-cache-mode>
               		<!-- set to ALL -->
               		<validation-mode>NONE</validation-mode>\s
               		<properties>
               			<!-- remove -->
               		</properties>
               	</persistence-unit>
               	<persistence-unit name="openjpa_cache3_flagged">
               		<!-- flag -->
               		<!-- create shared-cache-mode NONE -->
               		<validation-mode>NONE</validation-mode>\s
               		<properties>
               			<property name="javax.persistence.jdbc.driver" value="com.ibm.db2.jcc.DB2Driver" />
               		</properties>
               		<shared-cache-mode>NONE</shared-cache-mode>
                   </persistence-unit>
               	<persistence-unit name="openjpa_cache4_flagged">
               		<!-- flag -->
               		<shared-cache-mode>DISABLE_SELECTIVE</shared-cache-mode>
               		<validation-mode>NONE</validation-mode>\s
               		<properties>
               			<!-- remove -->
               		</properties>
               	</persistence-unit>
               	<persistence-unit name="openjpa_cache5_flagged">
               		<!-- flag -->
               		<shared-cache-mode>DISABLE_SELECTIVE</shared-cache-mode>
               		<validation-mode>NONE</validation-mode>\s
               		<properties>
               			<!-- remove -->
               		</properties>
               	</persistence-unit>
               	<persistence-unit name="openjpa_cache6_flagged">
               		<!-- flag -->
               		<shared-cache-mode>NONE</shared-cache-mode>
               		<!-- change to NONE -->
               		<validation-mode>NONE</validation-mode>\s
               		<properties>
               				
               		</properties>
               	</persistence-unit>
                 \s
                  <!-- don't flag -->
                   <persistence-unit name="openjpa_cache7_notflagged">
                       <shared-cache-mode>NONE</shared-cache-mode>
                       <validation-mode>NONE</validation-mode>\s
                       <properties>
                       </properties>
                   </persistence-unit>
                  \s
                   <!-- don't flag -->
                   <persistence-unit name="openjpa_cache8_notflagged">
                       <shared-cache-mode>ALL</shared-cache-mode>
                       <validation-mode>NONE</validation-mode>\s
                       <properties>
                       </properties>
                   </persistence-unit>
               	
               	<persistence-unit name="openjpa_cache9_flagged">
               		<!-- flag -->
               		<shared-cache-mode>NONE</shared-cache-mode>
               		<validation-mode>NONE</validation-mode>\s
               		<properties>
               			<!-- delete -->
               		</properties>
               	</persistence-unit>
               	<persistence-unit name="openjpa_cache10_flagged">
               		<!-- flag and insert shared-cache-mode NONE -->
               		<validation-mode>NONE</validation-mode>\s
               		<properties>
               			<!-- delete -->
               		</properties>
               		<shared-cache-mode>NONE</shared-cache-mode>
               	</persistence-unit>
               	<persistence-unit name="openjpa_cache11_flagged">
               		<!-- flag and insert shared-cache-mode ALL-->
               		<properties>
               			<!-- delete -->
               		</properties>
               		<shared-cache-mode>ALL</shared-cache-mode>
               	</persistence-unit>
               </persistence>
               
               """,
            sourceSpecs -> sourceSpecs.path("persistence.xml")
          )
        );
    }
}
