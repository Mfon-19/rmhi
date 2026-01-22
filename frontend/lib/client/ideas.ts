import { Idea } from "@/lib/types";

export interface IdeasPage {
  items: Idea[];
  nextCursor: string | null;
}

export async function fetchIdeasPage(
  cursor: string | null,
  limit: number
): Promise<IdeasPage> {
  const params = new URLSearchParams();
  if (cursor) params.set("cursor", cursor);
  params.set("limit", String(limit));

  const response = await fetch(`/api/ideas?${params.toString()}`, {
    method: "GET",
    credentials: "include",
  });

  if (!response.ok) {
    throw new Error("Failed to load ideas");
  }

  return response.json();
}
