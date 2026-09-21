import api from "./Axios";
import type { Thread } from "../types/Thread";

export interface CreateThreadRequest {
  content: string;
}

export interface UpdateThreadRequest {
  content: string;
}

export interface LikeResponse {
  liked: boolean;
  likeCount: number;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  last: boolean;
  first: boolean;
  empty: boolean;
}

interface SpringPageRaw<T> {
  content?: T[];
  totalPages?: number;
  totalElements?: number;
  number?: number;
  size?: number;
  last?: boolean;
  first?: boolean;
  empty?: boolean;
}

const parsePageResponse = <T>(data: unknown, fallbackPage = 0, fallbackSize = 10): PageResponse<T> => {
  if (Array.isArray(data)) {
    return {
      content: data,
      totalPages: 1,
      totalElements: data.length,
      number: fallbackPage,
      size: fallbackSize,
      last: true,
      first: true,
      empty: data.length === 0,
    };
  }

  if (data && typeof data === "object") {
    const raw = data as SpringPageRaw<T>;
    const content = Array.isArray(raw.content) ? raw.content : [];
    const isLast = typeof raw.last === "boolean" ? raw.last : content.length < fallbackSize;

    return {
      content,
      totalPages: typeof raw.totalPages === "number" ? raw.totalPages : 1,
      totalElements: typeof raw.totalElements === "number" ? raw.totalElements : content.length,
      number: typeof raw.number === "number" ? raw.number : fallbackPage,
      size: typeof raw.size === "number" ? raw.size : fallbackSize,
      last: isLast,
      first: typeof raw.first === "boolean" ? raw.first : fallbackPage === 0,
      empty: typeof raw.empty === "boolean" ? raw.empty : content.length === 0,
    };
  }

  return {
    content: [],
    totalPages: 0,
    totalElements: 0,
    number: fallbackPage,
    size: fallbackSize,
    last: true,
    first: true,
    empty: true,
  };
};

export interface CursorPageResponse<T> {
  content: T[];
  nextCursor: string | null;
  hasNext: boolean;
}

// Get feed threads using cursor-based pagination (api/threads/feed)
export const getFeedCursorPage = async (
  cursor?: string | null,
  size = 10
): Promise<CursorPageResponse<Thread>> => {
  const params: Record<string, unknown> = { size };
  if (cursor) {
    params.cursor = cursor;
  }

  const response = await api.get<CursorPageResponse<Thread>>("/api/threads/feed", {
    params,
  });

  return {
    content: Array.isArray(response.data?.content) ? response.data.content : [],
    nextCursor: response.data?.nextCursor || null,
    hasNext: !!response.data?.hasNext,
  };
};

// Backward-compatible getFeedPage
export const getFeedPage = async (page = 0, size = 10): Promise<PageResponse<Thread>> => {
  const res = await getFeedCursorPage(null, size);
  return {
    content: res.content,
    totalPages: res.hasNext ? page + 2 : page + 1,
    totalElements: res.content.length,
    number: page,
    size,
    last: !res.hasNext,
    first: page === 0,
    empty: res.content.length === 0,
  };
};

// Backward-compatible getFeedThreads
export const getFeedThreads = async (_page = 0, size = 10): Promise<Thread[]> => {
  const res = await getFeedCursorPage(null, size);
  return res.content;
};

// Get offset-paginated threads from backend (GET /api/threads)
export const getThreadsPage = async (
  page = 0,
  size = 10,
  username?: string
): Promise<PageResponse<Thread>> => {
  const params: Record<string, unknown> = {
    page,
    size,
    sort: "id,desc",
  };
  if (username) {
    params.username = username;
  }
  const response = await api.get<unknown>("/api/threads", { params });
  return parsePageResponse<Thread>(response.data, page, size);
};

// Get all threads (Explore)
export const getThreads = async (page = 0, size = 30): Promise<Thread[]> => {
  const parsed = await getThreadsPage(page, size);
  return parsed.content;
};

// Get all threads created by a specific user
export const getUserThreads = async (username: string): Promise<Thread[]> => {
  const target = username.trim().toLowerCase();
  const collected: Thread[] = [];
  const seenIds = new Set<number>();
  let page = 0;
  const pageSize = 200;
  let totalPages = 1;

  while (page < totalPages && page < 6) {
    try {
      const response = await api.get<unknown>("/api/threads", {
        params: { page, size: pageSize, sort: "id,desc" },
      });

      const parsed = parsePageResponse<Thread>(response.data, page, pageSize);
      totalPages = parsed.totalPages || 1;

      for (const t of parsed.content) {
        if (!seenIds.has(t.id) && t.username?.trim().toLowerCase() === target) {
          seenIds.add(t.id);
          collected.push(t);
        }
      }

      if (parsed.last || page >= totalPages - 1) break;
      page++;
    } catch (err) {
      console.error("Error fetching page of user threads:", err);
      break;
    }
  }

  return collected;
};

// Get all threads across system (for finding liked threads)
export const getAllThreadsList = async (): Promise<Thread[]> => {
  const response = await api.get<unknown>("/api/threads", {
    params: { page: 0, size: 500, sort: "id,desc" },
  });

  const parsed = parsePageResponse<Thread>(response.data, 0, 500);
  return parsed.content;
};

// Get single thread by ID
export const getThreadById = async (id: number): Promise<Thread> => {
  const response = await api.get<Thread>(`/api/threads/${id}`);
  return response.data;
};

// Create thread
export const createThread = async (
  data: CreateThreadRequest
): Promise<Thread> => {
  const response = await api.post<Thread>("/api/threads/create", data);
  return response.data;
};

// Update thread
export const updateThread = async (
  id: number,
  data: UpdateThreadRequest
): Promise<Thread> => {
  const response = await api.put<Thread>(`/api/threads/${id}`, data);
  return response.data;
};

// Delete thread
export const deleteThread = async (id: number): Promise<void> => {
  await api.delete(`/api/threads/${id}`);
};

// Like thread
export const likeThread = async (threadId: number): Promise<LikeResponse> => {
  const response = await api.post<LikeResponse>(`/api/threads/${threadId}/like`);
  return response.data;
};

// Unlike thread
export const unlikeThread = async (threadId: number): Promise<LikeResponse> => {
  const response = await api.delete<LikeResponse>(`/api/threads/${threadId}/like`);
  return response.data;
};