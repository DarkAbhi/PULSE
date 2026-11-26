import { fileURLToPath } from "node:url";
import type { StorybookConfig } from "@storybook/nextjs-vite";

const projectRoot = fileURLToPath(new URL("..", import.meta.url));

const config: StorybookConfig = {
  stories: ["../components/**/*.stories.tsx"],
  addons: ["@storybook/addon-docs"],
  framework: "@storybook/nextjs-vite",
  staticDirs: ["../public"],
  // Stories are excluded from tsconfig (Storybook is not installed in CI), so the
  // tsconfig-paths plugin does not map their imports. Declare the @/ alias directly.
  viteFinal(viteConfig) {
    const alias = { find: /^@\//, replacement: projectRoot };
    const existing = viteConfig.resolve?.alias;
    const aliases = Array.isArray(existing)
      ? existing
      : Object.entries(existing ?? {}).map(([find, replacement]) => ({ find, replacement }));
    viteConfig.resolve = { ...viteConfig.resolve, alias: [alias, ...aliases] };
    return viteConfig;
  },
};

export default config;
