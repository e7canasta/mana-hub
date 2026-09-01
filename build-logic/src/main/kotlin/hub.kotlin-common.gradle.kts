plugins {
    id("org.jetbrains.kotlin.jvm")
    jacoco
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        allWarningsAsErrors = false
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(false)
    }
}
