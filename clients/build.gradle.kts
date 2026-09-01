plugins {
    id("hub.kotlin-common")
    application
}

application {
    mainClass = "com.hub.clients.simulation.SimulationScenarioKt"
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(project(":shared-kernel"))
    implementation(project(":panel-api"))
    implementation(kotlin("stdlib"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:${libs.findVersion("jackson").get()}")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${libs.findVersion("jackson").get()}")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("io.kotest:kotest-assertions-core:5.8.1")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.1")
}

tasks.test {
    useJUnitPlatform()
}
