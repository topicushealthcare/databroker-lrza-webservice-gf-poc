import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.openapi.generator")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-base")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-client")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-structures-r4")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-client-okhttp")
    implementation("jakarta.validation:jakarta.validation-api")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.set(listOf("-Xjsr305=strict"))
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    dependsOn(tasks.openApiGenerate)
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("$rootDir/lrza-webservice/src/main/resources/api-spec/lrza-webservice.json")
    outputDir.set("$buildDir/generated/lrza-webservice")
    apiPackage.set("nl.topicuszorg.databroker.lrzawebservice.api.generated.api")
    modelPackage.set("nl.topicuszorg.databroker.lrzawebservice.api.generated.model")
    validateSpec.set(true)
    generateApiDocumentation.set(false)
    generateModelDocumentation.set(false)
    generateModelTests.set(false)
    generateApiTests.set(false)
    modelNameSuffix.set("Json")
    configOptions.set(mapOf(
        "enumPropertyNaming" to "UPPERCASE",
        "packageName" to "nl.topicuszorg.databroker.lrzawebservice.api.generated",
        "interfaceOnly" to "true",
        "useTags" to "true",
        "documentationProvider" to "none",
        "useSwaggerUI" to "false",
        "useSpringBoot3" to "true"
    ))
}

sourceSets {
    main {
        java {
            srcDir(files("${openApiGenerate.outputDir.get()}/src/main"))
        }
    }
}

idea {
    module {
        generatedSourceDirs.add(file("${openApiGenerate.outputDir.get()}/src/main"))
    }
}

tasks.register<Copy>("copyBootJar") {
    group = "build"
    from(tasks.named("bootJar"))
    into("../docker")
    rename { fileName ->
        fileName.replace(Regex("""(.+)-([0-9.].+)(-.+)?(.jar)"""), "$1$4")
    }
}
