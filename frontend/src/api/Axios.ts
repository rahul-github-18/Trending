import axios from "axios";

// When accessing via ngrok or remote host, use relative base path so Vite proxy forwards requests
// to the backend without HTTPS mixed-content errors or cross-device connection failures.
const isLocalhost =
  typeof window !== "undefined" &&
  (window.location.hostname === "localhost" ||
    window.location.hostname === "127.0.0.1");

const api = axios.create({
  baseURL: isLocalhost ? "http://localhost:8080" : "",
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  config.headers["ngrok-skip-browser-warning"] = "true";

  return config;
});

export default api;