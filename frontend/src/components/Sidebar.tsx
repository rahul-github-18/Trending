import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  Home,
  Search,
  Heart,
  User,
  LogOut,
  Flame,
} from "lucide-react";
import { logout } from "../api/AuthApi";
import { getUnreadNotificationCount } from "../api/NotificationApi";

function Sidebar() {
  const location = useLocation();
  const [unreadCount, setUnreadCount] = useState<number>(0);

  useEffect(() => {
    let isMounted = true;

    const fetchUnread = async () => {
      try {
        const count = await getUnreadNotificationCount();
        if (isMounted) setUnreadCount(count);
      } catch {
        // Silently ignore if not logged in
      }
    };

    fetchUnread();

    const handleNotificationsUpdated = () => {
      fetchUnread();
    };

    window.addEventListener("notifications-updated", handleNotificationsUpdated);

    // Poll every 15s to update unread badge in background
    const interval = setInterval(fetchUnread, 15000);

    return () => {
      isMounted = false;
      clearInterval(interval);
      window.removeEventListener("notifications-updated", handleNotificationsUpdated);
    };
  }, [location.pathname]);

  const handleLogout = async () => {
    const confirmed = window.confirm("Are you sure you want to log out?");
    if (!confirmed) return;
    await logout();
  };

  return (
    <>
      {/* Top Header with Unique Igniter Brand */}
      <header className="threads-top-bar">
        <Link to="/" className="brand-logo-link" title="Igniter">
          <div className="brand-logo-badge">
            <Flame size={20} />
          </div>
          <span className="brand-title">Trending</span>
        </Link>
      </header>

      {/* Bottom Navigation Bar */}
      <nav className="threads-bottom-nav">
        <div className="bottom-nav-container">
          <Link
            to="/"
            className={`nav-item ${location.pathname === "/" ? "active" : ""}`}
            title="Threads Feed"
          >
            <Home size={26} strokeWidth={location.pathname === "/" ? 2.5 : 1.8} />
          </Link>

          <Link
            to="/search"
            className={`nav-item ${location.pathname === "/search" ? "active" : ""}`}
            title="Search"
          >
            <Search size={26} strokeWidth={location.pathname === "/search" ? 2.5 : 1.8} />
          </Link>

          <Link
            to="/notifications"
            className={`nav-item nav-activity ${location.pathname === "/notifications" ? "active" : ""
              }`}
            title="Notifications"
          >
            <div className="nav-icon-container">
              <Heart
                size={26}
                strokeWidth={location.pathname === "/notifications" ? 2.5 : 1.8}
                fill={location.pathname === "/notifications" ? "currentColor" : "none"}
              />
              {unreadCount > 0 && (
                <span className="nav-count-badge">
                  {unreadCount > 99 ? "99+" : unreadCount}
                </span>
              )}
            </div>
          </Link>

          <Link
            to="/profile"
            className={`nav-item ${location.pathname === "/profile" ? "active" : ""}`}
            title="Profile"
          >
            <User size={26} strokeWidth={location.pathname === "/profile" ? 2.5 : 1.8} />
          </Link>

          <button
            type="button"
            className="nav-item nav-logout"
            onClick={handleLogout}
            title="Log out"
          >
            <LogOut size={24} strokeWidth={1.8} />
          </button>
        </div>
      </nav>
    </>
  );
}

export default Sidebar;