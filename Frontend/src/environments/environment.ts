/**
 * Development environment configuration.
 * The production build swaps this file for environment.production.ts
 * (see angular.json > configurations.production.fileReplacements).
 */
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api',
  /** Full WebSocket URL; leave empty to derive it from the page location. */
  wsUrl: 'ws://localhost:8080/ws',
};
