# Testing Strategy

## Test Pyramid

```
        ┌─────────────┐
        │     E2E     │  Blueprints (main functions)
        │   (5%)      │
        ├─────────────┤
        │ Integration │  BDD Specs (Kotlin Test)
        │   (25%)     │
        ├─────────────┤
        │    Unit     │  Domain logic, builders
        │   (70%)     │
        └─────────────┘
```

## Unit Tests
- Domain entities
- Value objects
- Builder logic

## Integration Tests (BDD Specs)
- API endpoints
- Repository queries
- Cross-module interactions

## Blueprints (E2E)
- Executable scenarios
- Full workflow validation
- Real API calls

## Running Tests

```bash
# All tests
./gradlew test

# Specific module
./gradlew :surveillance:test

# BDD Specs
./gradlew :clients:test --tests "*.specs.*"
```
