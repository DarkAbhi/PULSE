// Addresses shared by playwright.config.ts and the specs. The app and the fake backend run on
// dedicated ports so the suite never talks to a real backend or real data.
export const APP_PORT = 3107;
export const APP_URL = `http://localhost:${APP_PORT}`;
export const API_PORT = process.env.PULSE_E2E_API_PORT ?? "18081";
export const API_URL = `http://127.0.0.1:${API_PORT}`;
