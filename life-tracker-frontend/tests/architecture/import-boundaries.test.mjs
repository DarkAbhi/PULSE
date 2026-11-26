// Enforces the dependency and testing rules in ARCHITECTURE.md and TESTING.md. Runs with `npm test`.
import assert from "node:assert/strict";
import { readdirSync, readFileSync, statSync } from "node:fs";
import { dirname, join, relative } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..", "..");
const productionRoots = ["app", "components", "features", "hooks", "lib", "providers", "stores"];
const sourceRoots = [...productionRoots, ".storybook", "tests", "e2e"];
const sourceExtensions = /\.(ts|tsx|mjs|css)$/;
const importPattern = /(?:\bfrom\s+|\bimport\s*\(?\s*|@import\s+)["']([^"']+)["']/g;

function walk(dir) {
  let entries;
  try {
    entries = readdirSync(dir);
  } catch {
    return [];
  }
  return entries.flatMap((name) => {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) return walk(path);
    return sourceExtensions.test(name) ? [relative(root, path)] : [];
  });
}

const files = sourceRoots.flatMap((dir) => walk(join(root, dir)));
const imports = files.map((file) => ({
  file,
  specifiers: [...readFileSync(join(root, file), "utf8").matchAll(importPattern)].map((match) => match[1]),
}));

const isTest = (file) => /\.test\.mjs$/.test(file);
// Modules Node runs directly (no bundler, no tsconfig paths): unit tests and the fake E2E backend.
const isNodeModule = (file) => isTest(file) || /^e2e\/fixtures\/.*\.mjs$/.test(file);
const isTestCode = (path) => /^(tests|e2e)\//.test(path) || /\.(test|spec|fixture|factory)\.[cm]?[jt]sx?$/.test(path);
const isProduction = (file) => productionRoots.includes(layerOf(file)) && !isTestCode(file) && !isStory(file);
const isStory = (file) => /\.stories\.tsx$/.test(file);
const featureOf = (path) => path.match(/^features\/([^/]+)/)?.[1];
const layerOf = (path) => path.split("/")[0];

/** Path of an application import without the `@/` prefix, or null for packages and relative paths. */
function aliasTarget(specifier) {
  return specifier.startsWith("@/") ? specifier.slice(2) : null;
}

function violations(check) {
  return imports.flatMap(({ file, specifiers }) =>
    specifiers.flatMap((specifier) => {
      const message = check(file, specifier);
      return message ? [`${file} -> ${specifier}: ${message}`] : [];
    }),
  );
}

test("application modules use the @/ alias instead of relative imports", () => {
  // Exceptions, both because the resolver does not read tsconfig paths:
  // - Node itself, so unit tests import their colocated module relatively (shared test helpers
  //   use the package's `#tests/*` import) and the fake E2E backend imports its route modules;
  // - Tailwind's CSS @import inlining, so stylesheets import feature themes relatively.
  assert.deepEqual(violations((file, specifier) =>
    specifier.startsWith(".") && !isNodeModule(file) && !file.endsWith(".css") ? "use an @/ import" : null), []);
});

test("shared layers never depend on features or routes", () => {
  const allowed = {
    "components/ui": ["components/ui", "lib"],
    "components/layout": ["components/ui", "components/layout", "lib"],
    "components/shared": ["components", "lib", "providers", "hooks"],
    lib: ["lib"],
    providers: ["lib", "providers"],
    hooks: ["lib", "hooks"],
    stores: ["lib", "stores"],
  };
  assert.deepEqual(violations((file, specifier) => {
    const target = aliasTarget(specifier);
    if (!target || isStory(file)) return null;
    const layer = Object.keys(allowed).find((prefix) => file.startsWith(`${prefix}/`));
    if (!layer) return null;
    return allowed[layer].some((prefix) => target === prefix || target.startsWith(`${prefix}/`))
      ? null
      : `${layer} may only import ${allowed[layer].join(", ")}`;
  }), []);
});

test("stories depend only on the design system and global styles", () => {
  assert.deepEqual(violations((file, specifier) => {
    const target = aliasTarget(specifier);
    if (!target || !(isStory(file) || file.startsWith(".storybook/"))) return null;
    return target.startsWith("components/") || target.startsWith("lib/") || target === "app/globals.css"
      ? null
      : "stories must not depend on features or routes";
  }), []);
});

