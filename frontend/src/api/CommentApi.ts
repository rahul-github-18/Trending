import api from "./Axios";
import type { Comment } from "../types/Comment";

export interface CreateCommentRequest {
  content: string;
  parentCommentId?: number | null;
}

export const getComments = async (threadId: number): Promise<Comment[]> => {
  const response = await api.get<Comment[]>(
    `/api/threads/${threadId}/comments`
  );
  return Array.isArray(response.data) ? response.data : [];
};

export const createComment = async (
  threadId: number,
  content: string,
  parentCommentId?: number | null
): Promise<Comment> => {
  const response = await api.post<Comment>(
    `/api/threads/${threadId}/comments`,
    {
      content,
      parentCommentId: parentCommentId ?? null,
    }
  );
  return response.data;
};

export const updateComment = async (
  commentId: number,
  content: string
): Promise<Comment> => {
  const response = await api.put<Comment>(
    `/api/threads/comments/${commentId}`,
    { content }
  );
  return response.data;
};

export const deleteComment = async (commentId: number): Promise<string> => {
  const response = await api.delete<string>(
    `/api/threads/comments/${commentId}`
  );
  return response.data;
};
