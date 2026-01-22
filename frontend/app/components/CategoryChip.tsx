interface CategoryChipProps {
  label: string;
  variant?:
    | "default"
    | "mobile"
    | "ai"
    | "fintech"
    | "web3"
    | "health"
    | "education"
    | "gaming";
  size?: "sm" | "md";
  onClick?: () => void;
}

const categoryColors = {
  default: "bg-muted text-secondary hover:bg-muted",
  mobile: "bg-teal-100 text-teal-900 hover:bg-teal-200",
  ai: "bg-emerald-100 text-emerald-900 hover:bg-emerald-200",
  fintech: "bg-amber-100 text-amber-900 hover:bg-amber-200",
  web3: "bg-sky-100 text-sky-900 hover:bg-sky-200",
  health: "bg-rose-100 text-rose-900 hover:bg-rose-200",
  education: "bg-lime-100 text-lime-900 hover:bg-lime-200",
  gaming: "bg-cyan-100 text-cyan-900 hover:bg-cyan-200",
};

export default function CategoryChip({
  label,
  variant = "default",
  size = "sm",
  onClick,
}: CategoryChipProps) {
  const sizeClasses = {
    sm: "px-2 py-1 text-xs",
    md: "px-3 py-1.5 text-sm",
  };

  const colorClasses = categoryColors[variant];

  return (
    <button
      onClick={onClick}
      className={`inline-flex items-center rounded-full font-medium transition-colors ${sizeClasses[size]} ${colorClasses} ${onClick ? "cursor-pointer" : "cursor-default"}`}>
      #{label}
    </button>
  );
}

export function getCategoryVariant(
  category: string
): CategoryChipProps["variant"] {
  const lowerCategory = category.toLowerCase();

  if (lowerCategory.includes("mobile") || lowerCategory.includes("app"))
    return "mobile";
  if (lowerCategory.includes("ai") || lowerCategory.includes("ml")) return "ai";
  if (lowerCategory.includes("fintech") || lowerCategory.includes("finance"))
    return "fintech";
  if (
    lowerCategory.includes("web3") ||
    lowerCategory.includes("crypto") ||
    lowerCategory.includes("blockchain")
  )
    return "web3";
  if (lowerCategory.includes("health") || lowerCategory.includes("medical"))
    return "health";
  if (lowerCategory.includes("education") || lowerCategory.includes("learning"))
    return "education";
  if (lowerCategory.includes("gaming") || lowerCategory.includes("game"))
    return "gaming";

  return "default";
}
