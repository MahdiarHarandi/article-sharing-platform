# Nginx Reverse Proxy

## What a reverse proxy does

A reverse proxy accepts client requests on behalf of one or more internal services. The browser communicates only with Nginx. Nginx decides whether to return a static frontend file or forward the request to the backend.

## Routing used in this project

| Browser request | Nginx action |
|---|---|
| `/`, `/login`, `/profile`, `/articles/...` | Serve the React SPA and use `index.html` fallback |
| `/assets/...` | Serve versioned production assets with long-term cache headers |
| `/api/...` | Proxy the unchanged path to `http://backend:8080` |
| `/healthz` | Return Nginx container health response |

The frontend production bundle uses the relative API base URL `/api`. Therefore the browser sends API calls to the same scheme, host, and port used for the webpage. It never needs to know the backend container address.

## Important proxy headers

Nginx sends:

- `Host`
- `X-Real-IP`
- `X-Forwarded-For`
- `X-Forwarded-Proto`

Spring Boot is configured with `server.forward-headers-strategy=framework`, allowing it to understand the original request information supplied by the proxy.

## Benefits

- one public host port
- internal backend is not directly exposed
- no production CORS dependency for normal browser requests
- frontend and API share the same origin
- centralized logging and HTTP security headers
- efficient static file delivery and compression
- React Router browser refreshes work through SPA fallback

## Costs and disadvantages

- one extra component must be configured and monitored
- an incorrect location or `proxy_pass` rule can break API paths
- requests have one additional internal hop
- Nginx becomes the public entry point and must be healthy

The Compose health check and restart policy reduce the operational risk for this project.
