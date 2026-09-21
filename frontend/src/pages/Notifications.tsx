import { useEffect, useState } from "react";
import {
  Heart,
  UserPlus,
  MessageCircle,
  Bell,
  CheckCheck,
  Loader2,
} from "lucide-react";
import {
  getNotifications,
  markNotificationAsRead,
} from "../api/NotificationApi";
import type { Notification, NotificationType } from "../types/Notification";

function formatRelativeTime(dateStr: string) {
  try {
    const date = new Date(dateStr);
    const now = new Date();
    const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (diffInSeconds < 60) return "Just now";
    const diffInMinutes = Math.floor(diffInSeconds / 60);
    if (diffInMinutes < 60) return `${diffInMinutes}m`;
    const diffInHours = Math.floor(diffInMinutes / 60);
    if (diffInHours < 24) return `${diffInHours}h`;
    const diffInDays = Math.floor(diffInHours / 24);
    if (diffInDays < 7) return `${diffInDays}d`;

    return date.toLocaleDateString(undefined, {
      month: "short",
      day: "numeric",
    });
  } catch {
    return dateStr;
  }
}

export default function Notifications() {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);

  const loadAll = async () => {
    setLoading(true);
    try {
      const list = await getNotifications();
      setNotifications(list);
    } catch (err) {
      console.error("Failed to load notifications:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAll();
  }, []);

  // When clicking on a notification, it just gets marked as read as per backend API
  const handleNotificationClick = async (notif: Notification) => {
    if (notif.read) return;

    // Optimistic update
    setNotifications((prev) =>
      prev.map((item) =>
        item.id === notif.id ? { ...item, read: true } : item
      )
    );

    try {
      await markNotificationAsRead(notif.id);
      window.dispatchEvent(new CustomEvent("notifications-updated"));
    } catch (err) {
      console.error("Failed to mark notification as read:", err);
    }
  };

  const handleMarkAllAsRead = async () => {
    const unreadList = notifications.filter((n) => !n.read);
    if (unreadList.length === 0) return;

    setNotifications((prev) =>
      prev.map((item) => ({ ...item, read: true }))
    );

    await Promise.all(
      unreadList.map((n) => markNotificationAsRead(n.id).catch(() => {}))
    );
    window.dispatchEvent(new CustomEvent("notifications-updated"));
  };

  const unreadCount = notifications.filter((n) => !n.read).length;

  const renderTypeIcon = (type: NotificationType) => {
    switch (type) {
      case "LIKE":
        return <Heart size={12} fill="#ef4444" color="#ef4444" />;
      case "FOLLOW":
        return <UserPlus size={12} color="#8b5cf6" />;
      case "COMMENT":
        return <MessageCircle size={12} color="#3b82f6" />;
      default:
        return <Bell size={12} />;
    }
  };

  return (
    <section className="notifications-page">
      {/* Header: Pure title and Mark all read button only (counts only in bottom navigation icon) */}
      <div className="notifications-header">
        <div className="notifications-title-row">
          <h1>Notifications</h1>

          {unreadCount > 0 && (
            <button
              type="button"
              className="mark-all-read-btn"
              onClick={handleMarkAllAsRead}
              title="Mark all as read"
            >
              <CheckCheck size={16} />
              <span>Mark all read</span>
            </button>
          )}
        </div>
      </div>

      {/* Notification List */}
      <div className="notification-list">
        {loading ? (
          <div className="notification-loading">
            <Loader2 size={22} className="spin-icon" />
            <p>Loading notifications...</p>
          </div>
        ) : notifications.length === 0 ? (
          <div className="notification-empty">
            <Bell size={32} className="empty-bell-icon" />
            <p>No notifications yet</p>
            <span>
              When someone likes, comments, or follows you, you will see it here.
            </span>
          </div>
        ) : (
          notifications.map((notif) => (
            <div
              key={notif.id}
              className={`notification-item ${!notif.read ? "unread" : ""}`}
              onClick={() => handleNotificationClick(notif)}
              title={notif.read ? "Marked as read" : "Click to mark as read"}
            >
              <div className="notification-avatar-wrapper">
                <div className="notification-avatar">
                  {notif.senderProfilePicture ? (
                    <img
                      src={notif.senderProfilePicture}
                      alt={notif.senderUsername}
                    />
                  ) : (
                    <span>
                      {notif.senderUsername
                        ? notif.senderUsername.charAt(0).toUpperCase()
                        : "U"}
                    </span>
                  )}
                </div>
                <div
                  className={`notification-badge-icon badge-${notif.type.toLowerCase()}`}
                >
                  {renderTypeIcon(notif.type)}
                </div>
              </div>

              <div className="notification-content">
                <div className="notification-text">
                  <strong>{notif.senderUsername}</strong>{" "}
                  <span>
                    {notif.message.replace(notif.senderUsername, "").trim()}
                  </span>
                </div>
                <span className="notification-time">
                  {formatRelativeTime(notif.createdAt)}
                </span>
              </div>

              {!notif.read && <div className="unread-dot" />}
            </div>
          ))
        )}
      </div>
    </section>
  );
}
