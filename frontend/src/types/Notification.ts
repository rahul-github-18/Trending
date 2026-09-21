export type NotificationType = "FOLLOW" | "LIKE" | "COMMENT";

export interface Notification {
  id: number;
  senderId: number;
  senderUsername: string;
  senderProfilePicture: string | null;
  type: NotificationType;
  targetId?: number;
  message: string;
  read: boolean;
  createdAt: string;
}
