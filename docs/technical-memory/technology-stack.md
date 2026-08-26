# Technology Stack

## Backend
- **Language:** Kotlin 2.0+
- **Framework:** Spring Boot 3.x
- **Build:** Gradle 9.x (Kotlin DSL)
- **Database:** PostgreSQL 16
- **Migrations:** Flyway

## Testing
- **Unit:** JUnit 5 + Kotlin Test
- **BDD:** Kotlin Spec (DescribeSpec)
- **Integration:** Spring Boot Test

## Client Library
- **HTTP:** java.net.http.HttpClient
- **JSON:** Jackson (jackson-module-kotlin)
- **DSL:** Kotlin type-safe builders + @DslMarker

## Infrastructure
- **Container:** Docker
- **Orchestration:** Kubernetes
- **CI/CD:** GitHub Actions

## Dependencies

| Module | Dependencies |
|--------|--------------|
| shared-kernel | kotlin-stdlib |
| identity | spring-boot-starter-data-jpa, spring-boot-starter-security |
| residence | shared-kernel, population |
| population | shared-kernel |
| policy | shared-kernel, population |
| surveillance | shared-kernel, population, residence, evidence |
| observation | shared-kernel, population, residence |
| evidence | shared-kernel |
| care | shared-kernel, residence, population |
| history | shared-kernel, observation, evidence, care |
| audit | shared-kernel |
| streams | shared-kernel, residence |
| clients | kotlin-stdlib, jackson-module-kotlin |
