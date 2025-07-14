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
  default: "bg-gray-100 text-gray-700 hover:bg-gray-200",
  mobile: "bg-blue-100 text-blue-700 hover:bg-blue-200",
  ai: "bg-purple-100 text-purple-700 hover:bg-purple-200",
  fintech: "bg-green-100 text-green-700 hover:bg-green-200",
  web3: "bg-orange-100 text-orange-700 hover:bg-orange-200",
  health: "bg-red-100 text-red-700 hover:bg-red-200",
  education: "bg-yellow-100 text-yellow-700 hover:bg-yellow-200",
  gaming: "bg-pink-100 text-pink-700 hover:bg-pink-200",
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
      className={`
        inline-flex items-center rounded-full font-medium transition-colors
        ${sizeClasses[size]}
        ${colorClasses}
        ${onClick ? "cursor-pointer" : "cursor-default"}
      `}>
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
