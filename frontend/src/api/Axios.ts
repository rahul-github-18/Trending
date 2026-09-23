import axios from "axios";
import { isTokenExpired, clearAuthAndRedirect } from "../utils/auth";

// When accessing via ngrok or remote host, use relative base path so Vite proxy forwards requests
// to the backend without HTTPS mixed-content errors or cross-device connection failures.
const isLocalhost =
  typeof window !== "undefined" &&
  (window.location.hostname === "localhost" ||
    window.location.hostname === "127.0.0.1");

const envBaseUrl = import.meta.env.VITE_API_URL;

const api = axios.create({
  baseURL: envBaseUrl ? envBaseUrl : (isLocalhost ? "http://localhost:8080" : ""),
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");

  if (token) {
    if (isTokenExpired(token)) {
      clearAuthAndRedirect();
      return Promise.reject(new axios.Cancel("JWT expired"));
    }
    config.headers.Authorization = `Bearer ${token}`;
  }

  config.headers["ngrok-skip-browser-warning"] = "true";

  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // If request failed with 401 Unauthorized or 403 Forbidden (except credentials check at /api/auth/login)
    const isLoginEndpoint = error.config?.url?.includes("/api/auth/login");
    if (
      !isLoginEndpoint &&
      error.response &&
      (error.response.status === 401 || error.response.status === 403)
    ) {
      clearAuthAndRedirect();
    }
    return Promise.reject(error);
  }
);

export default api;
