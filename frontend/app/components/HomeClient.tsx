"use client";

import dynamic from "next/dynamic";
import { useCallback, useMemo, useState } from "react";
import LeftSidebar from "./LeftSidebar";
import MainFeed from "./MainFeed";
import RightSidebar from "./RightSidebar";
import TopNavigation from "./TopNavigation";
import useAuth from "@/lib/hooks/useAuth";
import { Category, Idea, Technology } from "@/lib/types";

const PostIdeaModal = dynamic(() => import("./PostIdeaModal"), {
  ssr: false,
});

type CountItem = {
  label: string;
  count: number;
};

type SidebarStats = {
  projectCount: number;
  contributorCount: number;
  averageLikes: number;
  coveragePercent: number;
};

type Insights = {
  categories: CountItem[];
  technologies: CountItem[];
  spotlight: Idea | null;
  topIdeas: Idea[];
  stats: SidebarStats;
};

const MAX_FILTER_ITEMS = 8;

function normalizeLabel(item: Category | Technology | string): string {
  if (typeof item === "string") return item;
  return item.name;
}

function collectCounts(
  ideas: Idea[],
  picker: (idea: Idea) => Array<Category | Technology | string> | undefined
): CountItem[] {
  const counts = new Map<string, CountItem>();

  for (const idea of ideas) {
    const items = picker(idea) ?? [];
    for (const item of items) {
      const label = normalizeLabel(item).trim();
      if (!label) continue;
      const key = label.toLowerCase();
      const existing = counts.get(key);
      if (existing) {
        existing.count += 1;
      } else {
        counts.set(key, { label, count: 1 });
      }
    }
  }

  return Array.from(counts.values()).sort((a, b) => {
    if (b.count !== a.count) return b.count - a.count;
    return a.label.localeCompare(b.label);
  });
}

function countCoverage(idea: Idea): number {
  let total = 0;
  if (idea.problemDescription?.trim()) total += 1;
  if (idea.solution?.trim()) total += 1;
  if (idea.technicalDetails?.trim()) total += 1;
  return total;
}

