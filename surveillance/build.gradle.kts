plugins {
    id("hub.spring-service")
}

dependencies {
    implementation(project(":shared-kernel"))
    implementation(project(":population"))
    implementation(project(":residence"))

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.mockk)
    testImplementation(libs.assertj)
}
