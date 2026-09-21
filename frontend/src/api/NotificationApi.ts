import api from "./Axios";
import type { Notification } from "../types/Notification";

export const getNotifications = async (): Promise<Notification[]> => {
  const response = await api.get<Notification[]>("/api/notifications");
  return Array.isArray(response.data) ? response.data : [];
};

export const getUnreadNotificationCount = async (): Promise<number> => {
  const response = await api.get<number>("/api/notifications/unread/count");
  return typeof response.data === "number" ? response.data : 0;
};

export const markNotificationAsRead = async (
  notificationId: number
): Promise<boolean> => {
  const response = await api.put<boolean>(
    `/api/notifications/${notificationId}/read`
  );
  return !!response.data;
};
