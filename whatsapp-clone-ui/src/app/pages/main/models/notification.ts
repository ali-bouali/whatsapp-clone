export interface Notification {
  chatId?: string;
  content?: string;
  senderId?: string;
  receiverId?: string;
  messageType?: 'TEXT' | 'IMAGE' | 'VIDEO' | 'AUDIO';
  type?: 'SEEN' | 'MESSAGE' | 'IMAGE' | 'VIDEO' | 'AUDIO' | 'PRIORITY_REMINDER';
  chatName?: string;
  media?: Array<string>;
  isPriority?: boolean;
  reminderIteration?: number;
}
