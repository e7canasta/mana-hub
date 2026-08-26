plugins {
    id("hub.kotlin-common")
}

val springFramework = "6.2.1"
val jacksonVersion = "2.19.2"
val slf4jVersion = "2.0.16"

dependencies {
    api("org.springframework:spring-context:$springFramework")
    api("org.springframework:spring-tx:$springFramework")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
}
