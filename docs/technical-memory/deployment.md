# Deployment

## Docker

```dockerfile
FROM eclipse-temurin:21-jre-alpine
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mana-hub
spec:
  replicas: 3
  selector:
    matchLabels:
      app: mana-hub
  template:
    spec:
      containers:
      - name: mana-hub
        image: mana-hub:latest
        ports:
        - containerPort: 8080
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL URL | jdbc:postgresql://localhost:5432/mana_hub |
| `SPRING_DATASOURCE_USERNAME` | DB username | postgres |
| `SPRING_DATASOURCE_PASSWORD` | DB password | — |
