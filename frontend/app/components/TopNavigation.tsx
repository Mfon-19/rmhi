"use client";

import { useState } from "react";
import { auth } from "../utils/firebase";
import useAuth from "../utils/useAuth";

export default function TopNavigation() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const { user, isLoggedIn } = useAuth();

  return (
    <nav className="fixed top-0 left-0 right-0 z-50 h-[72px] bg-white border-b border-border shadow-sm">
      <div className="flex items-center justify-between h-full px-4 max-w-7xl mx-auto">
        {/* Logo */}
        <div className="flex items-center space-x-2">
          <span className="text-2xl">🏆</span>
          <span className="font-semibold text-lg text-foreground">
            HackathonHub
          </span>
        </div>

        {/* Desktop Search Bar */}
        <div className="hidden md:flex flex-1 max-w-md mx-8">
          <div className="relative w-full">
            <input
              type="text"
              placeholder="Search hackathon projects…"
              className="w-full px-4 py-2 pl-10 bg-gray-50 border border-border rounded-full focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
            />
            <svg
              className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-secondary"
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
        </div>

        {/* Desktop Navigation Links */}
        <div className="hidden lg:flex items-center space-x-6">
          <DropdownButton label="Hackathons" />
          <DropdownButton label="Technologies" />
          <button className="px-4 py-2 bg-primary text-white rounded-full hover:bg-primary/90 transition-colors font-medium hover:cursor-pointer">
            Submit Project
          </button>

          {isLoggedIn ? (
            <div className="flex items-center space-x-3">
              <div className="w-8 h-8 bg-primary rounded-full flex items-center justify-center cursor-pointer hover:bg-primary/90 transition-colors">
                <svg
                  className="w-5 h-5 text-white"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                  />
                </svg>
              </div>
            </div>
          ) : (
            <>
              <button
                className="text-secondary hover:text-foreground transition-colors hover:cursor-pointer"
                onClick={() => (window.location.href = "/signin")}>
                Sign in
              </button>
              <button
                className="px-4 py-2 border border-border rounded-full hover:bg-gray-50 transition-colors hover:cursor-pointer"
                onClick={() => (window.location.href = "/signup")}>
                Sign up
              </button>
            </>
          )}
        </div>

        {/* Mobile Menu Button */}
        <div className="lg:hidden flex items-center space-x-2">
          <button className="p-2 bg-primary text-white rounded-full">
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

      {/* Mobile Menu */}
      {isMenuOpen && (
        <div className="lg:hidden bg-white border-t border-border shadow-lg">
          <div className="px-4 py-2">
            <input
              type="text"
              placeholder="Search hackathon projects…"
              className="w-full px-4 py-2 pl-10 bg-gray-50 border border-border rounded-full focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
            />
          </div>
          <div className="px-4 py-2 space-y-2">
            <MobileNavItem label="Hackathons" />
            <MobileNavItem label="Technologies" />
            <div className="pt-2 border-t border-border">
              {isLoggedIn ? (
                /* User profile section when logged in */
                <div className="flex items-center space-x-3 py-2">
                  <div className="w-8 h-8 bg-primary rounded-full flex items-center justify-center">
                    <svg
                      className="w-5 h-5 text-white"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24">
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                      />
                    </svg>
                  </div>
                  <span className="text-foreground">Profile</span>
                </div>
              ) : (
                /* Sign in/Sign up buttons when not logged in */
                <>
                  <button className="text-secondary hover:text-foreground block w-full text-left py-2">
                    Sign in
                  </button>
                  <button className="text-secondary hover:text-foreground block w-full text-left py-2">
                    Sign up
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </nav>
  );
}

function DropdownButton({ label }: { label: string }) {
  return (
    <button className="flex items-center space-x-1 text-secondary hover:text-foreground transition-colors">
      <span>{label}</span>
      <svg
        className="w-4 h-4"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M19 9l-7 7-7-7"
        />
      </svg>
    </button>
  );
}

function MobileNavItem({ label }: { label: string }) {
  return (
    <button className="flex items-center justify-between w-full py-2 text-secondary hover:text-foreground transition-colors">
      <span>{label}</span>
      <svg
        className="w-4 h-4"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M19 9l-7 7-7-7"
        />
      </svg>
    </button>
  );
}
