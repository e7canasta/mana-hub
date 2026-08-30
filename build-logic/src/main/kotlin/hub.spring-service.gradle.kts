plugins {
    id("hub.kotlin-common")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.kotlin.plugin.jpa")
    id("org.springframework.boot")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    "implementation"(platform("org.springframework.boot:spring-boot-dependencies:${libs.findVersion("spring-boot").get()}"))
    "implementation"("org.springframework.boot:spring-boot-starter")
    "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
    "testImplementation"("org.springframework.boot:spring-boot-starter-test")
}

// Leyden/CDS no mapea JARs firmados (BouncyCastle bcprov-lts8on). Desempaquetar + strip firmas.
tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    requiresUnpack("**/bcprov-*.jar")
    // El doFirst es necesario: exclude en configuración no aplica a BOOT-INF/lib
    // pero al re-empaquetar el-fat-jar sí limpia META-INF del propio jar.
    // Para bcprov transitivo, requiresUnpack es lo que evita el "Signed JAR" skip.
}

// JNA nunca en runtime (mordant usa JNA solo en batch CLI)
configurations.named("runtimeClasspath") {
    exclude(group = "net.java.dev.jna", module = "jna")
    exclude(group = "net.java.dev.jna", module = "jna-platform")
}
