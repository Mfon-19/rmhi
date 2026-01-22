"use client";

import { useState } from "react";

interface CountItem {
  label: string;
  count: number;
}

interface LeftSidebarProps {
  categories: CountItem[];
  technologies: CountItem[];
  activeCategory: string | null;
  activeTechnology: string | null;
  onCategorySelect: (value: string | null) => void;
  onTechnologySelect: (value: string | null) => void;
  onClear: () => void;
  isLoggedIn: boolean;
  sessionReady: boolean;
}

const DEFAULT_VISIBLE = 6;

export default function LeftSidebar({
  categories,
  technologies,
  activeCategory,
  activeTechnology,
  onCategorySelect,
  onTechnologySelect,
  onClear,
  isLoggedIn,
  sessionReady,
}: LeftSidebarProps) {
  const [showAllCategories, setShowAllCategories] = useState(false);
  const [showAllTechnologies, setShowAllTechnologies] = useState(false);

  if (!sessionReady) {
    return (
      <aside className="hidden lg:block">
        <div className="glass-panel rounded-2xl p-5 animate-pulse">
          <div className="h-4 w-24 bg-muted rounded-full" />
          <div className="mt-4 space-y-3">
            <div className="h-8 bg-muted rounded-xl" />
            <div className="h-8 bg-muted rounded-xl" />
            <div className="h-8 bg-muted rounded-xl" />
          </div>
        </div>
      </aside>
    );
  }

  if (!isLoggedIn) {
    return (
      <aside className="hidden lg:block">
        <div className="glass-panel rounded-2xl p-5 text-sm text-secondary">
          Sign in to filter the transformation archive.
        </div>
      </aside>
    );
  }

  const visibleCategories = showAllCategories
    ? categories
    : categories.slice(0, DEFAULT_VISIBLE);
  const visibleTechnologies = showAllTechnologies
    ? technologies
    : technologies.slice(0, DEFAULT_VISIBLE);
  const hasActiveFilter = Boolean(activeCategory || activeTechnology);

  return (
    <aside className="hidden lg:block">
      <div className="sticky top-24 space-y-5">
        <div className="glass-panel rounded-2xl p-5 space-y-5">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs uppercase tracking-[0.2em] text-secondary">
                Refine
              </p>
              <p className="text-sm font-medium text-foreground">
                Filter transformations
              </p>
            </div>
            {hasActiveFilter && (
              <button
                onClick={onClear}
                className="text-xs uppercase tracking-[0.2em] text-secondary hover:text-foreground transition-colors">
                Reset
              </button>
            )}
          </div>

          {hasActiveFilter && (
            <div className="flex flex-wrap gap-2">
              {activeCategory && (
                <ActiveTag
                  label={activeCategory}
                  onClick={() => onCategorySelect(null)}
                />
              )}
              {activeTechnology && (
                <ActiveTag
                  label={activeTechnology}
                  onClick={() => onTechnologySelect(null)}
                />
              )}
            </div>
          )}

          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-secondary mb-3">
              Categories
            </p>
            <div className="space-y-2">
              {visibleCategories.length === 0 ? (
                <p className="text-sm text-secondary">No categories yet.</p>
              ) : (
                visibleCategories.map((item) => (
                  <FilterButton
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
                ))
              )}
            </div>
            {categories.length > DEFAULT_VISIBLE && (
              <button
                onClick={() => setShowAllCategories((prev) => !prev)}
                className="mt-3 text-xs uppercase tracking-[0.2em] text-secondary hover:text-foreground transition-colors">
                {showAllCategories ? "Show fewer" : "Show more"}
              </button>
            )}
          </div>

          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-secondary mb-3">
              Technologies
            </p>
            <div className="space-y-2">
              {visibleTechnologies.length === 0 ? (
                <p className="text-sm text-secondary">No technologies yet.</p>
              ) : (
                visibleTechnologies.map((item) => (
                  <FilterButton
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
                ))
              )}
            </div>
            {technologies.length > DEFAULT_VISIBLE && (
              <button
                onClick={() => setShowAllTechnologies((prev) => !prev)}
                className="mt-3 text-xs uppercase tracking-[0.2em] text-secondary hover:text-foreground transition-colors">
                {showAllTechnologies ? "Show fewer" : "Show more"}
              </button>
            )}
          </div>
        </div>
      </div>
    </aside>
  );
}

function FilterButton({
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
      className={`flex items-center justify-between w-full px-3 py-2 rounded-xl text-sm transition-colors ${
        isActive
          ? "bg-primary/15 text-primary"
          : "text-secondary hover:text-foreground hover:bg-muted"
      }`}>
      <span>{label}</span>
      <span className="text-xs text-secondary">{count}</span>
    </button>
  );
}

function ActiveTag({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className="inline-flex items-center gap-2 rounded-full bg-primary/15 px-3 py-1 text-xs font-medium text-primary">
      <span>{label}</span>
      <span className="text-sm">x</span>
    </button>
  );
}
