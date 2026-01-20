"use client";

import { useState } from "react";
import CategoryChip, { getCategoryVariant } from "./CategoryChip";

interface PostIdeaModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export interface IdeaFormData {
  title: string;
  categories: string[];
  description: string;
  image?: File;
}

const SUGGESTED_CATEGORIES = [
  "MobileApp",
  "AI",
  "FinTech",
  "Web3",
  "Health",
  "Education",
  "Gaming",
  "SaaS",
  "E-commerce",
  "Social",
  "Productivity",
  "Entertainment",
];

const DISPLAY_NAME_TO_CATEGORY: Record<string, string> = {
  MobileApp: "MOBILE_APP",
  AI: "AI",
  FinTech: "FIN_TECH",
  Web3: "WEB3",
  Health: "HEALTH",
  Education: "EDUCATION",
  Gaming: "GAMING",
  SaaS: "SAAS",
  "E-commerce": "E_COMMERCE",
  Social: "SOCIAL",
  Productivity: "PRODUCTIVITY",
  Entertainment: "ENTERTAINMENT",
};

export default function PostIdeaModal({ isOpen, onClose }: PostIdeaModalProps) {
  const [formData, setFormData] = useState<IdeaFormData>({
    title: "",
    categories: [],
    description: "",
  });
  const [customCategory, setCustomCategory] = useState("");
  const [imagePreview, setImagePreview] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleClose = () => {
    setFormData({ title: "", categories: [], description: "" });
    setCustomCategory("");
    setImagePreview(null);
    onClose();
  };

  const addCategory = (category: string) => {
    if (!formData.categories.includes(category)) {
      setFormData((prev) => ({
        ...prev,
        categories: [...prev.categories, category],
      }));
    }
  };

  const removeCategory = (category: string) => {
    setFormData((prev) => ({
      ...prev,
      categories: prev.categories.filter((cat) => cat !== category),
    }));
  };

  const handleAddCustomCategory = () => {
    if (
      customCategory.trim() &&
      !formData.categories.includes(customCategory.trim())
    ) {
      addCategory(customCategory.trim());
      setCustomCategory("");
    }
  };

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setFormData((prev) => ({ ...prev, image: file }));
      const reader = new FileReader();
      reader.onload = (e) => {
        setImagePreview(e.target?.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const removeImage = () => {
    setFormData((prev) => ({ ...prev, image: undefined }));
    setImagePreview(null);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-border">
          <h2 className="text-xl font-semibold text-foreground">
            Post Your Idea
          </h2>
          <button
            onClick={handleClose}
            className="p-2 text-secondary hover:text-foreground hover:cursor-pointer transition-colors">
            <svg
              className="w-5 h-5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <form onSubmit={() => {}} className="p-6 space-y-6">
          <div>
            <label
              htmlFor="title"
              className="block text-sm font-medium text-foreground mb-2">
              Title *
            </label>
            <input
              id="title"
              type="text"
              required
              value={formData.title}
              onChange={(e) =>
                setFormData((prev) => ({ ...prev, title: e.target.value }))
              }
              placeholder="What's your idea?"
              className="w-full px-3 py-2 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-2">
              Categories
            </label>

            {formData.categories.length > 0 && (
              <div className="flex flex-wrap gap-2 mb-3">
                {formData.categories.map((category) => (
                  <div key={category} className="relative">
                    <CategoryChip
                      label={category}
                      variant={getCategoryVariant(category)}
                      size="md"
                    />
                    <button
                      type="button"
                      onClick={() => removeCategory(category)}
                      className="absolute -top-1 -right-1 w-4 h-4 bg-red-500 text-white rounded-full text-xs flex items-center justify-center hover:bg-red-600 hover:cursor-pointer">
                      ×
                    </button>
                  </div>
                ))}
              </div>
            )}

            <div className="mb-3">
              <p className="text-xs text-secondary mb-2">
                Suggested categories:
              </p>
              <div className="flex flex-wrap gap-2">
                {SUGGESTED_CATEGORIES.filter(
                  (cat) => !formData.categories.includes(cat)
                ).map((category) => (
                  <button
                    key={category}
                    type="button"
                    onClick={() => addCategory(category)}
                    className="text-xs px-2 py-1 border border-border rounded-full hover:bg-gray-50 hover:cursor-pointer transition-colors">
                    #{category}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex gap-2">
              <input
                type="text"
                value={customCategory}
                onChange={(e) => setCustomCategory(e.target.value)}
                placeholder="Add custom category..."
                className="flex-1 px-3 py-2 text-sm border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
                onKeyPress={(e) =>
                  e.key === "Enter" &&
                  (e.preventDefault(), handleAddCustomCategory())
                }
              />
              <button
                type="button"
                onClick={handleAddCustomCategory}
                className="px-3 py-2 text-sm bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 hover:cursor-pointer transition-colors">
                Add
              </button>
            </div>
          </div>

          <div>
            <label
              htmlFor="description"
              className="block text-sm font-medium text-foreground mb-2">
              Describe your idea *
            </label>
            <textarea
              id="description"
              required
              value={formData.description}
              onChange={(e) =>
                setFormData((prev) => ({
                  ...prev,
                  description: e.target.value,
                }))
              }
              placeholder="Tell us about your idea... What problem does it solve? How does it work?"
              rows={6}
              className="w-full px-3 py-2 border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent resize-none"
            />
            <p className="text-xs text-secondary mt-1">Markdown supported</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-foreground mb-2">
              Attach image (optional)
            </label>

            {imagePreview ? (
              <div className="relative">
                <img
                  src={imagePreview}
                  alt="Preview"
                  className="w-full h-48 object-cover rounded-lg border border-border"
                />
                <button
                  type="button"
                  onClick={removeImage}
                  className="absolute top-2 right-2 p-1 bg-red-500 text-white rounded-full hover:bg-red-600 hover:cursor-pointer transition-colors">
                  <svg
                    className="w-4 h-4"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M6 18L18 6M6 6l12 12"
                    />
                  </svg>
                </button>
              </div>
            ) : (
              <div className="border-2 border-dashed border-border rounded-lg p-6 text-center">
                <input
                  type="file"
                  id="image-upload"
                  accept="image/*"
                  onChange={handleImageUpload}
                  className="hidden"
                />
                <label
                  htmlFor="image-upload"
                  className="cursor-pointer hover:cursor-pointer flex flex-col items-center space-y-2">
                  <svg
                    className="w-8 h-8 text-secondary"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
                    />
                  </svg>
                  <span className="text-sm text-secondary">
                    Click to upload an image
                  </span>
                  <span className="text-xs text-secondary">
                    PNG, JPG up to 10MB
                  </span>
                </label>
              </div>
            )}
          </div>

          <div className="flex justify-end space-x-3 pt-4 border-t border-border">
            <button
              type="button"
              onClick={handleClose}
              className="px-4 py-2 text-secondary hover:text-foreground hover:cursor-pointer transition-colors">
              Cancel
            </button>
            <button
              type="submit"
              disabled={!formData.title.trim() || !formData.description.trim()}
              className="px-6 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 hover:cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-medium">
              Publish Idea
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
