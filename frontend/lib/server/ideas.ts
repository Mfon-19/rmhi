"use server";

import "server-only";

import { cookies } from "next/headers";
import { Idea } from "@/lib/types";
import { adminAuth } from "@/lib/firebase/admin";
import fs from "fs/promises";
import path from "path";

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

async function getToken() {
  const cookieStore = await cookies();
  const token = cookieStore.get("idToken")?.value;

  if (!token) throw new Error("Unauthenticated");

  try {
    await adminAuth.verifyIdToken(token, true);
  } catch {
    throw new Error("Invalid or expired ID token");
  }
  console.log("token acquired successfully...");
  return token;
}

export async function createIdea(idea: Idea) {
  const token = await getToken();
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
      throw new Error("Failed to create scraped idea");
    }
    const result = await response.json();
    return result.id;
  } catch (error) {
    console.error("Failed to create scraped idea");
  }
}

export async function getIdeas(cursor = 0, limit = 10) {
  const token = await getToken();
  const params = new URLSearchParams();
  params.set("cursor", String(cursor));
  params.set("limit", String(limit));
  const url = `${API_URL}/get-transformed-ideas?${params.toString()}`;
  const response = await fetch(url, {
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
  });

  if (response.status !== 200) {
    throw new Error("Failed to get ideas");
  }
  const result = await response.json();
  return result;
}

export async function importScrapedIdeas() {
  try {
    const ideasPath = path.resolve(process.cwd(), "ideas.json");
    const raw = JSON.parse(await fs.readFile(ideasPath, "utf-8")) as {
      ideas: any[];
    };

    const ideasData = raw.ideas;

    const ideas: Idea[] = ideasData.map((item) => ({
      projectName: item.project_name,
      likes: item.likes ?? 0,
      categories: item.categories ?? [],
      rating: item.rating ?? 0,
      createdBy: "anonymous",
      technologies: item.technologies ?? [],
      shortDescription: item.short_description,
      solution: item.solution,
      problemDescription: item.problem_description,
      technicalDetails: item.technical_details,
    }));

    console.log(`Starting import of ${ideas.length} scraped ideas`);

    let success = 0;
    let failed = 0;
    const errors: string[] = [];

    for (const idea of ideas) {
      try {
        await createIdea(idea);
        success += 1;
        console.log(`✓ Imported: ${idea.projectName}`);
      } catch (err) {
        failed += 1;
        const errorMsg = `Failed to import "${idea.projectName}": ${err}`;
        console.error(`✗ ${errorMsg}`);
        errors.push(errorMsg);
      }
    }

    const result = {
      total: ideas.length,
      success,
      failed,
      errors: errors.slice(0, 10),
    };

    console.log(`Import completed: ${success}/${ideas.length} successful`);
    return result;
  } catch (error) {
    console.error("Failed to import scraped ideas:", error);
    throw new Error(`Import failed: ${error}`);
  }
}
