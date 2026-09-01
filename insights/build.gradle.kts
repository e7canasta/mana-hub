plugins {
    id("hub.spring-service")
}

dependencies {
    implementation(project(":shared-kernel"))
    implementation(project(":population"))
    implementation(project(":observation"))
    implementation(project(":care"))
    implementation(project(":history"))
    implementation(project(":policy"))

    implementation("com.manahive:contracts:1.0.0")
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.assertj)
    testImplementation(libs.mockk)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    archiveFileName.set("mana-insights.jar")
}
