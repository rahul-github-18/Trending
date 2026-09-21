export interface Thread {
  id: number;
  username: string;
  content: string;
  createdAt: string;
  updatedAt?: string;

  likeCount: number;

  // Frontend state
  liked?: boolean;
}