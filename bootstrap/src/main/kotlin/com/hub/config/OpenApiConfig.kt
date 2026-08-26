package com.hub.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Mana Hub — System of Record API")
                .version("1.0.0-SNAPSHOT")
                .description(
                    """
                    |Mana Hub es el System of Record del dominio de monitoreo de residencias de adultos mayores.
                    |
                    |## Contextos
                    |- **Residence** — Facilities, Wings, Rooms, Beds
                    |- **Population** — Residents, Assignments
                    |- **Observation** — Sensor Events, Scene Events, Summaries
                    |- **Policy** — Alarm Profiles, Episodes
                    |- **Identity** — Users, Auth
                    """.trimMargin()
                )
                .contact(Contact().name("Mana Hub Team"))
                .license(License().name("Apache 2.0"))
        )
        .servers(
            listOf(Server().url("http://localhost:8080").description("Local Dev"))
        )
}
