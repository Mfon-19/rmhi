"use server";

export async function registerUsername(idToken: string, username: string) {
  try {
    const result = await fetch(
      "http://localhost:8080/api/auth/register-username",
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${idToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ username }),
      }
    );

    if (!result.ok) {
      throw new Error(`Failed to register username: ${result.statusText}`);
    }

    return { success: true };
  } catch (error) {
    console.error("Error registering username:", error);
    throw error;
  }
}
