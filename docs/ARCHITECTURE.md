# Container Architecture

```mermaid
flowchart LR
    U[Browser] -->|Host port 8080| F[Frontend container<br/>Nginx + React build]
    F -->|/api via application_network| B[Backend container<br/>Spring Boot JRE]
    B -->|JDBC via database_network| D[Database container<br/>MySQL 8.4]
    D --> V[(Named volume<br/>mysql_data)]
```

## Exposure matrix

| Component | Container port | Published to host | Reachable from |
|---|---:|---:|---|
| Frontend/Nginx | 8080 | `${APP_PORT:-8080}` | Browser/host |
| Backend | 8080 | No | Frontend container |
| MySQL | 3306 | No | Backend container |

## Request flow

1. The browser opens `http://localhost:8080`.
2. Nginx serves `/usr/share/nginx/html/index.html` and production assets.
3. React sends requests to relative URLs such as `/api/articles`.
4. Nginx forwards those requests to `backend:8080` without changing `/api`.
5. Spring Boot executes business logic and uses Hibernate/JDBC.
6. MySQL stores tables in `/var/lib/mysql`, backed by the named volume.
