"use client";

import { useMemo, useState, useEffect } from "react";
import IdeaCard from "./IdeaCard";
import { Idea, Category, Technology } from "@/lib/types";
import { fetchIdeasPage } from "@/lib/client/ideas";
import useAuth from "@/lib/hooks/useAuth";

interface MainFeedProps {
  onPostIdea: () => void;
}

type SortOption = "popular" | "recent" | "trending" | "most-liked";

type IdeaWithLike = Omit<Idea, "categories" | "technologies"> & {
  categories: Category[] | string[];
  technologies: Technology[] | string[];
  isLiked?: boolean;
};

const PAGE_SIZE = 10;

function normalizeIdeas(ideas: Idea[]): IdeaWithLike[] {
  return ideas.map((idea) => {
    const normalizedCategories = idea.categories.map((cat, idx) =>
      typeof cat === "string" ? { id: idx, name: cat } : cat
    );

    const normalizedTechnologies = idea.technologies?.map((tech, idx) =>
      typeof tech === "string" ? { id: idx, name: tech } : tech
    );

    return {
      ...idea,
      categories: normalizedCategories as Category[] | string[],
      technologies: normalizedTechnologies as Technology[] | string[],
      isLiked: false,
    } as IdeaWithLike;
  });
}

function mergeIdeas(
  existing: IdeaWithLike[],
  incoming: IdeaWithLike[]
): IdeaWithLike[] {
  const seen = new Set<number>();
  for (const idea of existing) {
    if (idea.id != null) seen.add(idea.id);
  }

  const merged = [...existing];
  for (const idea of incoming) {
    if (idea.id != null && seen.has(idea.id)) continue;
    if (idea.id != null) seen.add(idea.id);
    merged.push(idea);
  }

  return merged;
}

export default function MainFeed({ onPostIdea }: MainFeedProps) {
  const [ideas, setIdeas] = useState<IdeaWithLike[]>([]);
  const [sortBy, setSortBy] = useState<SortOption>("popular");
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [cursor, setCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(false);
  const { isLoggedIn, sessionReady } = useAuth();

  useEffect(() => {
    let isActive = true;

    async function fetchIdeas() {
      setLoading(true);
      setLoadingMore(false);
      try {
        const page = await fetchIdeasPage(null, PAGE_SIZE);
        if (!isActive) return;
        const normalized = normalizeIdeas(page.items);
        setIdeas(normalized);
        setCursor(page.nextCursor);
        setHasMore(Boolean(page.nextCursor));
      } catch (err) {
        console.error("Failed to fetch ideas", err);
      } finally {
        if (isActive) setLoading(false);
      }
    }

    if (!sessionReady) {
      return () => {
        isActive = false;
      };
    }

    if (!isLoggedIn) {
      setIdeas([]);
      setCursor(null);
      setHasMore(false);
      setLoading(false);
      return () => {
        isActive = false;
      };
    }

    void fetchIdeas();
    return () => {
      isActive = false;
    };
  }, [isLoggedIn, sessionReady]);

  const handleLike = (id: number) => {
    setIdeas((prevIdeas) =>
      prevIdeas.map((idea) =>
        idea.id === id
          ? {
              ...idea,
              isLiked: !idea.isLiked,
              likes: idea.isLiked ? idea.likes - 1 : idea.likes + 1,
            }
          : idea
      )
    );
  };

  const handleComment = (id: number) => {
    console.log("Comment on idea:", id);
  };

  const handleShare = (id: number) => {
    console.log("Share idea:", id);
  };

  const handleCategoryClick = (category: string) => {
    console.log("Filter by category:", category);
  };

  const sortedIdeas = useMemo(() => {
    return [...ideas].sort((a, b) => {
      switch (sortBy) {
        case "popular":
        case "most-liked":
        case "trending":
          return b.likes - a.likes;
        case "recent":
          return b.likes - a.likes;
        default:
          return 0;
      }
    });
  }, [ideas, sortBy]);

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

  if (!isLoggedIn && sessionReady) {
    return (
      <main className="flex-1 max-w-2xl mx-auto px-4 py-6">
        <div className="bg-white border border-border rounded-lg p-6 text-center text-secondary">
          Sign in to view and share hackathon ideas.
        </div>
      </main>
    );
  }

  return (
    <main className="flex-1 max-w-2xl mx-auto px-4 py-6">
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

      {hasMore && (
        <div className="flex justify-center mt-8">
          <button
            onClick={async () => {
              if (!cursor || loadingMore) return;
              setLoadingMore(true);
              try {
                const page = await fetchIdeasPage(cursor, PAGE_SIZE);
                const normalized = normalizeIdeas(page.items);
                setIdeas((prev) => mergeIdeas(prev, normalized));
                setCursor(page.nextCursor);
                setHasMore(Boolean(page.nextCursor));
              } catch (err) {
                console.error("Failed to load more ideas", err);
              } finally {
                setLoadingMore(false);
              }
            }}
            disabled={loadingMore}
            className="px-6 py-3 border border-border rounded-lg hover:bg-gray-50 transition-colors text-secondary hover:text-foreground disabled:opacity-60 disabled:cursor-not-allowed">
            {loadingMore ? "Loading..." : "Load more ideas"}
          </button>
        </div>
      )}
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
    { value: "recent", label: "Winners First" },
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