export default function HomeClient() {
  const [isPostModalOpen, setIsPostModalOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [activeCategory, setActiveCategory] = useState<string | null>(null);
  const [activeTechnology, setActiveTechnology] = useState<string | null>(null);
  const [ideasSnapshot, setIdeasSnapshot] = useState<Idea[]>([]);
  const { user, isLoggedIn, sessionReady } = useAuth();

  const handlePostIdea = () => {
    setIsPostModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsPostModalOpen(false);
  };

  const handleIdeasChange = useCallback((ideas: Idea[]) => {
    setIdeasSnapshot(ideas);
  }, []);

  const insights: Insights = useMemo(() => {
    const categories = collectCounts(ideasSnapshot, (idea) => idea.categories);
    const technologies = collectCounts(ideasSnapshot, (idea) => idea.technologies);
    const rankedIdeas = [...ideasSnapshot].sort((a, b) => b.likes - a.likes);
    const spotlight = rankedIdeas[0] ?? null;
    const topIdeas = rankedIdeas.slice(0, 3);
    const projectCount = ideasSnapshot.length;
    const contributorCount = new Set(
      ideasSnapshot.map((idea) => idea.createdBy).filter(Boolean)
    ).size;
    const averageLikes = projectCount
      ? Math.round(
          ideasSnapshot.reduce((sum, idea) => sum + (idea.likes ?? 0), 0) /
            projectCount
        )
      : 0;
    const coverageTotal = ideasSnapshot.reduce(
      (sum, idea) => sum + countCoverage(idea),
      0
    );
    const coveragePercent = projectCount
      ? Math.round((coverageTotal / (projectCount * 3)) * 100)
      : 0;

    return {
      categories,
      technologies,
      spotlight,
      topIdeas,
      stats: {
        projectCount,
        contributorCount,
        averageLikes,
        coveragePercent,
      },
    };
  }, [ideasSnapshot]);

  const userLabel = user?.displayName || user?.email || "";
  const userInitial = userLabel ? userLabel.charAt(0).toUpperCase() : "";

  const primaryCategory = insights.categories[0]?.label;
  const primaryTechnology = insights.technologies[0]?.label;

  return (
    <div className="min-h-screen text-foreground">
      <TopNavigation
        searchValue={searchQuery}
        onSearchChange={setSearchQuery}
        onPostIdea={handlePostIdea}
        isLoggedIn={isLoggedIn}
        userInitial={userInitial}
        userLabel={userLabel}
      />

      <header className="relative overflow-hidden pt-24 pb-12">
        <div
          className="pointer-events-none absolute inset-0 -z-10"
          aria-hidden="true">
          <div className="absolute -top-24 right-8 h-52 w-52 rounded-full bg-[radial-gradient(circle_at_center,rgba(216,106,58,0.35),transparent_70%)]" />
          <div className="absolute bottom-6 left-8 h-40 w-40 rounded-full bg-[radial-gradient(circle_at_center,rgba(47,111,109,0.35),transparent_70%)]" />
        </div>

        <div className="mx-auto max-w-6xl px-4">
          <div className="grid gap-10 lg:grid-cols-[1.1fr_0.9fr] items-end">
            <div className="space-y-6 animate-rise">
              <div className="inline-flex items-center gap-2 rounded-full bg-card/80 px-4 py-2 text-[11px] uppercase tracking-[0.3em] text-secondary">
                Transformed projects
              </div>
              <div className="space-y-4">
                <h1 className="font-display text-4xl md:text-5xl leading-tight text-foreground">
                  Hackathon ideas, clarified into shippable blueprints.
                </h1>
                <p className="text-base text-secondary max-w-xl">
                  Eureka gathers raw hackathon submissions and restructures them
                  into clear problem, solution, and build paths. Scan the
                  transformations, spot the strongest patterns, and build faster.
                </p>
              </div>
              <div className="flex flex-wrap gap-3">
                <button
                  onClick={handlePostIdea}
                  className="px-5 py-2.5 rounded-full bg-primary text-white font-medium shadow-sm hover:bg-primary/90 transition-colors">
                  Submit a project
                </button>
                <a
                  href="#project-feed"
                  className="px-5 py-2.5 rounded-full border border-border text-foreground hover:bg-muted transition-colors">
                  Browse transformations
                </a>
              </div>
              {!isLoggedIn && sessionReady && (
                <div className="glass-panel rounded-2xl px-4 py-3 text-sm text-secondary">
                  Sign in to view the full project archive and save your
                  favorites.
                </div>
              )}
            </div>

            <div
              className="glass-panel rounded-3xl p-6 space-y-5 animate-rise"
              style={{ animationDelay: "120ms" }}>
              <div>
                <p className="text-xs uppercase tracking-[0.3em] text-secondary">
                  Snapshot
                </p>
                <h2 className="font-display text-2xl text-foreground">
                  Live transformation index
                </h2>
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <MetricCard
                  label="Projects loaded"
                  value={insights.stats.projectCount}
                />
                <MetricCard
                  label="Contributors"
                  value={insights.stats.contributorCount}
                />
                <MetricCard
                  label="Avg likes"
                  value={insights.stats.averageLikes}
                />
                <MetricCard
                  label="Coverage"
                  value={`${insights.stats.coveragePercent}%`}
                />
              </div>
              <div className="rounded-2xl border border-border/70 bg-card/70 p-4 text-sm text-secondary">
                {primaryCategory || primaryTechnology ? (
                  <p>
                    Most represented focus: {primaryCategory || ""}
                    {primaryCategory && primaryTechnology ? " and " : ""}
                    {primaryTechnology || ""}.
                  </p>
                ) : (
                  <p>Waiting for transformed projects to populate the feed.</p>
                )}
              </div>
            </div>
          </div>
        </div>
      </header>

      <section id="project-feed" className="mx-auto max-w-6xl px-4 pb-16">
        <div className="grid gap-6 lg:grid-cols-[240px_minmax(0,1fr)] xl:grid-cols-[240px_minmax(0,1fr)_280px]">
          <LeftSidebar
            categories={insights.categories.slice(0, MAX_FILTER_ITEMS)}
            technologies={insights.technologies.slice(0, MAX_FILTER_ITEMS)}
            activeCategory={activeCategory}
            activeTechnology={activeTechnology}
            onCategorySelect={setActiveCategory}
            onTechnologySelect={setActiveTechnology}
            onClear={() => {
              setActiveCategory(null);
              setActiveTechnology(null);
            }}
            isLoggedIn={isLoggedIn}
            sessionReady={sessionReady}
          />

          <div className="flex flex-col gap-6">
            <MobileFilters
              categories={insights.categories.slice(0, MAX_FILTER_ITEMS)}
              technologies={insights.technologies.slice(0, MAX_FILTER_ITEMS)}
              activeCategory={activeCategory}
              activeTechnology={activeTechnology}
              onCategorySelect={setActiveCategory}
              onTechnologySelect={setActiveTechnology}
              onClear={() => {
                setActiveCategory(null);
                setActiveTechnology(null);
              }}
              isLoggedIn={isLoggedIn}
              sessionReady={sessionReady}
            />

            <MainFeed
              onPostIdea={handlePostIdea}
              onIdeasChange={handleIdeasChange}
              searchQuery={searchQuery}
              activeCategory={activeCategory}
              activeTechnology={activeTechnology}
              onCategorySelect={(value) => setActiveCategory(value)}
              onTechnologySelect={(value) => setActiveTechnology(value)}
              onClearFilters={() => {
                setActiveCategory(null);
                setActiveTechnology(null);
                setSearchQuery("");
              }}
              isLoggedIn={isLoggedIn}
              sessionReady={sessionReady}
            />
          </div>

          <RightSidebar
            spotlight={insights.spotlight}
            topIdeas={insights.topIdeas}
            categories={insights.categories.slice(0, 6)}
            technologies={insights.technologies.slice(0, 6)}
            stats={insights.stats}
            isLoggedIn={isLoggedIn}
            sessionReady={sessionReady}
          />
        </div>
      </section>

      <PostIdeaModal isOpen={isPostModalOpen} onClose={handleCloseModal} />

      <button
        onClick={handlePostIdea}
        className="lg:hidden fixed bottom-6 right-6 w-14 h-14 bg-primary text-white rounded-full shadow-lg hover:bg-primary/90 transition-colors z-40 flex items-center justify-center">
        <svg
          className="w-6 h-6"
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
    </div>
  );
}

function MetricCard({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="rounded-2xl border border-border/70 bg-card/70 p-4">
      <p className="text-xs uppercase tracking-[0.24em] text-secondary">
        {label}
      </p>
      <p className="mt-2 text-2xl font-semibold text-foreground">{value}</p>
    </div>
  );
}

function MobileFilters({
  categories,
  technologies,
  activeCategory,
  activeTechnology,
  onCategorySelect,
  onTechnologySelect,
  onClear,
  isLoggedIn,
  sessionReady,
}: {
  categories: CountItem[];
  technologies: CountItem[];
  activeCategory: string | null;
  activeTechnology: string | null;
  onCategorySelect: (value: string | null) => void;
  onTechnologySelect: (value: string | null) => void;
  onClear: () => void;
  isLoggedIn: boolean;
  sessionReady: boolean;
}) {
  if (!sessionReady) {
    return (
      <div className="glass-panel rounded-2xl p-4 lg:hidden animate-pulse">
        <div className="h-4 w-24 bg-muted rounded-full" />
        <div className="mt-4 space-y-3">
          <div className="h-8 bg-muted rounded-xl" />
          <div className="h-8 bg-muted rounded-xl" />
        </div>
      </div>
    );
  }

  if (!isLoggedIn) {
    return (
      <div className="glass-panel rounded-2xl p-4 lg:hidden text-sm text-secondary">
        Sign in to refine the transformation feed on mobile.
      </div>
    );
  }

  return (
    <div className="glass-panel rounded-2xl p-4 lg:hidden">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-secondary">
            Filters
          </p>
          <p className="text-sm font-medium text-foreground">
            Refine the transformation feed
          </p>
        </div>
        <button
          onClick={onClear}
          className="text-xs uppercase tracking-[0.2em] text-secondary hover:text-foreground transition-colors">
          Clear
        </button>
      </div>

      <div className="mt-4 space-y-4">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-secondary mb-2">
            Categories
          </p>
          <div className="flex flex-wrap gap-2">
            {categories.map((item) => (
              <FilterChip
                key={item.label}
                label={item.label}
                count={item.count}
                isActive={activeCategory === item.label}
                onClick={() =>
                  onCategorySelect(
                    activeCategory === item.label ? null : item.label
                  )
                }
              />
            ))}
          </div>
        </div>

        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-secondary mb-2">
            Technologies
          </p>
          <div className="flex flex-wrap gap-2">
            {technologies.map((item) => (
              <FilterChip
                key={item.label}
                label={item.label}
                count={item.count}
                isActive={activeTechnology === item.label}
                onClick={() =>
                  onTechnologySelect(
                    activeTechnology === item.label ? null : item.label
                  )
                }
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

function FilterChip({
  label,
  count,
  isActive,
  onClick,
}: {
  label: string;
  count: number;
  isActive: boolean;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className={`inline-flex items-center gap-2 rounded-full px-3 py-1 text-xs font-medium transition-colors ${
        isActive
          ? "bg-primary/15 text-primary"
          : "bg-muted text-secondary hover:text-foreground"
      }`}>
      <span>{label}</span>
      <span className="text-[10px]">{count}</span>
    </button>
  );
}
