import { defineConfig } from "@hey-api/openapi-ts";

// @ts-ignore
export default defineConfig({
  client: "@hey-api/client-fetch",
  input: "../backend/src/main/resources/openapi/documentation.yaml",
  output: {
    path: "src/lib/api",
  },
});
