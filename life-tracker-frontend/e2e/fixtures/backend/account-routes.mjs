// Sign-in, session and profile. Tokens "anonymous" and "expired-session" get 401 from every API route.

/** Answers the request and returns true, or returns undefined to let the next domain try. */
export function route({ req, res, url, token, body, send }) {
  if (url.pathname === "/api/auth/login" && req.method === "POST") {
    if (body.username !== "dashboard-test" || body.password !== "fixture-password") {
      return send(401, { error: "Invalid username or password" });
    }
    res.setHeader("Set-Cookie", "life_session=dashboard-test; Path=/; HttpOnly; SameSite=Lax");
    return send(200, { username: body.username });
  }
  if (url.pathname === "/api/auth/session") {
    return token === "anonymous" || token === "expired-session"
      ? send(401, { error: "Expired session" })
      : send(200, { username: token });
  }
  if ((token === "anonymous" || token === "expired-session") && url.pathname.startsWith("/api/")) {
    return send(401, { error: "Expired session" });
  }
  if (url.pathname === "/api/profile/api-keys" && req.method === "GET") return send(200, []);
  if (url.pathname === "/api/profile") {
    return send(200, { has_profile: true, name: "Browser test", timezone: "Asia/Kolkata", currency: "INR" });
  }
}
