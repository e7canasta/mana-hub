# API Design

## REST Conventions

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/{resources}` | List |
| GET | `/api/v1/{resources}/{id}` | Retrieve |
| POST | `/api/v1/{resources}` | Create |
| PATCH | `/api/v1/{resources}/{id}` | Update |
| DELETE | `/api/v1/{resources}/{id}` | Delete |

## Internal APIs

| Method | Path | Description |
|--------|------|-------------|
| POST | `/internal/v1/events` | Ingest sensor event |
| POST | `/internal/v1/clinical/*` | Ingest clinical data |
| POST | `/internal/v1/notifications` | Ingest notification |

## Error Response

```json
{
  "error": "NOT_FOUND",
  "message": "Resident not found",
  "details": { "residentId": "xxx" }
}
```

## Pagination

```json
{
  "data": [...],
  "total": 100,
  "page": 1,
  "pageSize": 20
}
```