test("features never import routes and cross feature boundaries only through public entry points", () => {
  assert.deepEqual(violations((file, specifier) => {
    const target = aliasTarget(specifier);
    if (!target) return null;
    const sourceFeature = featureOf(file);
    const targetFeature = featureOf(target);

    if (sourceFeature && layerOf(target) === "app") return "features must not import app/";
    if (!targetFeature) return null;

    const publicEntry = target === `features/${targetFeature}` || target === `features/${targetFeature}/server`;
    if (sourceFeature === targetFeature) {
      return publicEntry ? "import the module directly instead of the feature's own public entry" : null;
    }
    if (publicEntry) return null;
    // Route files compose screens; global CSS registers feature theme tokens.
    if (layerOf(file) === "app" && (/^features\/[^/]+\/screens\//.test(target) || (file.endsWith(".css") && /\/styles\/[^/]+\.css$/.test(target)))) {
      return null;
    }
    return `use @/features/${targetFeature} or @/features/${targetFeature}/server`;
  }), []);
});

test("production code never depends on test code", () => {
  assert.deepEqual(violations((file, specifier) => {
    if (!isProduction(file)) return null;
    const target = aliasTarget(specifier) ?? (specifier.startsWith("#tests/") ? `tests/${specifier.slice(7)}` : specifier);
    return isTestCode(target) ? "test code must never reach an application bundle" : null;
  }), []);
});

test("E2E specs drive the app only through the browser and the fake backend", () => {
  assert.deepEqual(violations((file, specifier) => {
    if (layerOf(file) !== "e2e") return null;
    const target = aliasTarget(specifier);
    return target && layerOf(target) !== "e2e" ? "E2E code may import only e2e/ helpers and packages" : null;
  }), []);
});

test("shared test infrastructure is independent of features and routes", () => {
  assert.deepEqual(violations((file, specifier) => {
    if (layerOf(file) !== "tests") return null;
    const target = aliasTarget(specifier);
    return target && ["features", "app"].includes(layerOf(target)) ? "tests/ helpers must not depend on a feature" : null;
  }), []);
});

test("each test runner owns its file names", () => {
  const problems = [];
  for (const file of files) {
    if (/\.spec\.[cm]?[jt]sx?$/.test(file) && !(layerOf(file) === "e2e" && file.endsWith(".spec.ts"))) {
      problems.push(`${file}: browser journeys are e2e/**/*.spec.ts (Playwright)`);
    }
    if (/\.test\.[cm]?[jt]sx?$/.test(file) && (layerOf(file) === "e2e" || !isTest(file))) {
      problems.push(`${file}: unit tests are colocated *.test.mjs files outside e2e/ (node --test)`);
    }
  }
  assert.deepEqual(problems, []);
});

test("server entry points are guarded and client entry points do not re-export server code", () => {
  const problems = [];
  for (const file of files) {
    const source = readFileSync(join(root, file), "utf8");
    if (/^features\/[^/]+\/server\.ts$/.test(file) && !source.includes('import "server-only"')) {
      problems.push(`${file} must import "server-only"`);
    }
    if (/^features\/[^/]+\/index\.ts$/.test(file) && /["']@\/features\/[^"']+\/server(\/|["'])/.test(source)) {
      problems.push(`${file} must not re-export server-only modules`);
    }
  }
  assert.deepEqual(problems, []);
});

test("the feature dependency graph is acyclic", () => {
  const graph = new Map();
  for (const { file, specifiers } of imports) {
    const from = featureOf(file);
    if (!from) continue;
    for (const specifier of specifiers) {
      const to = featureOf(aliasTarget(specifier) ?? "");
      if (to && to !== from) graph.set(from, new Set([...(graph.get(from) ?? []), to]));
    }
  }
  assert.deepEqual(findCycles(graph), []);
});

test("the module dependency graph is acyclic", () => {
  const known = new Set(files);
  const resolve = (target) =>
    [target, `${target}.ts`, `${target}.tsx`, `${target}/index.ts`].find((candidate) => known.has(candidate));
  const graph = new Map(imports.map(({ file, specifiers }) => [
    file,
    new Set(specifiers.map((specifier) => aliasTarget(specifier)).filter(Boolean).map(resolve).filter(Boolean)),
  ]));
  assert.deepEqual(findCycles(graph), []);
});

function findCycles(graph) {
  const cycles = [];
  const state = new Map();
  const stack = [];
  function visit(node) {
    state.set(node, "visiting");
    stack.push(node);
    for (const next of graph.get(node) ?? []) {
      if (state.get(next) === "visiting") cycles.push([...stack.slice(stack.indexOf(next)), next].join(" -> "));
      else if (!state.has(next)) visit(next);
    }
    stack.pop();
    state.set(node, "done");
  }
  for (const node of graph.keys()) if (!state.has(node)) visit(node);
  return cycles;
}
