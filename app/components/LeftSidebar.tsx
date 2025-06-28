"use client";

import { useState } from "react";

interface SidebarItemProps {
  icon: React.ReactNode;
  label: string;
  isActive?: boolean;
  onClick?: () => void;
}

export default function LeftSidebar({ isOpen = true }: { isOpen?: boolean }) {
  const [showMore, setShowMore] = useState(false);

  if (!isOpen) return null;

  return (
    <aside className="fixed left-0 top-[72px] w-60 h-[calc(100vh-72px)] bg-white border-r border-border overflow-y-auto z-40">
      <div className="p-4">
        {/* GENERAL Section */}
        <div className="mb-6">
          <h3 className="text-xs font-semibold text-secondary uppercase tracking-wider mb-3">
            GENERAL
          </h3>
          <div className="space-y-1">
            <SidebarItem icon={<HomeIcon />} label="Feed" isActive={true} />
            <SidebarItem icon={<UserIcon />} label="My Ideas" />
            <SidebarItem icon={<PollIcon />} label="Polls" />
          </div>
        </div>

        {/* DISCOVER Section */}
        <div className="mb-6">
          <h3 className="text-xs font-semibold text-secondary uppercase tracking-wider mb-3">
            DISCOVER
          </h3>
          <div className="space-y-1">
            <SidebarItem icon={<CategoryIcon />} label="Categories" />
            <SidebarItem icon={<TrendingIcon />} label="Trending" />

            {showMore && (
              <>
                <SidebarItem icon={<StarIcon />} label="Most Liked" />
                <SidebarItem icon={<ClockIcon />} label="Recent" />
                <SidebarItem icon={<FireIcon />} label="Hot" />
              </>
            )}

            <button
              onClick={() => setShowMore(!showMore)}
              className="flex items-center space-x-3 w-full px-3 py-2 text-sm text-secondary hover:text-foreground hover:bg-gray-50 rounded-lg transition-colors">
              <svg
                className="w-4 h-4"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d={showMore ? "M5 15l7-7 7 7" : "M19 9l-7 7-7-7"}
                />
              </svg>
              <span>{showMore ? "Show less" : "Show more"}</span>
            </button>
          </div>
        </div>

        {/* Quick Stats */}
        <div className="mt-8 p-3 bg-gray-50 rounded-lg">
          <h4 className="text-sm font-medium text-foreground mb-2">
            Community Stats
          </h4>
          <div className="space-y-1 text-xs text-secondary">
            <div className="flex justify-between">
              <span>Ideas posted today</span>
              <span>127</span>
            </div>
            <div className="flex justify-between">
              <span>Active members</span>
              <span>2.4k</span>
            </div>
          </div>
        </div>
      </div>
    </aside>
  );
}

function SidebarItem({
  icon,
  label,
  isActive = false,
  onClick,
}: SidebarItemProps) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center space-x-3 w-full px-3 py-2 text-sm rounded-lg transition-colors ${
        isActive
          ? "bg-primary/10 text-primary font-medium"
          : "text-secondary hover:text-foreground hover:bg-gray-50"
      }`}>
      <span className={isActive ? "text-primary" : "text-secondary"}>
        {icon}
      </span>
      <span>{label}</span>
    </button>
  );
}

// Icon Components
function HomeIcon() {
  return (
    <svg
      className="w-4 h-4"
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
      />
    </svg>
  );
}

function UserIcon() {
  return (
    <svg
      className="w-4 h-4"
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
  );
}

function PollIcon() {
  return (
    <svg
      className="w-4 h-4"
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
      />
    </svg>
  );
}

function CategoryIcon() {
  return (
    <svg
      className="w-4 h-4"
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"
      />
    </svg>
  );
}

function TrendingIcon() {
  return (
    <svg
      className="w-4 h-4"
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"
      />
    </svg>
  );
}

function StarIcon() {
  return (
    <svg
      className="w-4 h-4"
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"
      />
    </svg>
  );
}

function ClockIcon() {
  return (
    <svg
      className="w-4 h-4"
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
      />
    </svg>
  );
}

function FireIcon() {
  return (
    <svg
      className="w-4 h-4"
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z"
      />
    </svg>
  );
}
