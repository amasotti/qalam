import { defineConfig } from "@hey-api/openapi-ts";

const input =
  process.env.OPENAPI_URL ??
  "../backend/src/main/resources/openapi/documentation.yaml";

export default defineConfig({
  client: "@hey-api/client-fetch",
  input,
  output: {
    path: "src/lib/api",
  },
});
