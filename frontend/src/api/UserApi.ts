import api from "./Axios";
import type { User } from "../types/User";

export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  bio?: string;
}

export interface UpdateUserRequest {
  username: string;
  email: string;
  bio: string;
}

const isLocalhost =
  typeof window !== "undefined" &&
  (window.location.hostname === "localhost" ||
    window.location.hostname === "127.0.0.1");

const envBaseUrl = import.meta.env.VITE_API_URL;
const backendOrigin = envBaseUrl
  ? envBaseUrl.replace(/\/$/, "")
  : (isLocalhost ? "http://localhost:8080" : "");

export const normalizeUser = (user: User): User => {
  if (!user) return user;
  let pic = user.profilePicture;
  if (pic && typeof pic === "string") {
    if (
      pic.startsWith("http://") ||
      pic.startsWith("https://") ||
      pic.startsWith("data:") ||
      pic.startsWith("blob:")
    ) {
      // Already an absolute URL (e.g. S3 bucket, Cloudinary) or base64 / blob preview
    } else {
      const cleanPath = pic.startsWith("/") ? pic : `/${pic}`;
      pic = backendOrigin ? `${backendOrigin}${cleanPath}` : cleanPath;
    }
  }
  return {
    ...user,
    profilePicture: pic,
  };
};

export const createUser = async (
  data: CreateUserRequest
): Promise<User> => {
  const response = await api.post<User>("/api/users", data);
  return normalizeUser(response.data);
};

export const getCurrentUser = async (): Promise<User> => {
  const response = await api.get<User>("/api/users/me");
  return normalizeUser(response.data);
};

export const updateCurrentUser = async (
  data: UpdateUserRequest
): Promise<User> => {
  const response = await api.put<User>("/api/users/me", data);
  return normalizeUser(response.data);
};

export const uploadProfilePicture = async (
  file: File
): Promise<User> => {
  const formData = new FormData();
  formData.append("file", file);

  const response = await api.post<User>(
    "/api/users/profile-picture",
    formData
  );

  return normalizeUser(response.data);
};

export const getFollowersCount = async (
  userId: number
): Promise<number> => {
  const response = await api.get<number>(
    `/api/users/${userId}/followers/count`
  );
  return response.data;
};

export const getFollowingCount = async (
  userId: number
): Promise<number> => {
  const response = await api.get<number>(
    `/api/users/${userId}/following/count`
  );
  return response.data;
};

// Follow user
export const followUser = async (userId: number): Promise<string> => {
  const response = await api.post<string>(`/api/users/${userId}/follow`);
  return response.data;
};

// Unfollow user
export const unfollowUser = async (userId: number): Promise<string> => {
  const response = await api.post<string>(`/api/users/${userId}/unfollow`);
  return response.data;
};

// Check if current user is following target user
export const getFollowingStatus = async (
  userId: number
): Promise<boolean> => {
  const response = await api.get<boolean>(
    `/api/users/${userId}/following/status`
  );
  return response.data;
};

// Get followers list
export const getFollowers = async (userId: number): Promise<User[]> => {
  const response = await api.get<User[]>(`/api/users/${userId}/followers`);
  return Array.isArray(response.data) ? response.data.map(normalizeUser) : [];
};

// Get following list
export const getFollowing = async (userId: number): Promise<User[]> => {
  const response = await api.get<User[]>(`/api/users/${userId}/following`);
  return Array.isArray(response.data) ? response.data.map(normalizeUser) : [];
};

// Search users by typing character(s)
export const searchUsers = async (text: string): Promise<User[]> => {
  const response = await api.get<User[]>("/api/users/search", {
    params: { text },
  });
  return Array.isArray(response.data) ? response.data.map(normalizeUser) : [];
};