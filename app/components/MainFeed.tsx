"use client";

import { useState, useEffect } from "react";
import IdeaCard, { IdeaData } from "./IdeaCard";
import { generateMockIdeas } from "../utils/mockData";

interface MainFeedProps {
  onPostIdea: () => void;
}

type SortOption = "popular" | "recent" | "trending" | "most-liked";

export default function MainFeed({ onPostIdea }: MainFeedProps) {
  const [ideas, setIdeas] = useState<IdeaData[]>([]);
  const [sortBy, setSortBy] = useState<SortOption>("popular");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Simulate loading
    setTimeout(() => {
      setIdeas(generateMockIdeas(12));
      setLoading(false);
    }, 1000);
  }, []);

  const handleLike = (ideaId: string) => {
    setIdeas((prevIdeas) =>
      prevIdeas.map((idea) =>
        idea.id === ideaId
          ? {
              ...idea,
              isLiked: !idea.isLiked,
              stats: {
                ...idea.stats,
                likes: idea.isLiked
                  ? idea.stats.likes - 1
                  : idea.stats.likes + 1,
              },
            }
          : idea
      )
    );
  };

  const handleComment = (ideaId: string) => {
    console.log("Comment on idea:", ideaId);
    // In a real app, this would open a comment modal or navigate to the idea detail page
  };

  const handleShare = (ideaId: string) => {
    console.log("Share idea:", ideaId);
  };

  const handleCategoryClick = (category: string) => {
    console.log("Filter by category:", category);
    // In a real app, this would filter the feed by category
  };

  const sortedIdeas = [...ideas].sort((a, b) => {
    switch (sortBy) {
      case "popular":
        return b.stats.likes - a.stats.likes;
      case "recent":
        return 0; // Would sort by actual timestamp in real app
      case "trending":
        return (
          b.stats.likes + b.stats.comments - (a.stats.likes + a.stats.comments)
        );
      case "most-liked":
        return b.stats.likes - a.stats.likes;
      default:
        return 0;
    }
  });

  if (loading) {
    return (
      <main className="flex-1 max-w-2xl mx-auto px-4 py-6">
        <div className="space-y-6">
          {Array.from({ length: 5 }).map((_, index) => (
            <SkeletonCard key={index} />
          ))}
        </div>
      </main>
    );
  }

  return (
    <main className="flex-1 max-w-2xl mx-auto px-4 py-6">
      {/* Feed Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-4">
          <h1 className="text-xl font-semibold text-foreground">Feed</h1>
          <SortDropdown value={sortBy} onChange={setSortBy} />
        </div>
        <button
          onClick={onPostIdea}
          className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors font-medium">
          Post Idea
        </button>
      </div>

      {/* Ideas List */}
      <div className="space-y-6">
        {sortedIdeas.map((idea) => (
          <IdeaCard
            key={idea.id}
            idea={idea}
            onLike={handleLike}
            onComment={handleComment}
            onShare={handleShare}
            onCategoryClick={handleCategoryClick}
          />
        ))}
      </div>

      {/* Load More Button */}
      <div className="flex justify-center mt-8">
        <button
          onClick={() => {
            const newIdeas = generateMockIdeas(6);
            setIdeas((prev) => [...prev, ...newIdeas]);
          }}
          className="px-6 py-3 border border-border rounded-lg hover:bg-gray-50 transition-colors text-secondary hover:text-foreground">
          Load more ideas
        </button>
      </div>
    </main>
  );
}

function SortDropdown({
  value,
  onChange,
}: {
  value: SortOption;
  onChange: (value: SortOption) => void;
}) {
  const [isOpen, setIsOpen] = useState(false);

  const options: { value: SortOption; label: string }[] = [
    { value: "popular", label: "Popular" },
    { value: "recent", label: "Recent" },
    { value: "trending", label: "Trending" },
    { value: "most-liked", label: "Most Liked" },
  ];

  const currentOption = options.find((opt) => opt.value === value);

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center space-x-2 px-3 py-2 text-sm border border-border rounded-lg hover:bg-gray-50 transition-colors">
        <span>Sort by: {currentOption?.label}</span>
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

      {isOpen && (
        <div className="absolute top-full left-0 mt-1 w-48 bg-white border border-border rounded-lg shadow-lg z-10">
          {options.map((option) => (
            <button
              key={option.value}
              onClick={() => {
                onChange(option.value);
                setIsOpen(false);
              }}
              className={`w-full text-left px-3 py-2 text-sm hover:bg-gray-50 transition-colors first:rounded-t-lg last:rounded-b-lg ${
                value === option.value
                  ? "bg-primary/10 text-primary"
                  : "text-foreground"
              }`}>
              {option.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function SkeletonCard() {
  return (
    <div className="bg-white rounded-lg border border-border p-4 animate-pulse">
      <div className="flex items-center space-x-3 mb-3">
        <div className="w-10 h-10 bg-gray-200 rounded-full"></div>
        <div className="flex-1">
          <div className="h-4 bg-gray-200 rounded w-1/4 mb-1"></div>
          <div className="h-3 bg-gray-200 rounded w-1/6"></div>
        </div>
      </div>
      <div className="h-5 bg-gray-200 rounded w-3/4 mb-3"></div>
      <div className="space-y-2 mb-4">
        <div className="h-4 bg-gray-200 rounded"></div>
        <div className="h-4 bg-gray-200 rounded"></div>
        <div className="h-4 bg-gray-200 rounded w-5/6"></div>
      </div>
      <div className="flex space-x-2 mb-4">
        <div className="h-6 bg-gray-200 rounded-full w-16"></div>
        <div className="h-6 bg-gray-200 rounded-full w-12"></div>
        <div className="h-6 bg-gray-200 rounded-full w-20"></div>
      </div>
      <div className="flex items-center justify-between border-t border-gray-200 pt-3">
        <div className="flex space-x-4">
          <div className="h-5 bg-gray-200 rounded w-8"></div>
          <div className="h-5 bg-gray-200 rounded w-8"></div>
          <div className="h-5 bg-gray-200 rounded w-8"></div>
        </div>
        <div className="h-5 bg-gray-200 rounded w-5"></div>
      </div>
    </div>
  );
}
