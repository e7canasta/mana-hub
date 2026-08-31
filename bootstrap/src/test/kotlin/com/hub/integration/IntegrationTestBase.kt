package com.hub.integration

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Base de tests de integración — Spring Boot 4 + Kotlin 2026 style.
 *
 * Patrones aplicados:
 *  - @DynamicPropertySource (reemplaza ApplicationContextInitializer)
 *  - @Container @JvmStatic (contenedor compartido entre tests)
 *  - JdbcTemplate directo al container para seed/clean (no depende del context)
 *  - TRUNCATE + seed en @BeforeEach = DB limpia por test
 *  - @DirtiesContext = context nuevo por clase (aislamiento total)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class IntegrationTestBase {

    @Value("\${local.server.port}")
    lateinit var port: String

    val baseUrl: String get() = "http://localhost:$port"

    val client: org.springframework.web.client.RestTemplate by lazy {
        org.springframework.web.client.RestTemplate(
            org.springframework.http.client.HttpComponentsClientHttpRequestFactory()
        )
    }

    fun get(path: String): Any? {
        val response = client.getForEntity("$baseUrl$path", String::class.java)
        if (response.statusCode.isError) {
            throw AssertionError("GET $path returned ${response.statusCode}:\n${response.body}")
        }
        return com.fasterxml.jackson.databind.ObjectMapper().readValue(response.body, Any::class.java)
    }

    fun getList(path: String): List<*> = get(path) as? List<*> ?: emptyList<Any>()

    fun post(path: String, body: Map<String, Any?>): Any? {
        val response = client.postForEntity("$baseUrl$path", body, String::class.java)
        if (response.statusCode.isError) {
            throw AssertionError("POST $path returned ${response.statusCode}:\n${response.body}")
        }
        return com.fasterxml.jackson.databind.ObjectMapper().readValue(response.body, Any::class.java)
    }

    fun cleanDatabase() {
        val jdbc = newJdbc()
        val tables = jdbc.queryForList(
            "SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename != 'flyway_schema_history'"
        ).mapNotNull { it["tablename"] as? String }
        if (tables.isNotEmpty()) {
            jdbc.execute("TRUNCATE TABLE ${tables.joinToString(", ") { "\"$it\"" }} CASCADE")
        }
        seedData(jdbc)
    }

    private fun seedData(jdbc: JdbcTemplate = newJdbc()) {
        val seed = ClassPathResource("seed-test.sql")
        if (!seed.exists()) return
        val sql = seed.inputStream.bufferedReader().readText()
        sql.split(";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { raw ->
                val cleaned = raw.lines()
                    .filter { !it.trimStart().startsWith("--") }
                    .joinToString("\n").trim()
                if (cleaned.isNotEmpty()) {
                    try { jdbc.execute(cleaned) }
                    catch (e: Exception) {
                        System.err.println("SEED ERROR: ${e.message}\nSQL: ${cleaned.take(120)}")
                    }
                }
            }
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17")
            .withDatabaseName("mana_hub_test")
            .withUsername("test")
            .withPassword("test")

        /** Crea un JdbcTemplate nuevo cada vez — evita conexiones stale tras restart del container. */
        private fun newJdbc(): JdbcTemplate = JdbcTemplate(DriverManagerDataSource().apply {
            url = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
        })

        /**
         * Spring Boot 4: @DynamicPropertySource reemplaza el custom
         * ApplicationContextInitializer que teníamos antes.
         */
        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("nats.enabled") { "false" }
            registry.add("bridge.url") { "http://localhost:9999" }
            registry.add("bridge.webhook.url") { "http://localhost:9999/webhooks/policy-change" }
            registry.add("bridge.target.url") { "http://localhost:8080" }
            registry.add("server.error.include-stacktrace") { "always" }
            registry.add("server.error.include-message") { "always" }
            registry.add("server.error.include-exception") { "true" }
        }
    }
}
