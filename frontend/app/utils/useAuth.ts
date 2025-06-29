import { onAuthStateChanged, User } from "firebase/auth";
import { useEffect, useState } from "react";
import { auth } from "./firebase";

export default function useAuth() {
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    const unsub = onAuthStateChanged(auth, (u) => {
      setUser(u);
      establishSession();
    });
    return unsub;
  }, []);

  return { user, isLoggedIn: !!user };
}

export async function establishSession() {
  const idToken = await auth.currentUser!.getIdToken(true);

  await fetch("/api/set-token", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ idToken }),
  });
}
