import { cookies } from "next/headers";
import { adminAuth } from "@/lib/firebase/admin";

export async function POST(req: Request) {
  const body = await req.json().catch(() => null);
  const idToken = body?.idToken;

  if (!idToken || typeof idToken !== "string") {
    return new Response(null, { status: 400 });
  }

  try {
    await adminAuth.verifyIdToken(idToken);
    const cookie = await cookies();

    cookie.set("idToken", idToken, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      path: "/",
      maxAge: 60 * 60,
    });

    return new Response(null, { status: 204 });
  } catch {
    return new Response(null, { status: 401 });
  }
}

export async function DELETE() {
  const cookieStore = await cookies();
  cookieStore.delete("idToken");
  return new Response(null, { status: 204 });
}
