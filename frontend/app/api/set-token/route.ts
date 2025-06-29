import { cookies } from "next/headers";
import { adminAuth } from "@/app/utils/firebase-admin";

export async function POST(req: Request) {
  const { idToken } = await req.json();

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
