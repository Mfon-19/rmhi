import { describe, expect, it, vi } from "vitest";
import { fetchIdeasPage } from "@/lib/client/ideas";

describe("fetchIdeasPage", () => {
  it("requests ideas with cursor and limit", async () => {
    const response = {
      ok: true,
      json: vi.fn().mockResolvedValue({ items: [], nextCursor: null }),
    };
    const fetchMock = vi.fn().mockResolvedValue(response);
    globalThis.fetch = fetchMock as typeof fetch;

    const result = await fetchIdeasPage("10", 5);
    const url = fetchMock.mock.calls[0]?.[0] as string;

    expect(url).toContain("/api/ideas?");
    expect(url).toContain("cursor=10");
    expect(url).toContain("limit=5");
    expect(result).toEqual({ items: [], nextCursor: null });
  });

  it("throws when the request fails", async () => {
    const response = { ok: false };
    const fetchMock = vi.fn().mockResolvedValue(response);
    globalThis.fetch = fetchMock as typeof fetch;

    await expect(fetchIdeasPage(null, 10)).rejects.toThrow(
      "Failed to load ideas"
    );
  });
});
