plugins {
    id("hub.kotlin-common")
}

dependencies {
    api("io.nats:jnats:2.21.4")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.2")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")
    // Spring only for NatsClientConfiguration @Bean — compileOnly
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:4.0.1")
    compileOnly("org.springframework:spring-context:6.2.1")
    compileOnly("jakarta.annotation:jakarta.annotation-api:3.0.0")
    compileOnly("org.slf4j:slf4j-api:2.0.17")
}
