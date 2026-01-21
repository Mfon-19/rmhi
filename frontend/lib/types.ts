export interface Idea {
  id?: number;
  projectName: string;
  likes: number;
  createdBy: string;
  technologies: Technology[] | string[];
  categories: Category[] | string[];
  rating: number;
  shortDescription?: string;
  solution?: string;
  comments?: Comment[];
  problemDescription?: string;
  technicalDetails?: string;
}

export interface Comment {
  id: number;
  content: string;
  ideaId: number;
  userId: number;
}

export interface Category {
  id: number;
  name: string;
}

export interface Technology {
  id: number;
  name: string;
}
