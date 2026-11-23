import { apiBaseURL } from "@/app/lib/server-api";
import { cookies } from "next/headers";
import { proxyAPI } from "@/app/lib/api-transport";

async function proxy(request: Request) {
  try {
    return await proxyAPI(
      request,
      apiBaseURL,
      (await cookies()).get("life_session")?.value,
    );
  } catch {
    return Response.json({ error: "We couldn't reach the server. Please try again." }, {
      status: 502, headers: { "Cache-Control": "private, no-store" },
    });
  }
}

export { proxy as GET, proxy as HEAD, proxy as POST, proxy as PUT, proxy as PATCH, proxy as DELETE, proxy as OPTIONS };
