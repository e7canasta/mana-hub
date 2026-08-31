plugins {
    id("hub.spring-service")
}

dependencies {
    implementation(project(":shared-kernel"))
    /* El timeline de un episodio **es** historia de observación consolidada: las
     * señales del sentinel y los eventos de escena viven en `observation`, y
     * `EpisodeTimelineDeriver` los lee por sus repositorios JPA en vez de por SQL
     * a mano. No hay ciclo — `observation` no depende de `history`. */
    implementation(project(":observation"))
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
