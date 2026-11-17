import { defineConfig, devices } from "@playwright/test";

const apiURL = `http://127.0.0.1:${process.env.PULSE_E2E_API_PORT ?? "18081"}`;

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  timeout: 60000,
  expect: { timeout: 15000 },
  reporter: "list",
  outputDir: "test-results",
  use: { baseURL: "http://localhost:3107", trace: "retain-on-failure", screenshot: "only-on-failure" },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"], channel: "chrome" } }],
  webServer: [
    { command: "node e2e/fixtures/meal-api.mjs", url: `${apiURL}/health`, env: { PULSE_E2E_API_PORT: process.env.PULSE_E2E_API_PORT ?? "18081" }, reuseExistingServer: false },
    { command: process.env.PULSE_E2E_PRODUCTION === "1" ? "npm run start -- --port 3107" : "npm run dev -- --port 3107", url: "http://localhost:3107", env: { INTERNAL_API_BASE_URL: apiURL, NEXT_TELEMETRY_DISABLED: "1" }, reuseExistingServer: false, timeout: 180000 },
  ],
});
