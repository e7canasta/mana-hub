package com.hub.integration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.RestTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [com.hub.ManaHubApplication::class]
)
@Testcontainers
@ContextConfiguration(initializers = [IntegrationTestBase.Companion.DatasourceInitializer::class])
abstract class IntegrationTestBase {

    @Value("\${local.server.port}")
    lateinit var port: String

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    val baseUrl: String get() = "http://localhost:$port"

    val client: RestTemplate by lazy { RestTemplate() }

    fun get(path: String): Any? =
        client.getForObject("$baseUrl$path", Any::class.java)

    fun getList(path: String): List<*> =
        client.getForObject("$baseUrl$path", List::class.java) ?: emptyList<Any>()

    fun post(path: String, body: Map<String, Any?>): Any? {
        val response = client.postForEntity("$baseUrl$path", body, Any::class.java)
        if (response.statusCode.isError) {
            throw AssertionError("POST $path returned ${response.statusCode}: ${response.body}")
        }
        return response.body
    }

    fun postRaw(path: String, body: Map<String, Any?>): org.springframework.http.ResponseEntity<*> =
        client.postForEntity("$baseUrl$path", body, Any::class.java)

    fun patch(path: String, body: Map<String, Any?>): Any? =
        client.exchange(
            "$baseUrl$path",
            org.springframework.http.HttpMethod.PATCH,
            org.springframework.http.HttpEntity(body),
            Any::class.java
        ).body

    companion object {
        @Container
        @JvmField
        val postgres = PostgreSQLContainer("postgres:17")
            .withDatabaseName("mana_hub_test")
            .withUsername("test")
            .withPassword("test")
            .also { it.start() }

        class DatasourceInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
            override fun initialize(context: ConfigurableApplicationContext) {
                TestPropertyValues.of(
                    "spring.datasource.url=${postgres.jdbcUrl}",
                    "spring.datasource.username=${postgres.username}",
                    "spring.datasource.password=${postgres.password}",
                    "spring.flyway.enabled=true",
                    "nats.enabled=false",
                    "bridge.url=http://localhost:9999",
                    "bridge.webhook.url=http://localhost:9999/webhooks/policy-change",
                    "bridge.target.url=http://localhost:8080",
                ).applyTo(context.environment)

                val ds = org.springframework.jdbc.datasource.DriverManagerDataSource().apply {
                    url = postgres.jdbcUrl
                    username = postgres.username
                    password = postgres.password
                }
                try {
                    val flyway = org.flywaydb.core.Flyway.configure()
                        .dataSource(ds)
                        .locations("classpath:db/migration")
                        .load()
                    flyway.migrate()

                    val jdbc = JdbcTemplate(ds)
                    val seed = ClassPathResource("seed-test.sql")
                    if (seed.exists()) {
                        val sql = seed.inputStream.bufferedReader().readText()
                        sql.split(";")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .forEach { raw ->
                                // Strip SQL line comments (-- ...) before checking
                                val cleaned = raw.lines()
                                    .filter { line -> !line.trimStart().startsWith("--") }
                                    .joinToString("\n")
                                    .trim()
                                if (cleaned.isNotEmpty()) {
                                    try { jdbc.execute(cleaned) }
                                    catch (e: Exception) {
                                        System.err.println("SEED ERROR: ${e.message}\nSQL: ${cleaned.take(120)}")
                                    }
                                }
                            }
                    }
                } finally {
                    // no-op: DriverManagerDataSource has no close
                }
            }
        }
    }
}
