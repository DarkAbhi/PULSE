import { test, type BrowserContext } from "@playwright/test";
import { API_URL, APP_URL } from "@/e2e/helpers/environment";

const sessionsResetByTest = new Map<string, Set<string>>();

/**
 * Signs the context in as a fake-backend session. The token selects that spec's fixture data
 * (see e2e/fixtures/backend). The first time a test uses a session, its data is reset, so every
 * test starts from the same state whatever ran before it; later sign-ins within the same test (a
 * second browser, say) keep what the test has saved so far.
 */
export async function signIn(context: BrowserContext, session: string) {
  const { testId } = test.info();
  const reset = sessionsResetByTest.get(testId) ?? new Set<string>();
  sessionsResetByTest.set(testId, reset);
  if (!reset.has(session)) {
    const response = await context.request.post(`${API_URL}/__test/reset?user=${encodeURIComponent(session)}`);
    if (!response.ok()) throw new Error(`Could not reset the "${session}" fixture session: ${response.status()}`);
    reset.add(session);
  }
  await context.addCookies([{ name: "life_session", value: session, url: APP_URL }]);
}
