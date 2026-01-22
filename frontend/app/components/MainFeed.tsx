"use client";

import { useMemo, useState, useEffect } from "react";
import IdeaCard from "./IdeaCard";
import { Idea, Category, Technology } from "@/lib/types";
import { fetchIdeasPage } from "@/lib/client/ideas";

interface MainFeedProps {
  onPostIdea: () => void;
  onIdeasChange?: (ideas: Idea[]) => void;
  searchQuery?: string;
  activeCategory?: string | null;
  activeTechnology?: string | null;
  onCategorySelect?: (value: string) => void;
  onTechnologySelect?: (value: string) => void;
  onClearFilters?: () => void;
  isLoggedIn: boolean;
  sessionReady: boolean;
}

type SortOption =
  | "newest"
  | "most-liked"
  | "highest-rated"
  | "most-discussed";

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

    const normalizedTechnologies = (idea.technologies ?? []).map((tech, idx) =>
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

function normalizeLabel(item: Category | Technology | string): string {
  return typeof item === "string" ? item : item.name;
}

function includesLabel(
  items: Array<Category | Technology | string> | undefined,
  match: string
): boolean {
  if (!items || items.length === 0) return false;
  const lowered = match.toLowerCase();
  return items.some((item) => normalizeLabel(item).toLowerCase() === lowered);
}

function matchesQuery(idea: IdeaWithLike, query: string): boolean {
  if (!query) return true;
  const lowered = query.toLowerCase();
  return [
    idea.projectName,
    idea.shortDescription,
    idea.problemDescription,
    idea.solution,
    idea.technicalDetails,
    idea.createdBy,
  ].some((field) => field?.toLowerCase().includes(lowered));
}

export default function MainFeed({
  onPostIdea,
  onIdeasChange,
  searchQuery = "",
  activeCategory,
  activeTechnology,
  onCategorySelect,
  onTechnologySelect,
  onClearFilters,
  isLoggedIn,
  sessionReady,
}: MainFeedProps) {
  const [ideas, setIdeas] = useState<IdeaWithLike[]>([]);
  const [sortBy, setSortBy] = useState<SortOption>("most-liked");
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [cursor, setCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(false);

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

  useEffect(() => {
    onIdeasChange?.(ideas);
  }, [ideas, onIdeasChange]);

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
    onCategorySelect?.(category);
  };

  const handleTechnologyClick = (technology: string) => {
    onTechnologySelect?.(technology);
  };

  const hasActiveFilters = Boolean(
    searchQuery.trim() || activeCategory || activeTechnology
  );

  const filteredIdeas = useMemo(() => {
    const trimmedQuery = searchQuery.trim();
    return ideas.filter((idea) => {
      if (!matchesQuery(idea, trimmedQuery)) return false;
      if (activeCategory && !includesLabel(idea.categories, activeCategory)) {
        return false;
      }
      if (
        activeTechnology &&
        !includesLabel(idea.technologies, activeTechnology)
      ) {
        return false;
      }
      return true;
    });
  }, [ideas, searchQuery, activeCategory, activeTechnology]);

  const sortedIdeas = useMemo(() => {
    return [...filteredIdeas].sort((a, b) => {
      switch (sortBy) {
        case "most-liked":
          return b.likes - a.likes;
        case "highest-rated":
          return b.rating - a.rating;
        case "most-discussed":
          return (b.comments?.length ?? 0) - (a.comments?.length ?? 0);
        case "newest":
          return (b.id ?? 0) - (a.id ?? 0);
        default:
          return 0;
      }
    });
  }, [filteredIdeas, sortBy]);

  if (loading) {
    return (
      <main className="flex-1 w-full">
        <div className="space-y-6">
          {Array.from({ length: 4 }).map((_, index) => (
            <SkeletonCard key={index} />
          ))}
        </div>
      </main>
    );
  }

  if (!isLoggedIn && sessionReady) {
    return (
      <main className="flex-1 w-full">
        <div className="glass-panel rounded-2xl p-6 text-center text-secondary">
          Sign in to explore the transformed project archive.
        </div>
      </main>
    );
  }

  if (ideas.length === 0) {
    return (
      <main className="flex-1 w-full">
        <div className="glass-panel rounded-2xl p-6 text-center text-secondary">
          No transformed projects yet. Submit the first one to kick off the
          archive.
        </div>
      </main>
    );
  }

  return (
    <main className="flex-1 w-full">
      <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-secondary">
            Transformation feed
          </p>
          <h2 className="font-display text-2xl text-foreground">
            Latest projects
          </h2>
          <p className="text-sm text-secondary">
            Showing {sortedIdeas.length} of {ideas.length} transformed projects.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <SortDropdown value={sortBy} onChange={setSortBy} />
          <button
            onClick={onPostIdea}
            className="px-4 py-2 bg-primary text-white rounded-full hover:bg-primary/90 transition-colors font-medium">
            Submit
          </button>
        </div>
      </div>

      {sortedIdeas.length === 0 && hasActiveFilters && (
        <div className="glass-panel rounded-2xl p-6 text-center text-secondary mb-6">
          No projects match these filters.
          {onClearFilters && (
            <div className="mt-3">
              <button
                onClick={onClearFilters}
                className="text-sm text-primary hover:text-primary/80">
                Clear filters
              </button>
            </div>
          )}
        </div>
      )}

      <div className="space-y-6">
        {sortedIdeas.map((idea, index) => (
          <IdeaCard
            key={idea.id ?? `${idea.projectName}-${index}`}
            idea={idea}
            onLike={handleLike}
            onComment={handleComment}
            onShare={handleShare}
            onCategoryClick={handleCategoryClick}
            onTechnologyClick={handleTechnologyClick}
            className="animate-rise"
            style={{ animationDelay: `${Math.min(index, 6) * 90}ms` }}
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
            className="px-6 py-3 border border-border rounded-full hover:bg-muted transition-colors text-secondary hover:text-foreground disabled:opacity-60 disabled:cursor-not-allowed">
            {loadingMore ? "Loading..." : "Load more projects"}
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
    { value: "most-liked", label: "Most liked" },
    { value: "highest-rated", label: "Highest rated" },
    { value: "most-discussed", label: "Most discussed" },
    { value: "newest", label: "Newest" },
  ];

  const currentOption = options.find((opt) => opt.value === value);

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center space-x-2 px-3 py-2 text-sm border border-border rounded-full hover:bg-muted transition-colors">
        <span>{currentOption?.label}</span>
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
        <div className="absolute top-full right-0 mt-2 w-52 bg-card border border-border rounded-2xl shadow-lg z-10">
          {options.map((option) => (
            <button
              key={option.value}
              onClick={() => {
                onChange(option.value);
                setIsOpen(false);
              }}
              className={`w-full text-left px-4 py-2 text-sm transition-colors first:rounded-t-2xl last:rounded-b-2xl ${
                value === option.value
                  ? "bg-primary/15 text-primary"
                  : "text-foreground hover:bg-muted"
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
    <div className="glass-panel rounded-2xl p-5 animate-pulse">
      <div className="flex items-center justify-between mb-4">
        <div>
          <div className="h-4 bg-muted rounded w-32 mb-2" />
          <div className="h-3 bg-muted rounded w-20" />
        </div>
        <div className="h-8 w-12 bg-muted rounded" />
      </div>
      <div className="h-5 bg-muted rounded w-2/3 mb-4" />
      <div className="grid gap-3 sm:grid-cols-3 mb-4">
        <div className="h-16 bg-muted rounded-xl" />
        <div className="h-16 bg-muted rounded-xl" />
        <div className="h-16 bg-muted rounded-xl" />
      </div>
      <div className="flex items-center justify-between border-t border-border/60 pt-4">
        <div className="flex gap-3">
          <div className="h-4 bg-muted rounded w-10" />
          <div className="h-4 bg-muted rounded w-10" />
        </div>
        <div className="h-4 bg-muted rounded w-16" />
      </div>
    </div>
  );
}
