export function authenticatedFetch(
  url: string,
  init: RequestInit,
  token: string | undefined,
  fetcher: typeof fetch = fetch,
) {
  const headers = new Headers(init.headers);
  headers.delete("Cookie");
  headers.delete("Authorization");
  headers.delete("X-Session-Token");
  if (token) headers.set("Authorization", `Bearer ${token}`);
  return fetcher(url, { ...init, headers, cache: "no-store", redirect: "manual" });
}

export async function proxyAPI(
  request: Request,
  backend: string,
  token: string | undefined,
  fetcher: typeof fetch = fetch,
) {
  const url = new URL(request.url);
  const origin = request.headers.get("Origin");
  if (!["GET", "HEAD", "OPTIONS"].includes(request.method) &&
      ((origin && origin !== url.origin) || request.headers.get("Sec-Fetch-Site") === "cross-site")) {
    return new Response("Cross-origin request denied", {
      status: 403, headers: { "Cache-Control": "private, no-store" },
    });
  }
  const headers = new Headers();
  for (const name of ["Content-Type", "Accept", "Range"]) {
    const value = request.headers.get(name);
    if (value) headers.set(name, value);
  }
  const init: RequestInit & { duplex?: "half" } = { method: request.method, headers, signal: request.signal };
  if (request.method !== "GET" && request.method !== "HEAD") {
    init.body = request.body;
    init.duplex = "half";
  }
  const response = await authenticatedFetch(
    `${backend.replace(/\/$/, "")}${url.pathname}${url.search}`, init, token, fetcher,
  );
  const responseHeaders = new Headers({ "Cache-Control": "private, no-store" });
  for (const name of ["Content-Type", "Content-Disposition", "Content-Range", "Accept-Ranges", "Location"]) {
    const value = response.headers.get(name);
    if (value) responseHeaders.set(name, value);
  }
  if (url.pathname === "/api/auth/login" || url.pathname === "/api/auth/logout") {
    for (const cookie of response.headers.getSetCookie()) responseHeaders.append("Set-Cookie", cookie);
  }
  return new Response(response.body, { status: response.status, headers: responseHeaders });
}
