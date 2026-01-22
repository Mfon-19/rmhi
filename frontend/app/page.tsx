import TopNavigation from "./components/TopNavigation";
import RightSidebar from "./components/RightSidebar";
import HomeClient from "./components/HomeClient";

export default function Home() {
  return (
    <div className="min-h-screen bg-background">
      <TopNavigation />
      <div className="flex">
        <HomeClient />
        <RightSidebar />
      </div>
    </div>
  );
}
