import assert from "node:assert/strict";
import { test } from "node:test";
import { commitImport, editImport, ImportError, uploadStatement } from "./statement-import.ts";

test("uploads with a generic filename and reports progress before returning a preview", async context => {
  let form;
  let method;
  let path;
  const updates = [];
  const original = Object.getOwnPropertyDescriptor(globalThis, "XMLHttpRequest");
  context.after(() => {
    if (original) Object.defineProperty(globalThis, "XMLHttpRequest", original);
    else delete globalThis.XMLHttpRequest;
  });
  globalThis.XMLHttpRequest = class {
    upload = {};
    response = { id: "draft-id" };
    status = 201;

    open(verb, url) {
      method = verb;
      path = url;
    }

    send(body) {
      form = body;
      this.upload.onprogress({ lengthComputable: true, loaded: 1, total: 2 });
      this.upload.onload();
      this.onload();
    }
  };
  await uploadStatement(new File(["%PDF"], "private-bank.pdf", { type: "application/pdf" }), 7, v => updates.push(v));
  assert.equal(method, "POST");
  assert.equal(path, "/api/horizon/statement-imports");
  assert.equal(form.get("account_id"), "7");
  assert.equal(form.get("file").name, "statement.pdf");
  assert.deepEqual(updates, [0.5, 1]);
});

test("saves explicit exclusions and commits the supplied draft revision", async context => {
  const requests = [];
  context.mock.method(globalThis, "fetch", async (url, options) => {
    requests.push([url, options.method, JSON.parse(options.body)]);
    return Response.json({ revision: 3 });
  });
  await editImport("draft", 2, true, [{ id: 3, included: false }]);
  await commitImport("draft", 3);
  assert.deepEqual(requests, [
    ["/api/horizon/statement-imports/draft/rows", "PATCH", {
      revision: 2, account_confirmed: true, changes: [{ id: 3, included: false }],
    }],
    ["/api/horizon/statement-imports/draft/commit", "POST", { revision: 3 }],
  ]);
});

test("surfaces safe backend errors for a draft that changed", async context => {
  context.mock.method(globalThis, "fetch", async () =>
    Response.json({ code: "STALE_REVISION", error: "Reload the preview." }, { status: 409 }));
  await assert.rejects(commitImport("draft", 3), error =>
    error instanceof ImportError && error.code === "STALE_REVISION" && error.message === "Reload the preview.");
});
