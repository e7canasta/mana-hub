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
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.assertj)
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    archiveFileName.set("mana-insights.jar")
}
