plugins {
    id("hub.kotlin-common")
    application
}

application {
    mainClass = "com.hub.blueprints.AppKt"
}

dependencies {
    implementation(project(":clients"))
    implementation(kotlin("stdlib"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")
}
