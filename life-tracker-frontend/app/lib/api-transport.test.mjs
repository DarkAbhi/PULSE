import assert from "node:assert/strict";
import { test } from "node:test";
import { authenticatedFetch, proxyAPI } from "./api-transport.ts";

test("private requests use only the current session and cannot be cached or redirect credentials", async () => {
  const calls = [];
  const fetcher = async (url, init) => {
    calls.push(init);
    return new Response("ok");
  };
  const init = { headers: { Cookie: "unrelated=secret", Authorization: "Bearer forged", "X-Session-Token": "forged" }, cache: "force-cache" };
  await authenticatedFetch("http://api/api/vehicles", init, "alice", fetcher);
  await authenticatedFetch("http://api/api/vehicles", init, "bob", fetcher);
  await authenticatedFetch("http://api/api/vehicles", init, undefined, fetcher);
  assert.deepEqual(calls.map((call) => call.headers.get("Authorization")), ["Bearer alice", "Bearer bob", null]);
  for (const call of calls) {
    assert.equal(call.headers.has("Cookie"), false);
    assert.equal(call.headers.has("X-Session-Token"), false);
    assert.equal(call.cache, "no-store");
    assert.equal(call.redirect, "manual");
  }
});

test("browser proxy streams uploads with the server session and preserves download redirects", async () => {
  const form = new FormData();
  form.append("file", new Blob(["receipt"]), "receipt.txt");
  const request = new Request("https://pulse.example/api/vehicles/1/attachments?download=1", {
    method: "POST", headers: { Origin: "https://pulse.example", Authorization: "Bearer forged", Cookie: "other=secret" }, body: form,
  });
  const response = await proxyAPI(request, "http://api:8080", "session", async (url, init) => {
    assert.equal(url, "http://api:8080/api/vehicles/1/attachments?download=1");
    assert.equal(init.headers.get("Authorization"), "Bearer session");
    assert.equal(init.headers.has("Cookie"), false);
    assert.match(init.headers.get("Content-Type"), /^multipart\/form-data; boundary=/);
    assert.match(await new Response(init.body).text(), /receipt/);
    return new Response(null, { status: 302, headers: { Location: "https://storage.example/signed" } });
  });
  assert.equal(response.status, 302);
  assert.equal(response.headers.get("Location"), "https://storage.example/signed");
  assert.equal(response.headers.get("Cache-Control"), "private, no-store");
});

test("login and logout cookies survive the proxy without exposing them to JavaScript", async () => {
  for (const path of ["login", "logout"]) {
    const response = await proxyAPI(new Request(`https://pulse.example/api/auth/${path}`, { method: "POST" }), "http://api", undefined,
      async () => new Response("{}", { headers: { "Set-Cookie": "life_session=token; HttpOnly; Path=/; SameSite=Lax" } }));
    assert.match(response.headers.get("Set-Cookie"), /HttpOnly/);
  }
});

test("cross-origin writes are rejected before forwarding a credential", async () => {
  const response = await proxyAPI(new Request("https://pulse.example/api/vehicles", {
    method: "DELETE", headers: { Origin: "https://attacker.example" },
  }), "http://api", "session", async () => { throw new Error("must not forward"); });
  assert.equal(response.status, 403);
});
