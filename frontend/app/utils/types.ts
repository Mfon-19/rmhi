export interface Idea {
  project_name: string;
  likes: number;
  created_by: string;
  technologies: string[];
  categories: string[];
  rating: number;
  short_description?: string;
  solution?: string;
  problem_description?: string;
  technical_details?: string;
}