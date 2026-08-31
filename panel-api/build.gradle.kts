plugins {
    id("hub.spring-service")
}

dependencies {
    implementation(project(":shared-kernel"))
    implementation(project(":residence"))
    implementation(project(":population"))
    implementation(project(":surveillance"))
    implementation(project(":policy"))
    implementation(project(":observation"))
    implementation(project(":history"))

    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.mockk)
    testImplementation(libs.assertj)
}

/* Sembrar el piso mínimo.
 *
 * Existe como tarea y no como instrucción en un comentario porque el comentario
 * anterior decía "armá el classpath a mano" — y eso no lo corre nadie dos veces.
 * Con base nueva: `./gradlew :panel-api:runSeed`. */
tasks.register<JavaExec>("runSeed") {
    group = "application"
    description = "Siembra el piso mínimo: José, su cama y su perfil de observación."
    mainClass.set("com.hub.panel.seed.SeedDataKt")
    classpath = sourceSets["main"].runtimeClasspath
}
