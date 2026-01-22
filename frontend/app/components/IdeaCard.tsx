"use client";

import { useState, type CSSProperties } from "react";
import CategoryChip, { getCategoryVariant } from "./CategoryChip";
import { Idea, Category, Technology } from "@/lib/types";

type IdeaCardData = Omit<Idea, "categories" | "technologies"> & {
  categories: Category[] | string[];
  technologies: Technology[] | string[];
  isLiked?: boolean;
};

interface IdeaCardProps {
  idea: IdeaCardData;
  onLike?: (id: number) => void;
  onComment?: (id: number) => void;
  onShare?: (id: number) => void;
  onCategoryClick?: (category: string) => void;
  onTechnologyClick?: (technology: string) => void;
  className?: string;
  style?: CSSProperties;
}

export default function IdeaCard({
  idea,
  onLike,
  onComment,
  onShare,
  onCategoryClick,
  onTechnologyClick,
  className,
  style,
}: IdeaCardProps) {
  const [isExpanded, setIsExpanded] = useState(false);
  const initial = idea.createdBy?.charAt(0).toUpperCase() || "?";

  const sections = [
    { label: "Problem", value: idea.problemDescription, tone: "text-primary" },
    { label: "Solution", value: idea.solution, tone: "text-accent" },
    { label: "Build", value: idea.technicalDetails, tone: "text-foreground" },
  ];

  const coverage = sections.filter((section) => section.value?.trim()).length;
  const canExpand = sections.some(
    (section) => section.value && section.value.length > 160
  );

  const handleLike = () => {
    onLike?.(idea.id!);
    console.log("Liked idea:", idea.id);
  };

  const handleComment = () => {
    onComment?.(idea.id!);
    console.log("Comment on idea:", idea.id);
  };

  const handleShare = () => {
    onShare?.(idea.id!);
    navigator.clipboard.writeText(`${window.location.origin}/idea/${idea.id!}`);
    console.log("Shared idea:", idea.id);
  };

  return (
    <article
      className={`group relative overflow-hidden rounded-3xl glass-panel card-shadow transition-transform duration-300 hover:-translate-y-1 ${
        className ?? ""
      }`}
      style={style}>
      <div className="absolute -right-16 -top-12 h-40 w-40 rounded-full bg-[radial-gradient(circle_at_center,rgba(216,106,58,0.25),transparent_70%)]" />

      <div className="relative p-6 space-y-5">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="space-y-2">
            <p className="text-[11px] uppercase tracking-[0.3em] text-secondary">
              Transformed project
            </p>
            <h3 className="font-display text-2xl text-foreground">
              {idea.projectName}
            </h3>
            <div className="flex items-center gap-2 text-sm text-secondary">
              <div className="h-8 w-8 rounded-full bg-muted text-foreground flex items-center justify-center font-semibold">
                {initial}
              </div>
              <span>by {idea.createdBy}</span>
            </div>
          </div>
          <div className="text-right">
            <p className="text-xs uppercase tracking-[0.24em] text-secondary">
              Coverage
            </p>
            <p className="text-lg font-semibold text-foreground">
              {coverage}/3
            </p>
            <p className="text-xs text-secondary">{idea.likes} likes</p>
          </div>
        </div>

        {idea.shortDescription && (
          <p className="text-base text-foreground/80 line-clamp-2">
            {idea.shortDescription}
          </p>
        )}

        <div className="grid gap-3 md:grid-cols-3">
          {sections
            .filter((section) => section.value?.trim())
            .map((section) => (
              <div
                key={section.label}
                className="rounded-2xl border border-border/70 bg-card/70 p-3">
                <p
                  className={`text-xs uppercase tracking-[0.2em] ${section.tone}`}>
                  {section.label}
                </p>
                <p
                  className={`mt-2 text-sm text-foreground/80 ${
                    isExpanded ? "" : "line-clamp-3"
                  }`}>
                  {section.value}
                </p>
              </div>
            ))}
        </div>

        <div className="flex flex-wrap gap-2">
          {idea.categories.map((category) => {
            const label = typeof category === "string" ? category : category.name;
            const key =
              typeof category === "string" ? label : category.id ?? label;

            return (
              <CategoryChip
                key={key}
                label={label}
                variant={getCategoryVariant(label)}
                onClick={() => onCategoryClick?.(label)}
              />
            );
          })}
          {idea.technologies?.map((tech, idx) => {
            const label = typeof tech === "string" ? tech : tech.name;
            return (
              <CategoryChip
                key={`${label}-${idx}`}
                label={label}
                variant={getCategoryVariant(label)}
                onClick={() => onTechnologyClick?.(label)}
              />
            );
          })}
        </div>
      </div>

      <div className="flex items-center justify-between border-t border-border/70 px-6 py-4">
        <div className="flex items-center gap-4">
          <ActionButton
            icon={<HeartIcon filled={idea.isLiked} />}
            count={idea.likes}
            isActive={idea.isLiked}
            onClick={handleLike}
          />
          <ActionButton
            icon={<CommentIcon />}
            count={idea.comments?.length}
            onClick={handleComment}
          />
        </div>
        <div className="flex items-center gap-3">
          {canExpand && (
            <button
              onClick={() => setIsExpanded((prev) => !prev)}
              className="text-xs text-primary hover:text-primary/80">
              {isExpanded ? "Collapse" : "Expand"}
            </button>
          )}
          <ActionButton icon={<ShareIcon />} onClick={handleShare} />
        </div>
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

function formatCount(count: number): string {
  if (count >= 1000000) {
    return `${(count / 1000000).toFixed(1)}M`;
  } else if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}k`;
  }
  return count.toString();
}
