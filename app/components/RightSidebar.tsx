export default function RightSidebar() {
  return (
    <aside className="hidden xl:block fixed right-0 top-[72px] w-70 h-[calc(100vh-72px)] bg-white border-l border-border overflow-y-auto">
      <div className="p-4 space-y-6">
        {/* Top Categories Widget */}
        <div className="bg-white rounded-lg border border-border p-4">
          <h3 className="font-semibold text-foreground mb-3">Top Categories</h3>
          <div className="space-y-2">
            {[
              { name: "AI & ML", count: 234, trend: "up" },
              { name: "FinTech", count: 189, trend: "up" },
              { name: "Health Tech", count: 156, trend: "down" },
              { name: "Gaming", count: 142, trend: "up" },
              { name: "E-commerce", count: 128, trend: "stable" },
            ].map((category, index) => (
              <div
                key={index}
                className="flex items-center justify-between py-1">
                <div className="flex items-center space-x-2">
                  <span className="text-sm text-foreground">
                    {category.name}
                  </span>
                  <TrendIcon trend={category.trend} />
                </div>
                <span className="text-xs text-secondary">{category.count}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Most Liked This Week Widget */}
        <div className="bg-white rounded-lg border border-border p-4">
          <h3 className="font-semibold text-foreground mb-3">
            Most Liked This Week
          </h3>
          <div className="space-y-3">
            {[
              {
                title: "AI-Powered Code Review Assistant",
                author: "Sarah Chen",
                likes: 267,
              },
              {
                title: "Virtual Reality Fitness Trainer",
                author: "Alex Rivera",
                likes: 203,
              },
              {
                title: "Micro-Investment Gaming App",
                author: "Jake Wilson",
                likes: 178,
              },
            ].map((idea, index) => (
              <div
                key={index}
                className="border-b border-border last:border-b-0 pb-2 last:pb-0">
                <h4 className="text-sm font-medium text-foreground line-clamp-2 mb-1">
                  {idea.title}
                </h4>
                <div className="flex items-center justify-between">
                  <span className="text-xs text-secondary">
                    by {idea.author}
                  </span>
                  <div className="flex items-center space-x-1">
                    <svg
                      className="w-3 h-3 text-primary"
                      fill="currentColor"
                      viewBox="0 0 24 24">
                      <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                    </svg>
                    <span className="text-xs text-secondary">{idea.likes}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Community Stats Widget */}
        <div className="bg-white rounded-lg border border-border p-4">
          <h3 className="font-semibold text-foreground mb-3">
            Community Stats
          </h3>
          <div className="space-y-3">
            <StatItem label="Ideas this week" value="127" change="+12%" />
            <StatItem label="Active members" value="2.4k" change="+5%" />
            <StatItem label="Comments today" value="89" change="+23%" />
            <StatItem label="Total ideas" value="15.2k" change="+8%" />
          </div>
        </div>

        {/* Trending Tags Widget */}
        <div className="bg-white rounded-lg border border-border p-4">
          <h3 className="font-semibold text-foreground mb-3">Trending Tags</h3>
          <div className="flex flex-wrap gap-2">
            {[
              "#AI",
              "#MobileApp",
              "#FinTech",
              "#Web3",
              "#Health",
              "#Gaming",
              "#SaaS",
              "#Productivity",
              "#Social",
            ].map((tag, index) => (
              <button
                key={index}
                className="px-2 py-1 text-xs bg-gray-100 text-gray-700 rounded-full hover:bg-gray-200 transition-colors">
                {tag}
              </button>
            ))}
          </div>
        </div>
      </div>
    </aside>
  );
}

function TrendIcon({ trend }: { trend: "up" | "down" | "stable" }) {
  const icons = {
    up: (
      <svg
        className="w-3 h-3 text-green-500"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M7 17l9.2-9.2M17 17V7m0 10H7"
        />
      </svg>
    ),
    down: (
      <svg
        className="w-3 h-3 text-red-500"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M17 7l-9.2 9.2M7 7v10m0-10h10"
        />
      </svg>
    ),
    stable: (
      <svg
        className="w-3 h-3 text-gray-400"
        fill="none"
        stroke="currentColor"
        viewBox="0 0 24 24">
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M20 12H4"
        />
      </svg>
    ),
  };

  return icons[trend];
}

function StatItem({
  label,
  value,
  change,
}: {
  label: string;
  value: string;
  change: string;
}) {
  const isPositive = change.startsWith("+");

  return (
    <div className="flex items-center justify-between">
      <span className="text-sm text-secondary">{label}</span>
      <div className="flex items-center space-x-2">
        <span className="text-sm font-medium text-foreground">{value}</span>
        <span
          className={`text-xs ${
            isPositive ? "text-green-500" : "text-red-500"
          }`}>
          {change}
        </span>
      </div>
    </div>
  );
}
