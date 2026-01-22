import { onIdTokenChanged, signOut, User } from "firebase/auth";
import { useEffect, useState } from "react";
import { auth, ensureAuthPersistence } from "@/lib/firebase/client";

export default function useAuth() {
  const [user, setUser] = useState<User | null>(null);
  const [sessionReady, setSessionReady] = useState(false);

  useEffect(() => {
    let isActive = true;
    void ensureAuthPersistence();
    const unsub = onIdTokenChanged(auth, (u) => {
      setUser(u);
      setSessionReady(false);
      if (u) {
        void establishSession(u)
          .then(() => {
            if (isActive) setSessionReady(true);
          })
          .catch((error) => {
            console.error("Failed to establish session", error);
            if (isActive) setSessionReady(true);
          });
      } else {
        void clearSession().finally(() => {
          if (isActive) setSessionReady(true);
        });
      }
    });
    return () => {
      isActive = false;
      unsub();
    };
  }, []);

  return { user, isLoggedIn: !!user, sessionReady };
}

export async function establishSession(user: User) {
  const idToken = await user.getIdToken();
  const response = await fetch("/api/set-token", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ idToken }),
  });
  if (!response.ok) {
    await signOut(auth);
    throw new Error("Failed to establish session");
  }
}

export async function clearSession() {
  try {
    await fetch("/api/set-token", {
      method: "DELETE",
      credentials: "include",
    });
  } catch (error) {
    console.error("Failed to clear session", error);
  }
}
