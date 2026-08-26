# Spec: User Registration

## Context Group
**Facility Management** — Residence + Streams + Identity

## DSL Spec (Kotlin Test)

```kotlin
class UserRegistrationSpec : DescribeSpec({
    describe("User registration") {
        it("should register user with role") {
            // Given
            val identity = hub.identity

            // When
            val user = identity.register(
                username = "nurse_maria",
                displayName = "Maria Rodriguez",
                role = Role.STAFF,
                password = "secure123"
            )

            // Then
            user.username shouldBe "nurse_maria"
            user.role shouldBe Role.STAFF
        }

        it("should login and get session") {
            // Given
            identity.register("admin", "Admin", Role.OWNER, "admin123")

            // When
            val session = identity.login("admin", "admin123")

            // Then
            session.username shouldBe "admin"
            session.role shouldBe Role.OWNER
        }
    }
})
```

## Status
📝 Pending implementation
