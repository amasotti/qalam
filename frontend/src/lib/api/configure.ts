// One-time API client configuration. Import this in +layout.ts or stores
// to ensure the client is configured before any API calls.
//
// During dev, relative URLs work because Vite proxies /api to the backend.
// In production, set PUBLIC_API_BASE_URL to the backend URL if needed.

import { createClient, createConfig } from './client/index.js';
import type { Client } from './client/index.js';

let _client: Client | null = null;

export function getApiClient(): Client {
  if (!_client) {
    _client = createClient(
      createConfig({
        baseUrl:
          typeof window !== 'undefined'
            ? (import.meta.env.PUBLIC_API_BASE_URL ?? '')
            : '',
      }),
    );
  }
  return _client;
}
