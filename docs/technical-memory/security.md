# Security

## Authentication
- Session-based authentication
- Login via `POST /api/v1/auth/login`
- Session token in header: `Authorization: Bearer <token>`

## RBAC (Role-Based Access Control)

| Role | Permissions |
|------|-------------|
| OWNER | Full access |
| SUPERVISOR | Manage residents, episodes, rounds |
| STAFF | View residents, complete rounds, add notes |

## API Security

| Endpoint | Auth Required | Role |
|----------|---------------|------|
| `/api/v1/*` | Yes | Any |
| `/internal/v1/*` | Service-to-service | Internal |

## Data Security
- Passwords hashed with bcrypt
- No secrets in code
- Audit log for all mutations
