export function parseJwtPayload(token: string): any | null {
  try {
    const parts = token.split(".");
    if (parts.length < 2) return null;
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

export function isTokenExpired(token: string | null): boolean {
  if (!token) return true;
  const payload = parseJwtPayload(token);
  if (!payload || typeof payload.exp !== "number") {
    return true;
  }
  // exp is in seconds, Date.now() is in ms. Add 2s grace window for clock skew.
  return Date.now() >= payload.exp * 1000 - 2000;
}

export function getTokenRemainingTime(token: string | null): number {
  if (!token) return 0;
  const payload = parseJwtPayload(token);
  if (!payload || typeof payload.exp !== "number") return 0;
  const remaining = payload.exp * 1000 - Date.now();
  return remaining > 0 ? remaining : 0;
}

export function getValidToken(): string | null {
  const token = localStorage.getItem("token");
  if (!token) return null;
  if (isTokenExpired(token)) {
    localStorage.removeItem("token");
    return null;
  }
  return token;
}

export function clearAuthAndRedirect(): void {
  localStorage.removeItem("token");
  window.dispatchEvent(new Event("auth-token-changed"));
  if (window.location.pathname !== "/login") {
    window.location.href = "/login";
  }
}
