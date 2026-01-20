"use client";

import { Suspense, useEffect, useState } from "react";
import { onAuthStateChanged } from "firebase/auth";
import { auth } from "@/lib/firebase/client";
import { registerUsername } from "@/lib/server/ideas";
import { establishSession } from "@/lib/hooks/useAuth";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { toast } from "sonner";

function CompleteProfileContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const suggestedUsername = searchParams.get("username") ?? "";
  const reason = searchParams.get("reason");
  const [username, setUsername] = useState(() => suggestedUsername);
  const [loading, setLoading] = useState(false);
  const [checkingAuth, setCheckingAuth] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!username && suggestedUsername) {
      setUsername(suggestedUsername);
    }
  }, [suggestedUsername, username]);

  useEffect(() => {
    const unsub = onAuthStateChanged(auth, (user) => {
      if (!user) {
        router.replace("/signup");
        return;
      }
      setCheckingAuth(false);
    });
    return unsub;
  }, [router]);

  const validateUsername = () => {
    const normalized = username.trim();
    if (!normalized) {
      return "Username is required";
    }

    if (normalized.length < 3) {
      return "Username must be at least 3 characters long";
    }

    const usernameRegex = /^[a-zA-Z0-9_]+$/;
    if (!usernameRegex.test(normalized)) {
      return "Username can only contain letters, numbers, and underscores";
    }

    return null;
  };

  const handleCompleteProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    const usernameError = validateUsername();
    if (usernameError) {
      setError(usernameError);
      setLoading(false);
      return;
    }

    const user = auth.currentUser;
    if (!user) {
      setError("Your session expired. Please sign in again.");
      setLoading(false);
      return;
    }

    try {
      const idToken = await user.getIdToken();
      await registerUsername(idToken, username.trim());
      await establishSession(user);
      router.push("/");
    } catch (err) {
      const message = err instanceof Error ? err.message : "";
      if (message.includes("Conflict")) {
        setError("That username is already taken.");
      } else {
        setError("Failed to register username. Please try again.");
      }
      toast.error("Failed to complete profile");
    } finally {
      setLoading(false);
    }
  };

  if (checkingAuth) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center px-4">
        <div className="max-w-md w-full space-y-4">
          <div className="bg-card rounded-2xl shadow-lg p-8 border border-border text-center text-secondary">
            Loading your profile...
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background flex items-center justify-center px-4">
      <div className="max-w-md w-full space-y-8">
        <div className="text-center">
          <h1 className="text-4xl font-bold text-primary mb-2">Eureka</h1>
          <h2 className="text-2xl font-semibold text-foreground mb-2">
            Complete your profile
          </h2>
          <p className="text-secondary">
            {reason === "username"
              ? "We couldn't finish your account setup. Pick a username to continue."
              : "Pick a username to finish signing up."}
          </p>
        </div>

        <div className="bg-card rounded-2xl shadow-lg p-8 border border-border">
          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded-lg text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleCompleteProfile} className="space-y-6">
            <div>
              <label
                htmlFor="username"
                className="block text-sm font-medium text-foreground mb-2">
                Username
              </label>
              <input
                id="username"
                type="text"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full px-4 py-3 border border-border rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent transition-colors bg-background text-foreground"
                placeholder="Enter your username"
                minLength={3}
              />
              <p className="text-xs text-secondary mt-1">
                Must be at least 3 characters. Only letters, numbers, and
                underscores allowed.
              </p>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-primary text-white py-3 px-4 rounded-lg font-medium hover:bg-primary/90 focus:ring-2 focus:ring-primary focus:ring-offset-2 transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
              {loading ? "Saving..." : "Continue"}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-secondary text-sm">
              Already have an account?{" "}
              <Link
                href="/signin"
                className="text-primary hover:underline font-medium">
                Sign in
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

function CompleteProfileFallback() {
  return (
    <div className="min-h-screen bg-background flex items-center justify-center px-4">
      <div className="max-w-md w-full space-y-4">
        <div className="bg-card rounded-2xl shadow-lg p-8 border border-border text-center text-secondary">
          Loading...
        </div>
      </div>
    </div>
  );
}

export default function CompleteProfile() {
  return (
    <Suspense fallback={<CompleteProfileFallback />}>
      <CompleteProfileContent />
    </Suspense>
  );
}
