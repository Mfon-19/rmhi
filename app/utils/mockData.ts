import { IdeaData } from "../components/IdeaCard";

const SAMPLE_AVATARS = [
  "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=40&h=40&fit=crop&crop=face",
  "https://images.unsplash.com/photo-1494790108755-2616b612b786?w=40&h=40&fit=crop&crop=face",
  "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=40&h=40&fit=crop&crop=face",
  "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=40&h=40&fit=crop&crop=face",
  "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=40&h=40&fit=crop&crop=face",
  "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=40&h=40&fit=crop&crop=face",
  "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=40&h=40&fit=crop&crop=face",
  "https://images.unsplash.com/photo-1544725176-7c40e5a71c5e?w=40&h=40&fit=crop&crop=face",
];

const SAMPLE_IDEAS: Omit<IdeaData, "id">[] = [
  {
    title: "AI-Powered Code Review Assistant",
    body: "A tool that automatically reviews pull requests and suggests improvements based on best practices, security vulnerabilities, and performance optimizations. It would integrate with GitHub, GitLab, and other version control systems to provide real-time feedback to developers. The AI would learn from successful projects and adapt to team coding standards over time.",
    author: {
      name: "Sarah Chen",
      avatar: SAMPLE_AVATARS[0],
      role: "Senior Developer",
      company: "TechCorp",
    },
    timestamp: "2h",
    categories: ["AI", "Developer Tools", "Productivity"],
    stats: {
      likes: 127,
      comments: 23,
      views: 1420,
    },
    isLiked: false,
  },
  {
    title: "Sustainable Fashion Marketplace",
    body: "A platform connecting eco-conscious consumers with sustainable fashion brands. Features include carbon footprint tracking for each purchase, clothing lifecycle analysis, and a resale marketplace for extending garment life. Users can scan clothing tags to learn about materials, manufacturing processes, and environmental impact.",
    author: {
      name: "Marcus Johnson",
      avatar: SAMPLE_AVATARS[1],
      role: "Product Manager",
      company: "GreenTech",
    },
    timestamp: "4h",
    categories: ["E-commerce", "Sustainability", "Fashion"],
    stats: {
      likes: 89,
      comments: 15,
      views: 892,
    },
    isLiked: true,
  },
  {
    title: "Virtual Reality Fitness Trainer",
    body: "An immersive VR fitness app that transforms boring workouts into exciting adventures. Users can climb virtual mountains, fight dragons while doing cardio, or explore alien worlds during strength training. The app tracks real-world movements and provides personalized coaching based on fitness goals and progress.",
    author: {
      name: "Alex Rivera",
      avatar: SAMPLE_AVATARS[2],
      role: "VR Developer",
    },
    timestamp: "6h",
    categories: ["VR", "Health", "Gaming", "Fitness"],
    stats: {
      likes: 203,
      comments: 41,
      views: 2156,
    },
    isLiked: false,
  },
  {
    title: "Decentralized Learning Platform",
    body: "A blockchain-based education platform where experts can create courses and students earn crypto tokens for completing lessons. Smart contracts automatically distribute payments to instructors based on student engagement and completion rates. The platform includes peer-to-peer mentoring and skill verification through NFT certificates.",
    author: {
      name: "Priya Patel",
      avatar: SAMPLE_AVATARS[3],
      role: "Blockchain Developer",
      company: "CryptoEd",
    },
    timestamp: "8h",
    categories: ["Web3", "Education", "Blockchain"],
    stats: {
      likes: 156,
      comments: 28,
      views: 1678,
    },
    isLiked: true,
  },
  {
    title: "Smart Home Energy Optimizer",
    body: "An IoT system that learns your daily routines and automatically optimizes energy usage throughout your home. It integrates with smart appliances, solar panels, and energy storage systems to minimize costs and environmental impact. The app provides detailed insights into energy consumption patterns and suggests behavioral changes for maximum efficiency.",
    author: {
      name: "David Kim",
      avatar: SAMPLE_AVATARS[4],
      role: "IoT Engineer",
      company: "SmartTech",
    },
    timestamp: "12h",
    categories: ["IoT", "Energy", "Smart Home"],
    stats: {
      likes: 94,
      comments: 19,
      views: 1123,
    },
    isLiked: false,
  },
  {
    title: "Mental Health Check-in Bot",
    body: "A compassionate AI chatbot that provides daily mental health check-ins and emotional support. It uses natural language processing to detect mood patterns and offers personalized coping strategies, meditation exercises, and professional resource recommendations. The bot ensures complete privacy while building long-term wellness habits.",
    author: {
      name: "Emma Thompson",
      avatar: SAMPLE_AVATARS[5],
      role: "UX Designer",
      company: "MindfulTech",
    },
    timestamp: "1d",
    categories: ["AI", "Health", "Mental Health"],
    stats: {
      likes: 267,
      comments: 52,
      views: 3421,
    },
    isLiked: true,
  },
  {
    title: "Micro-Investment Gaming App",
    body: "A gamified investment platform that turns stock market investing into an engaging RPG-style experience. Users start with virtual currency, complete investment quests, and gradually transition to real money as they level up their financial knowledge. The app includes educational content, risk assessment tools, and social features for sharing investment strategies.",
    author: {
      name: "Jake Wilson",
      avatar: SAMPLE_AVATARS[6],
      role: "Fintech Developer",
    },
    timestamp: "1d",
    categories: ["FinTech", "Gaming", "Education"],
    stats: {
      likes: 178,
      comments: 34,
      views: 2087,
    },
    isLiked: false,
  },
  {
    title: "AR Recipe Assistant",
    body: "An augmented reality cooking app that overlays step-by-step instructions directly onto your kitchen workspace. It recognizes ingredients and cookware through your phone's camera, provides real-time cooking tips, and adjusts recipes based on available ingredients. The app includes voice commands and gesture controls for hands-free operation while cooking.",
    author: {
      name: "Lisa Zhang",
      avatar: SAMPLE_AVATARS[7],
      role: "AR Developer",
      company: "CookTech",
    },
    timestamp: "2d",
    categories: ["AR", "Food", "MobileApp"],
    stats: {
      likes: 145,
      comments: 27,
      views: 1834,
    },
    isLiked: false,
  },
];

export function generateMockIdeas(count: number = 20): IdeaData[] {
  const ideas: IdeaData[] = [];

  for (let i = 0; i < count; i++) {
    const baseIdea = SAMPLE_IDEAS[i % SAMPLE_IDEAS.length];
    ideas.push({
      ...baseIdea,
      id: `idea-${i + 1}`,
      // Add some randomization to make each instance unique
      stats: {
        likes: baseIdea.stats.likes + Math.floor(Math.random() * 50),
        comments: baseIdea.stats.comments + Math.floor(Math.random() * 10),
        views: baseIdea.stats.views + Math.floor(Math.random() * 500),
      },
      isLiked: Math.random() > 0.7, // 30% chance of being liked
    });
  }

  return ideas;
}

export function getTimeAgo(timestamp: string): string {
  // This is a simple mock function - in a real app you'd calculate actual time differences
  const timeOptions = [
    "2m",
    "5m",
    "15m",
    "1h",
    "2h",
    "4h",
    "6h",
    "8h",
    "12h",
    "1d",
    "2d",
    "3d",
  ];
  return timeOptions[Math.floor(Math.random() * timeOptions.length)];
}
