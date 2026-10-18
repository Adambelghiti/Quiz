/**
 * Production environment configuration.
 * Assumes the Angular app and the API are served from the same origin
 * (e.g. behind a reverse proxy that forwards /api and /ws to the backend).
 */
export const environment = {
  production: true,
  apiBaseUrl: '/api',
  /** Empty means "derive ws:// or wss:// from the current page location". */
  wsUrl: '',
};
