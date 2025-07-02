export interface Idea {
  title: string;
  description: string;
  categories: Category[];
  userId: string;
}

export type Category =
  | "MOBILE_APP"
  | "AI"
  | "FIN_TECH"
  | "WEB3"
  | "HEALTH"
  | "EDUCATION"
  | "GAMING"
  | "SAAS"
  | "E_COMMERCE"
  | "SOCIAL"
  | "PRODUCTIVITY"
  | "ENTERTAINMENT";

export interface ScrapedIdea {
  id?: number;
  project_name: string;
  likes: number;
  submitted_to?: string;
  winner: boolean;
  created_by: string;
  technologies: string[];
  short_description?: string;
  solution?: string;
  problem_description?: string;
  technical_details?: string;
}

export interface Technology {
  id?: number;
  name: string;
}