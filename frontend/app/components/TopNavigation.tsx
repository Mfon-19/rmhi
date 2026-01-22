"use client";

import { useState } from "react";

interface TopNavigationProps {
  searchValue: string;
  onSearchChange: (value: string) => void;
  onPostIdea: () => void;
  isLoggedIn: boolean;
  userInitial?: string;
  userLabel?: string;
}

export default function TopNavigation({
  searchValue,
  onSearchChange,
  onPostIdea,
  isLoggedIn,
  userInitial,
  userLabel,
}: TopNavigationProps) {
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  return (
    <nav className="fixed top-0 left-0 right-0 z-50 border-b border-border/70 bg-card/80 backdrop-blur">
      <div className="flex items-center justify-between h-[72px] px-4 max-w-6xl mx-auto">
        <div className="flex items-center gap-3">
          <div className="h-10 w-10 rounded-2xl bg-primary text-white flex items-center justify-center font-display text-lg">
            E
          </div>
          <div className="leading-tight">
            <p className="font-display text-lg text-foreground">Eureka</p>
            <p className="text-[10px] uppercase tracking-[0.3em] text-secondary">
              Transformation lab
            </p>
          </div>
        </div>

        <div className="hidden md:flex flex-1 max-w-lg mx-6">
          <SearchInput value={searchValue} onChange={onSearchChange} />
        </div>

        <div className="hidden lg:flex items-center gap-3">
          <button
            onClick={onPostIdea}
            className="px-4 py-2 rounded-full bg-primary text-white font-medium shadow-sm hover:bg-primary/90 transition-colors">
            Submit project
          </button>

          {isLoggedIn ? (
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-full bg-muted text-foreground flex items-center justify-center font-semibold">
                {userInitial || "U"}
              </div>
              <span className="text-sm text-secondary">
                {userLabel || "Profile"}
              </span>
            </div>
          ) : (
            <>
              <button
                className="text-secondary hover:text-foreground transition-colors"
                onClick={() => (window.location.href = "/signin")}>
                Sign in
              </button>
              <button
                className="px-4 py-2 border border-border rounded-full hover:bg-muted transition-colors"
                onClick={() => (window.location.href = "/signup")}>
                Sign up
              </button>
            </>
          )}
        </div>

        <div className="lg:hidden flex items-center gap-2">
          <button
            onClick={onPostIdea}
            className="p-2 bg-primary text-white rounded-full">
            <svg
              className="w-5 h-5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 4v16m8-8H4"
              />
            </svg>
          </button>
          <button
            onClick={() => setIsMenuOpen(!isMenuOpen)}
            className="p-2 text-secondary hover:text-foreground">
            <svg
              className="w-6 h-6"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d={
                  isMenuOpen
                    ? "M6 18L18 6M6 6l12 12"
                    : "M4 6h16M4 12h16M4 18h16"
                }
              />
            </svg>
          </button>
        </div>
      </div>

      {isMenuOpen && (
        <div className="lg:hidden border-t border-border/70 bg-card/95 shadow-lg">
          <div className="px-4 py-3">
            <SearchInput value={searchValue} onChange={onSearchChange} />
          </div>
          <div className="px-4 py-3 space-y-3 text-sm">
            {isLoggedIn ? (
              <div className="flex items-center gap-3 py-2">
                <div className="h-10 w-10 rounded-full bg-muted text-foreground flex items-center justify-center font-semibold">
                  {userInitial || "U"}
                </div>
                <div>
                  <p className="text-foreground">
                    {userLabel || "Profile"}
                  </p>
                  <p className="text-xs text-secondary">Signed in</p>
                </div>
              </div>
            ) : (
              <div className="flex flex-col gap-2">
                <button
                  className="text-secondary hover:text-foreground text-left"
                  onClick={() => (window.location.href = "/signin")}>
                  Sign in
                </button>
                <button
                  className="text-secondary hover:text-foreground text-left"
                  onClick={() => (window.location.href = "/signup")}>
                  Sign up
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </nav>
  );
}

function SearchInput({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <div className="relative w-full">
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Search transformed projects"
        aria-label="Search transformed projects"
        className="w-full px-4 py-2.5 pl-11 bg-muted border border-border rounded-full focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-transparent"
      />
      <svg
        className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-secondary"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
        />
      </svg>
    </div>
  );
}
