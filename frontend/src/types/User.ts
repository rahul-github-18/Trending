export interface User {
  id: number;
  username: string;
  email: string;
  bio: string;
  profilePicture: string | null;
  createdAt: string;
  updatedAt?: string;
}