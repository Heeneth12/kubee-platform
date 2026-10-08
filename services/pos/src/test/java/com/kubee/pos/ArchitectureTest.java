package com.kubee.pos;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/** Keeps the DDD / CQRS layering honest as more modules are added. */
@AnalyzeClasses(packages = "com.kubee.pos", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_depends_on_nothing_outer = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..application..", "..infrastructure..", "..api..", "..common.web..", "..common.cqrs..",
                    "org.springframework.web..", "org.springframework.jdbc..");

    @ArchTest
    static final ArchRule application_does_not_know_delivery_or_infrastructure = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..infrastructure..", "..api..", "org.springframework.web..", "org.springframework.jdbc..");

    @ArchTest
    static final ArchRule infrastructure_is_not_used_directly = noClasses()
            .that().resideOutsideOfPackage("..infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule common_does_not_know_business_modules = noClasses()
            .that().resideInAPackage("com.kubee.pos.common..")
            .should().dependOnClassesThat().resideInAnyPackage("com.kubee.pos.catalog..", "com.kubee.pos.ordering..",
                    "com.kubee.pos.billing..", "com.kubee.pos.reporting..", "com.kubee.pos.shift..");

    @ArchTest
    static final ArchRule modules_are_independent = slices()
            .matching("com.kubee.pos.(*)..")
            .should().notDependOnEachOther()
            .ignoreDependency(DescribedPredicate.alwaysTrue(), resideInAPackage("com.kubee.pos.common.."));

    @ArchTest
    static final ArchRule controllers_only_talk_to_buses = noClasses()
            .that().resideInAPackage("..api..")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule handlers_live_in_application = classes()
            .that().haveSimpleNameEndingWith("Handler").and().resideOutsideOfPackage("com.kubee.pos.common..")
            .should().resideInAPackage("..application..");
}
