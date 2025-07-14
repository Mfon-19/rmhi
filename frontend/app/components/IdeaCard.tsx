"use client";

import { useState } from "react";
import CategoryChip, { getCategoryVariant } from "./CategoryChip";
import { Idea, Category, Technology } from "../utils/types";

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

  const buildBody = (idea: IdeaCardProps["idea"]): string => {
    let body = idea.shortDescription || "";
    if (idea.problemDescription) {
      body += `\n\n**Problem:** ${idea.problemDescription}`;
    }
    if (idea.solution) {
      body += `\n\n**Solution:** ${idea.solution}`;
    }
    if (idea.technicalDetails) {
      body += `\n\n**Technical Details:** ${idea.technicalDetails}`;
    }
    return body;
  };

  const rawBody = buildBody(idea);

  const shouldTruncate = rawBody.length > 180;
  const displayBody =
    shouldTruncate && !isExpanded ? rawBody.substring(0, 180) + "..." : rawBody;

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
      className={`bg-white rounded-lg border border-border transition-all duration-200 ${
        isHovered ? "shadow-md" : "shadow-sm"
      }`}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}>
      {/* Header */}
      <div className="flex items-center justify-between p-4 pb-3">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center text-sm font-bold text-gray-600">
            {idea.createdBy.charAt(0).toUpperCase()}
          </div>
          <div>
            <span className="font-semibold text-foreground">
              {idea.createdBy}
            </span>
          </div>
        </div>
      </div>

      {/* Title */}
      <div className="px-4 pb-2">
        <h4 className="text-lg font-medium text-foreground line-clamp-1">
          {idea.projectName}
        </h4>
      </div>

      {/* Body */}
      <div className="px-4 pb-3">
        <div className="text-foreground leading-relaxed">
          <FormattedText text={displayBody} />
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
        </div>
      </div>

      {/* Category Tags */}
      {idea.categories && idea.categories.length > 0 && (
        <div className="px-4 pb-3">
          <div className="flex flex-wrap gap-2">
            {idea.categories.map((category) => {
              const label =
                typeof category === "string" ? category : category.name;
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
          </div>
        </div>
      )}

      {/* Technology Tags */}
      {idea.technologies && idea.technologies.length > 0 && (
        <div className="px-4 pb-3">
          <div className="flex flex-wrap gap-2">
            {idea.technologies.map((tech, idx) => {
              const label = typeof tech === "string" ? tech : tech.name;
              return (
                <CategoryChip
                  key={idx}
                  label={label}
                  variant={getCategoryVariant(label)}
                  onClick={() => console.log("Filter by technology:", label)}
                />
              );
            })}
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

        <ActionButton icon={<ShareIcon />} onClick={handleShare} />
      </div>
    </article>
  );
}

function FormattedText({ text }: { text: string }) {
  const formatText = (input: string) => {
    const paragraphs = input.split("\n\n");

    return paragraphs
      .map((paragraph, paragraphIndex) => {
        const trimmedParagraph = paragraph.trim();
        if (!trimmedParagraph) return null;

        const isProblemSection = trimmedParagraph.startsWith("**Problem:**");
        const isSolutionSection = trimmedParagraph.startsWith("**Solution:**");
        const isTechnicalSection = trimmedParagraph.startsWith(
          "**Technical Details:**"
        );

        if (isProblemSection || isSolutionSection || isTechnicalSection) {
          const headerMatch = trimmedParagraph.match(
            /^\*\*(Problem|Solution|Technical Details):\*\*\s*([\s\S]*)/
          );
          if (headerMatch) {
            const [, headerType, content] = headerMatch;
            return (
              <div key={paragraphIndex} className="mt-6 first:mt-0">
                <h5 className="text-lg font-bold text-primary mb-3 border-l-4 border-primary pl-3">
                  {headerType}:
                </h5>
                <div className="ml-4">
                  <FormattedParagraph text={content} />
                </div>
              </div>
            );
          }
        }

        return (
          <div key={paragraphIndex} className="mb-4 first:mb-2">
            <FormattedParagraph text={trimmedParagraph} />
          </div>
        );
      })
      .filter(Boolean);
  };

  return <div>{formatText(text)}</div>;
}

function FormattedParagraph({ text }: { text: string }) {
  const parts = text.split(/(\*\*[^*]+\*\*)/);

  return (
    <p className="leading-relaxed">
      {parts.map((part, partIndex) => {
        if (part.startsWith("**") && part.endsWith("**")) {
          const boldText = part.slice(2, -2);
          return (
            <strong key={partIndex} className="font-semibold text-foreground">
              {boldText}
            </strong>
          );
        }
        return part;
      })}
    </p>
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

function formatCount(count: number): string {
  if (count >= 1000000) {
    return `${(count / 1000000).toFixed(1)}M`;
  } else if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}k`;
  }
  return count.toString();
}
