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
        html.required.set(true)
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named<JacocoReport>("jacocoTestReport"))
    violationRules {
        rule {
            limit { minimum = "0.0".toBigDecimal() } // baseline: no rompe CI; subir a 0.7 cuando policy tenga specs
        }
    }
}
