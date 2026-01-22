import { NextRequest } from "next/server";
import { getIdeas } from "@/lib/server/ideas";

const DEFAULT_LIMIT = 10;
const MAX_LIMIT = 50;

function parseCursor(value: string | null) {
  if (!value) return 0;
  const parsed = Number.parseInt(value, 10);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : 0;
}

function parseLimit(value: string | null) {
  if (!value) return DEFAULT_LIMIT;
  const parsed = Number.parseInt(value, 10);
  if (!Number.isFinite(parsed)) return DEFAULT_LIMIT;
  return Math.min(Math.max(parsed, 1), MAX_LIMIT);
}

export async function GET(req: NextRequest) {
  try {
    const { searchParams } = new URL(req.url);
    const cursor = parseCursor(searchParams.get("cursor"));
    const limit = parseLimit(searchParams.get("limit"));

    const items = await getIdeas(cursor, limit);
    const nextCursor = items.length < limit ? null : String(cursor + items.length);

    return Response.json({ items, nextCursor });
  } catch (error) {
    const message = error instanceof Error ? error.message : "";
    if (
      message.includes("Unauthenticated") ||
      message.includes("Invalid or expired ID token")
    ) {
      return Response.json({ message: "Unauthorized" }, { status: 401 });
    }

    return Response.json({ message: "Failed to load ideas" }, { status: 500 });
  }
}
