import { defineConfig, devices } from "@playwright/test";
import { API_PORT, API_URL, APP_PORT, APP_URL } from "@/e2e/helpers/environment";

export default defineConfig({
  testDir: "./e2e",
  // Only browser journeys; unit tests (*.test.mjs) belong to `npm test`.
  testMatch: "**/*.spec.ts",
  fullyParallel: false,
  workers: 1,
  timeout: 60000,
  expect: { timeout: 15000 },
  reporter: "list",
  outputDir: "test-results",
  use: { baseURL: APP_URL, trace: "retain-on-failure", screenshot: "only-on-failure" },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"], channel: "chrome" } }],
  webServer: [
    { command: "node e2e/fixtures/backend/server.mjs", url: `${API_URL}/health`, env: { PULSE_E2E_API_PORT: API_PORT }, reuseExistingServer: false },
    { command: process.env.PULSE_E2E_PRODUCTION === "1" ? `npm run start -- --port ${APP_PORT}` : `npm run dev -- --port ${APP_PORT}`, url: APP_URL, env: { INTERNAL_API_BASE_URL: API_URL, NEXT_TELEMETRY_DISABLED: "1" }, reuseExistingServer: false, timeout: 180000 },
  ],
});
