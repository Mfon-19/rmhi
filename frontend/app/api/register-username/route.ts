import { NextRequest } from "next/server";
import { registerUsername } from "@/lib/server/ideas";

export async function POST(req: NextRequest) {
  const body = await req.json().catch(() => null);
  const idToken = body?.idToken;
  const username = body?.username;

  if (
    typeof idToken !== "string" ||
    !idToken ||
    typeof username !== "string" ||
    !username
  ) {
    return Response.json({ message: "Invalid request" }, { status: 400 });
  }

  try {
    await registerUsername(idToken, username);
    return Response.json({ success: true });
  } catch (error) {
    const message =
      error instanceof Error && error.message
        ? error.message
        : "Failed to register username";
    const status = message.includes("Conflict") ? 409 : 500;
    return Response.json({ message }, { status });
  }
}
