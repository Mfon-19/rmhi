export async function registerUsername(idToken: string, username: string) {
  const response = await fetch("/api/register-username", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ idToken, username }),
  });

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    const message =
      body?.message || `Failed to register username (${response.status})`;
    throw new Error(message);
  }

  return response.json().catch(() => ({ success: true }));
}
