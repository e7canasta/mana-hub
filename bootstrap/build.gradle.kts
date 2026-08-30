plugins {
    id("hub.spring-service")
}

val springBootVersion = libs.versions.spring.boot.get()

dependencies {
    implementation(project(":shared-kernel"))
    implementation(project(":identity"))
    implementation(project(":audit"))
    implementation(project(":residence"))
    implementation(project(":population"))
    implementation(project(":coverage"))
    implementation(project(":care"))
    implementation(project(":history"))
    implementation(project(":policy"))
    implementation(project(":surveillance"))
    implementation(project(":evidence"))
    implementation(project(":streams"))
    implementation(project(":observation"))
    implementation(project(":event-bridge"))

    implementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.postgresql)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    runtimeOnly(libs.postgresql)

configurations.named("runtimeClasspath") {
    exclude(group = "net.java.dev.jna", module = "jna")
    exclude(group = "net.java.dev.jna", module = "jna-platform")
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    requiresUnpack("**/bcprov-*.jar")
}

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.mockk)
    testImplementation(libs.assertj)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.bootBuildImage {
    builder = "eclipse-temurin:25-jammy"
    imageName = "mana-hub:latest"
}
