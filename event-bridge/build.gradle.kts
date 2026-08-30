plugins {
    id("hub.spring-service")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    // Shared Kernel — mismos tipos que mana-hive publica
    implementation("com.manahive:contracts:1.0.0")
    implementation("io.nats:jnats:2.21.4")
    implementation(libs.findLibrary("jackson.module.kotlin").get())
    implementation(libs.findLibrary("jackson.datatype.jsr310").get())
    implementation(libs.findLibrary("spring.boot.starter.web").get())
    implementation("org.springframework.boot:spring-boot-starter-webflux")
}
