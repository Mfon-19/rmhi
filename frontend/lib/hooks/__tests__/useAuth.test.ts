import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/lib/firebase/client", () => ({
  auth: {},
  ensureAuthPersistence: vi.fn().mockResolvedValue(undefined),
}));
vi.mock("firebase/auth", () => ({
  signOut: vi.fn(),
  onIdTokenChanged: vi.fn(),
}));

import { clearSession, establishSession } from "@/lib/hooks/useAuth";
import { signOut } from "firebase/auth";

describe("session helpers", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("establishSession posts the id token", async () => {
    const response = { ok: true };
    const fetchMock = vi.fn().mockResolvedValue(response);
    globalThis.fetch = fetchMock as typeof fetch;
    const user = {
      getIdToken: vi.fn().mockResolvedValue("token-123"),
    } as unknown as { getIdToken: () => Promise<string> };

    await establishSession(user);

    expect(fetchMock).toHaveBeenCalledWith("/api/set-token", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ idToken: "token-123" }),
    });
    expect(vi.mocked(signOut)).not.toHaveBeenCalled();
  });

  it("establishSession signs out when the session fails", async () => {
    const response = { ok: false };
    const fetchMock = vi.fn().mockResolvedValue(response);
    globalThis.fetch = fetchMock as typeof fetch;
    const user = {
      getIdToken: vi.fn().mockResolvedValue("token-123"),
    } as unknown as { getIdToken: () => Promise<string> };

    await expect(establishSession(user)).rejects.toThrow(
      "Failed to establish session"
    );
    expect(vi.mocked(signOut)).toHaveBeenCalled();
  });

  it("clearSession sends a delete request", async () => {
    const fetchMock = vi.fn().mockResolvedValue({});
    globalThis.fetch = fetchMock as typeof fetch;

    await clearSession();

    expect(fetchMock).toHaveBeenCalledWith("/api/set-token", {
      method: "DELETE",
      credentials: "include",
    });
  });

  it("clearSession swallows fetch errors", async () => {
    const fetchMock = vi.fn().mockRejectedValue(new Error("boom"));
    globalThis.fetch = fetchMock as typeof fetch;
    const consoleSpy = vi.spyOn(console, "error").mockImplementation(() => {});

    await expect(clearSession()).resolves.toBeUndefined();

    consoleSpy.mockRestore();
  });
});
