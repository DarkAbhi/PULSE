import type { NextConfig } from "next";
import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";

const releasePath = existsSync("/release.json")
  ? "/release.json"
  : resolve(process.cwd(), "../release.json");
const release = JSON.parse(readFileSync(releasePath, "utf8"));
if (typeof release.version !== "string" || !release.version) {
  throw new Error("release.json must contain a Pulse version");
}

const nextConfig: NextConfig = {
  output: "standalone",
  env: {
    NEXT_PUBLIC_PULSE_VERSION: release.version,
    NEXT_PUBLIC_PULSE_RELEASE_DATE: release.releasedAt ?? "",
    NEXT_PUBLIC_PULSE_BUILD_DATE: new Date().toISOString(),
  },
  experimental: {
    serverActions: { bodySizeLimit: "12mb" },
  },
  async rewrites() {
    return [{
      source: "/api/:path*",
      destination: `${process.env.INTERNAL_API_BASE_URL ?? "http://localhost:8080"}/api/:path*`,
    }];
  },
};

export default nextConfig;
