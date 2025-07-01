import { IdeaData } from "../components/IdeaCard";
import hackathonIdeas from "../../ideas.json";

interface HackathonIdea {
  id: string;
  project_name: string;
  short_description: string;
  like_count: number;
  submitted_to: string;
  winner: boolean;
  created_by: {
    name: string;
    profile: string;
  };
  problem_description: string;
  solution: string;
  technical_details: string;
  built_with: string[];
  comments: any[];
}

const HACKATHON_AVATARS = [
  "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=40&h=40&fit=crop&crop=face",
  "https://images.unsplash.com/photo-1494790108755-2616b612b786?w=40&h=40&fit=crop&crop=face",
  "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=40&h=40&fit=crop&crop=face",
  "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=40&h=40&fit=crop&crop=face",
  "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=40&h=40&fit=crop&crop=face",
  "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=40&h=40&fit=crop&crop=face",
  "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=40&h=40&fit=crop&crop=face",
  "https://images.unsplash.com/photo-1544725176-7c40e5a71c5e?w=40&h=40&fit=crop&crop=face",
  "https://images.unsplash.com/photo-1557862921-37829c790f19?w=40&h=40&fit=crop&crop=face",
  "https://images.unsplash.com/photo-1519244703995-f4e0f30006d5?w=40&h=40&fit=crop&crop=face",
];

function getRandomAvatar(): string {
  return HACKATHON_AVATARS[
    Math.floor(Math.random() * HACKATHON_AVATARS.length)
  ];
}

function generateTimestamp(): string {
  const timeOptions = [
    "2h",
    "4h",
    "6h",
    "8h",
    "12h",
    "1d",
    "2d",
    "3d",
    "1w",
    "2w",
  ];
  return timeOptions[Math.floor(Math.random() * timeOptions.length)];
}

function transformCategories(builtWith: string[]): string[] {
  const categoryMap: { [key: string]: string } = {
    python: "Python",
    javascript: "JavaScript",
    react: "React",
    "node.js": "Node.js",
    arduino: "Hardware",
    "raspberry-pi": "Hardware",
    ai: "AI",
    "machine-learning": "AI",
    ml: "AI",
    tensorflow: "AI",
    opencv: "Computer Vision",
    blockchain: "Web3",
    ethereum: "Web3",
    android: "Mobile",
    ios: "Mobile",
    swift: "iOS",
    java: "Java",
    "c++": "C++",
    unity: "Game Dev",
    vr: "VR/AR",
    ar: "VR/AR",
    iot: "IoT",
    cloud: "Cloud",
    aws: "Cloud",
    "google-cloud": "Cloud",
    azure: "Cloud",
    web: "Web",
    api: "API",
    database: "Database",
    mysql: "Database",
    mongodb: "Database",
    health: "Healthcare",
    fintech: "FinTech",
    education: "EdTech",
    sustainability: "Green Tech",
  };

  const categories = builtWith
    .map((tech) => categoryMap[tech.toLowerCase()] || tech)
    .filter((category, index, arr) => arr.indexOf(category) === index) // Remove duplicates
    .slice(0, 4); // Limit to 4 categories

  return categories;
}

function createBodyText(idea: HackathonIdea): string {
  let body = idea.short_description;

  if (idea.problem_description) {
    body += `\n\n**Problem:** ${idea.problem_description}`;
  }

  if (idea.solution) {
    body += `\n\n**Solution:** ${idea.solution}`;
  }

  return body;
}

function extractUsernameFromProfile(profileUrl: string): {
  name: string;
  url: string;
} {
  if (!profileUrl || !profileUrl.includes("devpost.com/")) {
    return { name: "", url: "" };
  }

  const username = profileUrl.split("/").pop() || "";
  const capitalizedName = username.charAt(0).toUpperCase() + username.slice(1);

  return {
    name: capitalizedName,
    url: profileUrl,
  };
}

function transformHackathonIdea(
  hackathonIdea: HackathonIdea,
  index: number
): IdeaData {
  const authorInfo = extractUsernameFromProfile(
    hackathonIdea.created_by.profile
  );

  return {
    id: hackathonIdea.id,
    title: hackathonIdea.project_name,
    body: createBodyText(hackathonIdea),
    author: {
      name:
        authorInfo.name ||
        hackathonIdea.created_by.name ||
        `Developer ${index + 1}`,
      avatar: getRandomAvatar(),
      role: hackathonIdea.winner ? "🏆 Winner" : "Participant",
      company: hackathonIdea.submitted_to,
      profileUrl: authorInfo.url || undefined,
    },
    timestamp: generateTimestamp(),
    categories: transformCategories(hackathonIdea.built_with),
    stats: {
      likes: hackathonIdea.like_count,
      comments: hackathonIdea.comments.length,
      views: hackathonIdea.like_count * 10 + Math.floor(Math.random() * 500), // Estimate views
    },
    isLiked: false,
  };
}

export function getHackathonIdeas(count?: number): IdeaData[] {
  const ideas = hackathonIdeas.ideas as HackathonIdea[];
  const limitedIdeas = count ? ideas.slice(0, count) : ideas;

  return limitedIdeas.map((idea, index) => transformHackathonIdea(idea, index));
}

export function getWinningIdeas(count?: number): IdeaData[] {
  const ideas = hackathonIdeas.ideas as HackathonIdea[];
  const winningIdeas = ideas.filter((idea) => idea.winner);
  const limitedIdeas = count ? winningIdeas.slice(0, count) : winningIdeas;

  return limitedIdeas.map((idea, index) => transformHackathonIdea(idea, index));
}

export function getIdeasByHackathon(
  hackathonName: string,
  count?: number
): IdeaData[] {
  const ideas = hackathonIdeas.ideas as HackathonIdea[];
  const filteredIdeas = ideas.filter((idea) =>
    idea.submitted_to.toLowerCase().includes(hackathonName.toLowerCase())
  );
  const limitedIdeas = count ? filteredIdeas.slice(0, count) : filteredIdeas;

  return limitedIdeas.map((idea, index) => transformHackathonIdea(idea, index));
}

export function searchIdeas(query: string, count?: number): IdeaData[] {
  const ideas = hackathonIdeas.ideas as HackathonIdea[];
  const searchTerm = query.toLowerCase();

  const filteredIdeas = ideas.filter(
    (idea) =>
      idea.project_name.toLowerCase().includes(searchTerm) ||
      idea.short_description.toLowerCase().includes(searchTerm) ||
      idea.built_with.some((tech) => tech.toLowerCase().includes(searchTerm)) ||
      idea.submitted_to.toLowerCase().includes(searchTerm)
  );

  const limitedIdeas = count ? filteredIdeas.slice(0, count) : filteredIdeas;

  return limitedIdeas.map((idea, index) => transformHackathonIdea(idea, index));
}

export function getRandomIdeas(count: number = 20): IdeaData[] {
  const ideas = hackathonIdeas.ideas as HackathonIdea[];
  const shuffled = [...ideas].sort(() => Math.random() - 0.5);
  const limitedIdeas = shuffled.slice(0, count);

  return limitedIdeas.map((idea, index) => transformHackathonIdea(idea, index));
}
