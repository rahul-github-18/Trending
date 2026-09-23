import api from "./Axios";
import { clearAuthAndRedirect } from "../utils/auth";

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
}

export const login = async (
  data: LoginRequest
): Promise<LoginResponse> => {
  const response = await api.post<LoginResponse>(
    "/api/auth/login",
    data
  );

  return response.data;
};

export const logout = async (): Promise<void> => {
  try {
    await api.post("/api/auth/logout");
  } catch (error) {
    console.error("Logout request error:", error);
  } finally {
    clearAuthAndRedirect();
  }
};