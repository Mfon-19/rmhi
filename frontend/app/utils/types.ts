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
