// Serves the production build the way the Docker image does (see Dockerfile): the standalone
// server with the static assets and public/ copied beside it. Used when PULSE_E2E_PRODUCTION=1.
import { spawn } from "node:child_process";
import { cpSync, existsSync } from "node:fs";

const standalone = ".next/standalone";
if (!existsSync(`${standalone}/server.js`)) {
  console.error("No standalone build found. Run `npm run build` first.");
  process.exit(1);
}
cpSync(".next/static", `${standalone}/.next/static`, { recursive: true });
cpSync("public", `${standalone}/public`, { recursive: true });

// "::" accepts IPv4 and IPv6, so http://localhost works whichever address it resolves to.
const server = spawn(process.execPath, [`${standalone}/server.js`], {
  stdio: "inherit",
  env: { ...process.env, HOSTNAME: "::" },
});
for (const signal of ["SIGINT", "SIGTERM"]) process.on(signal, () => server.kill(signal));
server.on("exit", (code) => process.exit(code ?? 0));
