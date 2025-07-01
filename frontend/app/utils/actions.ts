"use server";

import { cookies } from "next/headers";
import { Idea } from "./types";
import { adminAuth } from "./firebase-admin";

const API_URL = process.env.API_URL;

export async function registerUsername(idToken: string, username: string) {
  try {
    const result = await fetch(`${API_URL}/register-username`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${idToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ username }),
    });

    if (!result.ok) {
      throw new Error(`Failed to register username: ${result.statusText}`);
    }

    return { success: true };
  } catch (error) {
    console.error("Error registering username:", error);
    throw error;
  }
}

export async function createIdea(idea: Idea) {
  const cookieStore = await cookies();
  const token = cookieStore.get("idToken")?.value;

  if (!token) throw new Error("Unauthenticated");

  let decoded;
  try {
    decoded = await adminAuth.verifyIdToken(token, true);
  } catch {
    throw new Error("Invalid or expired ID token");
  }

  try {
    const response = await fetch(`${API_URL}/create-idea`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ idea: idea }),
    });

    if (response.status !== 200) {
      throw new Error("Failed to create idea");
    }
    const result = await response.json();
    return { ideaId: result.id };
  } catch (error) {
    console.error("Failed to create idea");
  }
}
