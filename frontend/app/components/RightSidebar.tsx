import CategoryChip, { getCategoryVariant } from "./CategoryChip";
import { Category, Idea, Technology } from "@/lib/types";

interface CountItem {
  label: string;
  count: number;
}

interface SidebarStats {
  projectCount: number;
  contributorCount: number;
  averageLikes: number;
  coveragePercent: number;
}

interface RightSidebarProps {
  spotlight: Idea | null;
  topIdeas: Idea[];
  categories: CountItem[];
  technologies: CountItem[];
  stats: SidebarStats;
  isLoggedIn: boolean;
  sessionReady: boolean;
}

function normalizeLabels(items: Array<Category | Technology | string> = []) {
  return items.map((item) => (typeof item === "string" ? item : item.name));
}

export default function RightSidebar({
  spotlight,
  topIdeas,
  categories,
  technologies,
  stats,
  isLoggedIn,
  sessionReady,
}: RightSidebarProps) {
  if (!sessionReady) {
    return (
      <aside className="hidden xl:block">
        <div className="glass-panel rounded-2xl p-5 animate-pulse">
          <div className="h-4 w-32 bg-muted rounded-full" />
          <div className="mt-4 space-y-3">
            <div className="h-3 bg-muted rounded-full" />
            <div className="h-3 bg-muted rounded-full" />
            <div className="h-3 bg-muted rounded-full" />
          </div>
        </div>
      </aside>
    );
  }

  if (!isLoggedIn) {
    return (
      <aside className="hidden xl:block">
        <div className="glass-panel rounded-2xl p-5 text-sm text-secondary">
          Sign in to unlock transformation insights.
        </div>
      </aside>
    );
  }

  return (
    <aside className="hidden xl:block">
      <div className="sticky top-24 space-y-6">
        <section className="glass-panel rounded-2xl p-5 space-y-4">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-secondary">
              Spotlight
            </p>
            <h3 className="font-display text-xl text-foreground">
              Top transformation
            </h3>
          </div>
          {spotlight ? (
            <div className="space-y-3">
              <div>
                <h4 className="text-lg font-semibold text-foreground">
                  {spotlight.projectName}
                </h4>
                <p className="text-sm text-secondary line-clamp-3">
                  {spotlight.shortDescription ||
                    spotlight.problemDescription ||
                    "No summary available yet."}
                </p>
              </div>
              <div className="flex flex-wrap gap-2">
                {normalizeLabels(spotlight.categories).map((label) => (
                  <CategoryChip
                    key={label}
                    label={label}
                    variant={getCategoryVariant(label)}
                  />
                ))}
              </div>
              <div className="flex items-center justify-between text-xs text-secondary">
                <span>by {spotlight.createdBy}</span>
                <span>{spotlight.likes} likes</span>
              </div>
            </div>
          ) : (
            <p className="text-sm text-secondary">
              Waiting for transformed projects to load.
            </p>
          )}
          {topIdeas.length > 1 && (
            <div className="border-t border-border/70 pt-4 space-y-2">
              <p className="text-xs uppercase tracking-[0.2em] text-secondary">
                Rising
              </p>
              {topIdeas.slice(1).map((idea) => (
                <div
                  key={idea.id ?? idea.projectName}
                  className="flex items-center justify-between text-sm">
                  <span className="text-foreground line-clamp-1">
                    {idea.projectName}
                  </span>
                  <span className="text-xs text-secondary">
                    {idea.likes} likes
                  </span>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="glass-panel rounded-2xl p-5 space-y-4">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-secondary">
              Signals
            </p>
            <h3 className="font-display text-xl text-foreground">
              Category momentum
            </h3>
          </div>
          <div className="space-y-3">
            <div>
              <p className="text-xs uppercase tracking-[0.2em] text-secondary mb-2">
                Top categories
              </p>
              <div className="space-y-2">
                {categories.length === 0 && (
                  <p className="text-sm text-secondary">
                    No categories yet.
                  </p>
                )}
                {categories.map((item) => (
                  <CountRow
                    key={item.label}
                    label={item.label}
                    count={item.count}
                  />
                ))}
              </div>
            </div>
            <div>
              <p className="text-xs uppercase tracking-[0.2em] text-secondary mb-2">
                Top technologies
              </p>
              <div className="space-y-2">
                {technologies.length === 0 && (
                  <p className="text-sm text-secondary">
                    No technologies yet.
                  </p>
                )}
                {technologies.map((item) => (
                  <CountRow
                    key={item.label}
                    label={item.label}
                    count={item.count}
                  />
                ))}
              </div>
            </div>
          </div>
        </section>

        <section className="glass-panel rounded-2xl p-5 space-y-4">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-secondary">
              Metrics
            </p>
            <h3 className="font-display text-xl text-foreground">
              Transformation health
            </h3>
          </div>
          <div className="space-y-3">
            <StatRow label="Projects" value={stats.projectCount.toString()} />
            <StatRow
              label="Contributors"
              value={stats.contributorCount.toString()}
            />
            <StatRow
              label="Avg likes"
              value={stats.averageLikes.toString()}
            />
            <StatRow
              label="Coverage"
              value={`${stats.coveragePercent}%`}
            />
          </div>
        </section>
      </div>
    </aside>
  );
}

function CountRow({ label, count }: { label: string; count: number }) {
  return (
    <div className="flex items-center justify-between text-sm">
      <span className="text-foreground">{label}</span>
      <span className="text-xs text-secondary">{count}</span>
    </div>
  );
}

function StatRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between text-sm">
      <span className="text-secondary">{label}</span>
      <span className="text-foreground font-medium">{value}</span>
    </div>
  );
}
