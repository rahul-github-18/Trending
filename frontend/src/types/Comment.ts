export interface Comment {
  id: number;
  content: string;
  username: string;
  parentCommentId?: number | null;
  createdAt: string;
  updatedAt?: string;
}
