import { useState, useEffect } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import "./App.css";

import Login from "./pages/Login";
import Profile from "./pages/Profile";
import Threads from "./pages/Threads";
import Search from "./pages/Search";
import Notifications from "./pages/Notifications";

import Sidebar from "./components/Sidebar";
import {
  getValidToken,
  getTokenRemainingTime,
  clearAuthAndRedirect,
} from "./utils/auth";

function App() {
  const [token, setToken] = useState<string | null>(() => getValidToken());

  useEffect(() => {
    const handleAuthTokenChanged = () => {
      setToken(getValidToken());
    };

    window.addEventListener("auth-token-changed", handleAuthTokenChanged);
    window.addEventListener("storage", handleAuthTokenChanged);

    // Also check token validity when tab becomes active / focused again
    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        const valid = getValidToken();
        if (!valid && localStorage.getItem("token")) {
          clearAuthAndRedirect();
        } else {
          setToken(valid);
        }
      }
    };
    document.addEventListener("visibilitychange", handleVisibilityChange);
    window.addEventListener("focus", handleVisibilityChange);

    return () => {
      window.removeEventListener("auth-token-changed", handleAuthTokenChanged);
      window.removeEventListener("storage", handleAuthTokenChanged);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      window.removeEventListener("focus", handleVisibilityChange);
    };
  }, []);

  // Automatic logout timer when token expires
  useEffect(() => {
    if (!token) return;

    const remainingTime = getTokenRemainingTime(token);
    if (remainingTime <= 0) {
      clearAuthAndRedirect();
      return;
    }

    const timer = setTimeout(() => {
      clearAuthAndRedirect();
    }, remainingTime);

    return () => clearTimeout(timer);
  }, [token]);

  const handleLoginSuccess = (newToken: string) => {
    localStorage.setItem("token", newToken);
    setToken(newToken);
    window.dispatchEvent(new Event("auth-token-changed"));
  };

  return (
    <BrowserRouter>
      {token ? (
        <div className="app">
          <Sidebar />

          <main className="main-content">
            <Routes>
              <Route path="/" element={<Threads />} />
              <Route path="/threads" element={<Navigate to="/" />} />
              <Route path="/search" element={<Search />} />
              <Route path="/notifications" element={<Notifications />} />
              <Route path="/profile" element={<Profile />} />
              <Route path="/login" element={<Navigate to="/" />} />
              <Route path="*" element={<Navigate to="/" />} />
            </Routes>
          </main>
        </div>
      ) : (
        <Routes>
          <Route
            path="/login"
            element={<Login onLoginSuccess={handleLoginSuccess} />}
          />
          <Route path="*" element={<Navigate to="/login" />} />
        </Routes>
      )}
    </BrowserRouter>
  );
}

export default App;