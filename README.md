
# ![RealWorld Example App](logo.png)

> ### real-world-tapir codebase containing real world examples (CRUD, auth, advanced patterns, etc) that adheres to the [RealWorld](https://github.com/gothinkster/realworld) spec and API.


[![CI](https://github.com/note/real-world-tapir/actions/workflows/ci.yml/badge.svg)](https://github.com/note/real-world-tapir/actions)

PostgreSQL used for persistence

# Main Dependencies

- doobie
- tapir
- circe
- flyway

# Development

To spin up a local PostgreSQL instance:

```bash
docker-compose up -d db
```
