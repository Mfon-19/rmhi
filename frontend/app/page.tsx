"use client";

import { useState } from "react";
import TopNavigation from "./components/TopNavigation";
import LeftSidebar from "./components/LeftSidebar";
import MainFeed from "./components/MainFeed";
import RightSidebar from "./components/RightSidebar";
import PostIdeaModal from "./components/PostIdeaModal";
import { auth } from "./utils/firebase";

export default function Home() {
  const [isPostModalOpen, setIsPostModalOpen] = useState(false);
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  let isLoggedIn = false;
  if (auth.currentUser) {
    isLoggedIn = true;
  }

  const handlePostIdea = () => {
    setIsPostModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsPostModalOpen(false);
  };

  return (
    <div className="min-h-screen bg-background">
      <TopNavigation />
      <div className="flex">
        <LeftSidebar isOpen={isSidebarOpen} />
        <div
          className={`flex-1 transition-all duration-300 ${
            isSidebarOpen ? "ml-60" : "ml-0"
          } xl:mr-70`}>
          <div className="pt-[72px]">
            <MainFeed onPostIdea={handlePostIdea} />
          </div>
        </div>
        <RightSidebar />
      </div>
      <PostIdeaModal
        isOpen={isPostModalOpen}
        onClose={handleCloseModal}
      />
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
