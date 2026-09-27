import type { NextConfig } from "next";

const nextConfig: NextConfig = {
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
