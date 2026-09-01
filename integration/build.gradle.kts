plugins {
    id("hub.spring-service")
}

dependencies {
    implementation(project(":shared-kernel"))
    implementation(project(":observation"))
    implementation(project(":surveillance"))
    implementation(project(":population"))
    implementation(project(":policy"))
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.postgresql)
    implementation("com.manahive:contracts:1.0.0")

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.assertj)
}
