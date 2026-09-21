import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Flame, Loader2, UserPlus, LogIn, AlertCircle } from "lucide-react";
import { login } from "../api/AuthApi";
import { createUser } from "../api/UserApi";

interface LoginProps {
  onLoginSuccess?: (token: string) => void;
}

type AuthMode = "login" | "register";

export default function Login({ onLoginSuccess }: LoginProps) {
  const navigate = useNavigate();
  const [mode, setMode] = useState<AuthMode>("login");

  // Login form states
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  // Register form states
  const [regUsername, setRegUsername] = useState("");
  const [regEmail, setRegEmail] = useState("");
  const [regPassword, setRegPassword] = useState("");
  const [regBio, setRegBio] = useState("");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Handle Login
  const handleLogin = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const data = await login({
        username: username.trim(),
        password,
      });

      // Save JWT token
      localStorage.setItem("token", data.token);

      // Notify App state immediately so React Router re-renders authenticated routes without refreshing
      if (onLoginSuccess) {
        onLoginSuccess(data.token);
      } else {
        window.dispatchEvent(new Event("auth-token-changed"));
      }

      navigate("/");
    } catch (err: any) {
      console.error("Login failed:", err);
      setError(
        err.response?.data?.message ||
        err.message ||
        "Invalid username or password. Please try again."
      );
    } finally {
      setLoading(false);
    }
  };

  // Handle Create User / Register
  const handleRegister = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);

    // Client-side validations matching backend constraints
    if (regUsername.trim().length < 3 || regUsername.trim().length > 10) {
      setError("Username must be between 3 and 10 characters.");
      return;
    }

    if (regPassword.length < 4) {
      setError("Password must be at least 4 characters long.");
      return;
    }

    setLoading(true);

    try {
      // 1. Call backend to create user: POST /api/users
      await createUser({
        username: regUsername.trim(),
        email: regEmail.trim(),
        password: regPassword,
        bio: regBio.trim() || undefined,
      });

      // 2. Automatically log in with the new credentials
      const loginData = await login({
        username: regUsername.trim(),
        password: regPassword,
      });

      localStorage.setItem("token", loginData.token);

      if (onLoginSuccess) {
        onLoginSuccess(loginData.token);
      } else {
        window.dispatchEvent(new Event("auth-token-changed"));
      }

      navigate("/");
    } catch (err: unknown) {
      console.error("User registration failed:", err);
      setError("Could not create user. Username or email may already exist.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-form-container">
        {/* Brand Header */}
        <div className="auth-brand-header">
          <div className="brand-logo-badge auth-badge">
            <Flame size={24} />
          </div>
          <h1 className="brand-title auth-brand-title">Trending</h1>
          <p className="auth-tagline">
            {mode === "login"
              ? "Sign in to see updates and conversations"
              : "Create an account to join the community"}
          </p>
        </div>

        {/* Tab Switcher */}
        <div className="auth-tabs">
          <button
            type="button"
            className={`auth-tab ${mode === "login" ? "active" : ""}`}
            onClick={() => {
              setMode("login");
              setError(null);
            }}
          >
            <LogIn size={15} />
            <span>Log in</span>
          </button>

          <button
            type="button"
            className={`auth-tab ${mode === "register" ? "active" : ""}`}
            onClick={() => {
              setMode("register");
              setError(null);
            }}
          >
            <UserPlus size={15} />
            <span>Create account</span>
          </button>
        </div>

        {/* Error Notice */}
        {error && (
          <div className="auth-error-banner">
            <AlertCircle size={16} />
            <span>{error}</span>
          </div>
        )}

        {/* Login Form */}
        {mode === "login" ? (
          <form className="auth-form" onSubmit={handleLogin}>
            <div className="auth-input-group">
              <label htmlFor="login-username">Username</label>
              <input
                id="login-username"
                type="text"
                placeholder="Enter your username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                disabled={loading}
              />
            </div>

            <div className="auth-input-group">
              <label htmlFor="login-password">Password</label>
              <input
                id="login-password"
                type="password"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                disabled={loading}
              />
            </div>

            <button
              type="submit"
              className="auth-submit-btn"
              disabled={loading || !username.trim() || !password}
            >
              {loading ? (
                <>
                  <Loader2 size={16} className="spin-icon" />
                  <span>Logging in...</span>
                </>
              ) : (
                <span>Log in</span>
              )}
            </button>

            <div className="auth-footer-toggle">
              <span>Don&apos;t have an account?</span>{" "}
              <button
                type="button"
                className="toggle-link-btn"
                onClick={() => {
                  setMode("register");
                  setError(null);
                }}
              >
                Create one
              </button>
            </div>
          </form>
        ) : (
          /* Register Form */
          <form className="auth-form" onSubmit={handleRegister}>
            <div className="auth-input-group">
              <label htmlFor="reg-username">Username (3-10 chars)</label>
              <input
                id="reg-username"
                type="text"
                placeholder="e.g. rahul1"
                value={regUsername}
                onChange={(e) => setRegUsername(e.target.value)}
                required
                minLength={3}
                maxLength={10}
                disabled={loading}
              />
            </div>

            <div className="auth-input-group">
              <label htmlFor="reg-email">Email</label>
              <input
                id="reg-email"
                type="email"
                placeholder="name@example.com"
                value={regEmail}
                onChange={(e) => setRegEmail(e.target.value)}
                required
                disabled={loading}
              />
            </div>

            <div className="auth-input-group">
              <label htmlFor="reg-password">Password (min 4 chars)</label>
              <input
                id="reg-password"
                type="password"
                placeholder="Create a password"
                value={regPassword}
                onChange={(e) => setRegPassword(e.target.value)}
                required
                minLength={4}
                disabled={loading}
              />
            </div>

            <div className="auth-input-group">
              <label htmlFor="reg-bio">Bio (optional)</label>
              <input
                id="reg-bio"
                type="text"
                placeholder="Tell us something about you"
                value={regBio}
                onChange={(e) => setRegBio(e.target.value)}
                disabled={loading}
              />
            </div>

            <button
              type="submit"
              className="auth-submit-btn"
              disabled={
                loading ||
                !regUsername.trim() ||
                !regEmail.trim() ||
                regPassword.length < 4
              }
            >
              {loading ? (
                <>
                  <Loader2 size={16} className="spin-icon" />
                  <span>Creating user...</span>
                </>
              ) : (
                <span>Create user &amp; enter</span>
              )}
            </button>

            <div className="auth-footer-toggle">
              <span>Already have an account?</span>{" "}
              <button
                type="button"
                className="toggle-link-btn"
                onClick={() => {
                  setMode("login");
                  setError(null);
                }}
              >
                Log in
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}