"use client";

import { useState } from "react";
import CategoryChip, { getCategoryVariant } from "./CategoryChip";

export interface IdeaData {
  id: string;
  title: string;
  body: string;
  author: {
    name: string;
    avatar: string;
    role?: string;
    company?: string;
  };
  timestamp: string;
  categories: string[];
  stats: {
    likes: number;
    comments: number;
    views: number;
  };
  isLiked?: boolean;
}

interface IdeaCardProps {
  idea: IdeaData;
  onLike?: (id: string) => void;
  onComment?: (id: string) => void;
  onShare?: (id: string) => void;
  onCategoryClick?: (category: string) => void;
}

export default function IdeaCard({
  idea,
  onLike,
  onComment,
  onShare,
  onCategoryClick,
}: IdeaCardProps) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [isHovered, setIsHovered] = useState(false);

  const shouldTruncate = idea.body.length > 180;
  const displayBody =
    shouldTruncate && !isExpanded
      ? idea.body.substring(0, 180) + "..."
      : idea.body;

  const handleLike = () => {
    onLike?.(idea.id);
    console.log("Liked idea:", idea.id);
  };

  const handleComment = () => {
    onComment?.(idea.id);
    console.log("Comment on idea:", idea.id);
  };

  const handleShare = () => {
    onShare?.(idea.id);
    navigator.clipboard.writeText(`${window.location.origin}/idea/${idea.id}`);
    console.log("Shared idea:", idea.id);
  };

  return (
    <article
      className={`bg-white rounded-lg border border-border transition-all duration-200 ${
        isHovered ? "shadow-md" : "shadow-sm"
      }`}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}>
      {/* Header */}
      <div className="flex items-center justify-between p-4 pb-3">
        <div className="flex items-center space-x-3">
          <img
            src={idea.author.avatar}
            alt={idea.author.name}
            className="w-10 h-10 rounded-full object-cover"
          />
          <div className="flex items-center space-x-2">
            <span className="font-semibold text-foreground">
              {idea.author.name}
            </span>
            {idea.author.role && (
              <span className="text-xs text-secondary">
                {idea.author.company
                  ? `${idea.author.role} at ${idea.author.company}`
                  : idea.author.role}
              </span>
            )}
            <span className="text-secondary">·</span>
            <span className="text-sm text-secondary">{idea.timestamp}</span>
          </div>
        </div>

        <button className="p-1 text-secondary hover:text-foreground transition-colors">
          <svg
            className="w-5 h-5"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z"
            />
          </svg>
        </button>
      </div>

      {/* Title */}
      <div className="px-4 pb-2">
        <h4 className="text-lg font-medium text-foreground line-clamp-1">
          {idea.title}
        </h4>
      </div>

      {/* Body */}
      <div className="px-4 pb-3">
        <p className="text-foreground leading-relaxed">
          {displayBody}
          {shouldTruncate && !isExpanded && (
            <button
              onClick={() => setIsExpanded(true)}
              className="ml-1 text-primary hover:text-primary/80 font-medium">
              Read more
            </button>
          )}
          {isExpanded && shouldTruncate && (
            <button
              onClick={() => setIsExpanded(false)}
              className="ml-1 text-primary hover:text-primary/80 font-medium">
              Show less
            </button>
          )}
        </p>
      </div>

      {/* Tags */}
      {idea.categories.length > 0 && (
        <div className="px-4 pb-3">
          <div className="flex flex-wrap gap-2">
            {idea.categories.map((category, index) => (
              <CategoryChip
                key={index}
                label={category}
                variant={getCategoryVariant(category)}
                onClick={() => onCategoryClick?.(category)}
              />
            ))}
          </div>
        </div>
      )}

      {/* Action Row */}
      <div
        className={`flex items-center justify-between px-4 py-3 border-t transition-colors ${
          isHovered ? "border-border" : "border-transparent"
        }`}>
        <div className="flex items-center space-x-6">
          <ActionButton
            icon={<HeartIcon filled={idea.isLiked} />}
            count={idea.stats.likes}
            isActive={idea.isLiked}
            onClick={handleLike}
          />
          <ActionButton
            icon={<CommentIcon />}
            count={idea.stats.comments}
            onClick={handleComment}
          />
          <ActionButton icon={<EyeIcon />} count={idea.stats.views} />
        </div>

        <ActionButton icon={<ShareIcon />} onClick={handleShare} />
      </div>
    </article>
  );
}

interface ActionButtonProps {
  icon: React.ReactNode;
  count?: number;
  isActive?: boolean;
  onClick?: () => void;
}

function ActionButton({
  icon,
  count,
  isActive = false,
  onClick,
}: ActionButtonProps) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center space-x-1 transition-colors ${
        isActive ? "text-primary" : "text-secondary hover:text-primary"
      } ${onClick ? "cursor-pointer" : "cursor-default"}`}>
      <span className="w-5 h-5">{icon}</span>
      {count !== undefined && (
        <span className="text-sm font-medium">{formatCount(count)}</span>
      )}
    </button>
  );
}

// Icon Components
function HeartIcon({ filled = false }: { filled?: boolean }) {
  return (
    <svg
      className="w-5 h-5"
      fill={filled ? "currentColor" : "none"}
      stroke="currentColor"
      viewBox="0 0 24 24">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
      />
    </svg>
  );
}

function CommentIcon() {
  return (
    <svg
      className="w-5 h-5"
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
      />
    </svg>
  );
}

function EyeIcon() {
  return (
    <svg
      className="w-5 h-5"
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
      />
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
      />
    </svg>
  );
}

function ShareIcon() {
  return (
    <svg
      className="w-5 h-5"
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24">
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.367 2.684 3 3 0 00-5.367-2.684z"
      />
    </svg>
  );
}

// Utility function to format numbers
function formatCount(count: number): string {
  if (count >= 1000000) {
    return `${(count / 1000000).toFixed(1)}M`;
  } else if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}k`;
  }
  return count.toString();
}
