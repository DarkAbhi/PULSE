/**
 * Replaces `target[key]` for one test and restores the original descriptor (or removes the
 * property) when the test ends, so stubbed browser globals never leak into later tests.
 * Later changes in the same test can redefine the property directly; it stays configurable.
 *
 * @param {import("node:test").TestContext} context
 * @param {object} target
 * @param {PropertyKey} key
 * @param {unknown} value
 */
export function stubProperty(context, target, key, value) {
  const original = Object.getOwnPropertyDescriptor(target, key);
  context.after(() => {
    if (original) Object.defineProperty(target, key, original);
    else delete target[key];
  });
  Object.defineProperty(target, key, { configurable: true, writable: true, value });
}
