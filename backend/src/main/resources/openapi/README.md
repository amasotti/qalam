# OpenAPI specification

`source/openapi.yaml` and its referenced files are the OpenAPI source of truth.

## Layout

- `source/openapi.yaml` holds API metadata, tags, and the path index.
- `source/paths/` contains one path item per file.
- `source/components/schemas/` contains one reusable schema per file.
- `documentation.yaml` is the generated, bundled runtime artifact. Ktor Swagger UI,
  the OpenAPI smoke test, and frontend type generation read this file.

## Workflow

1. Edit `source/` only.
2. Run `just lint-api`.
3. Run `just bundle-api` and include the resulting `documentation.yaml` update.
4. Run `just gtypes` when the API contract changes.

Never edit `documentation.yaml` directly; it will be overwritten by `just bundle-api`.
