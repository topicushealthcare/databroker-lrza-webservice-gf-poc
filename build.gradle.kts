import org.gradle.kotlin.dsl.withType
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.springframework.boot") version "3.5.14"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.22.0" apply false
}

group = "nl.topicuszorg.databroker"
version = "0.0.1-SNAPSHOT"
description = "lrza-webservice"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

allprojects {
    repositories {
        mavenCentral()
    }
}

version = "1.0-SNAPSHOT"

subprojects {
    group = "nl.topicuszorg"

    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.gradle.idea") // Equivalent of `idea`
    apply(plugin = "jacoco")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
    }

    ext["jackson.version"] = "2.16.1" // Specifieke versie voor HAPI FHIR 7

    dependencyManagement {
        imports {
            mavenBom("ca.uhn.hapi.fhir:hapi-fhir-bom:8.8.1")
            mavenBom(SpringBootPlugin.BOM_COORDINATES)
        }
    }

    kotlin {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
