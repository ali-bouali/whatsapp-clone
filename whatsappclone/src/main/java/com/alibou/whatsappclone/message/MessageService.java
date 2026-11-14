package com.alibou.whatsappclone.message;

import com.alibou.whatsappclone.chat.Chat;
import com.alibou.whatsappclone.chat.ChatRepository;
import com.alibou.whatsappclone.file.FileService;
import com.alibou.whatsappclone.file.FileUtils;
import com.alibou.whatsappclone.notification.Notification;
import com.alibou.whatsappclone.notification.NotificationService;
import com.alibou.whatsappclone.notification.NotificationType;
import com.alibou.whatsappclone.reminder.MessageReminder;
import com.alibou.whatsappclone.reminder.MessageReminderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final MessageMapper mapper;
    private final NotificationService notificationService;
    private final FileService fileService;
    private final MessageReminderRepository reminderRepository;

    public void saveMessage(MessageRequest messageRequest) {
        Chat chat = chatRepository.findById(messageRequest.getChatId())
                .orElseThrow(() -> new EntityNotFoundException("Chat not found"));

        Message message = new Message();
        message.setContent(messageRequest.getContent());
        message.setChat(chat);
        message.setSenderId(messageRequest.getSenderId());
        message.setReceiverId(messageRequest.getReceiverId());
        message.setType(messageRequest.getType());
        message.setState(MessageState.SENT);
        message.setPriority(messageRequest.isPriority());
        message.setReminderIntervalMinutes(messageRequest.getReminderIntervalMinutes());
        message.setReminderMaxRepeats(messageRequest.getReminderMaxRepeats());

        messageRepository.save(message);

        if (message.isPriority() && message.getReminderMaxRepeats() != null && message.getReminderMaxRepeats() > 0) {
            int interval = (message.getReminderIntervalMinutes() != null && message.getReminderIntervalMinutes() > 0)
                    ? message.getReminderIntervalMinutes()
                    : 30;

            MessageReminder reminder = MessageReminder.builder()
                    .messageId(message.getId())
                    .senderId(message.getSenderId())
                    .receiverId(message.getReceiverId())
                    .chatId(chat.getId())
                    .intervalMinutes(interval)
                    .remainingRepeats(message.getReminderMaxRepeats())
                    .totalIterationsSent(0)
                    .nextRunAt(LocalDateTime.now().plusMinutes(interval))
                    .active(true)
                    .build();

            reminderRepository.save(reminder);
        }

        Notification notification = Notification.builder()
                .chatId(chat.getId())
                .messageType(messageRequest.getType())
                .content(messageRequest.getContent())
                .senderId(messageRequest.getSenderId())
                .receiverId(messageRequest.getReceiverId())
                .type(NotificationType.MESSAGE)
                .chatName(chat.getTargetChatName(message.getSenderId()))
                .isPriority(message.isPriority())
                .build();

        notificationService.sendNotification(messageRequest.getReceiverId(), notification);
    }

    public List<MessageResponse> findChatMessages(String chatId) {
        return messageRepository.findMessagesByChatId(chatId)
                .stream()
                .map(mapper::toMessageResponse)
                .toList();
    }

    @Transactional
    public void setMessagesToSeen(String chatId, Authentication authentication) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        final String recipientId = getRecipientId(chat, authentication);

        messageRepository.setMessagesToSeenByChatId(chatId, MessageState.SEEN);
        reminderRepository.deactivateAllForChatAndReceiver(chatId, authentication.getName());

        Notification notification = Notification.builder()
                .chatId(chat.getId())
                .type(NotificationType.SEEN)
                .receiverId(recipientId)
                .senderId(getSenderId(chat, authentication))
                .build();

        notificationService.sendNotification(recipientId, notification);
    }

    public void uploadMediaMessage(String chatId, MultipartFile file, Authentication authentication) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        final String senderId = getSenderId(chat, authentication);
        final String receiverId = getRecipientId(chat, authentication);

        final String filePath = fileService.saveFile(file, senderId);
        Message message = new Message();
        message.setReceiverId(receiverId);
        message.setSenderId(senderId);
        message.setState(MessageState.SENT);
        message.setType(MessageType.IMAGE);
        message.setMediaFilePath(filePath);
        message.setChat(chat);
        messageRepository.save(message);

        Notification notification = Notification.builder()
                .chatId(chat.getId())
                .type(NotificationType.IMAGE)
                .senderId(senderId)
                .receiverId(receiverId)
                .messageType(MessageType.IMAGE)
                .media(FileUtils.readFileFromLocation(filePath))
                .build();

        notificationService.sendNotification(receiverId, notification);
    }

    private String getSenderId(Chat chat, Authentication authentication) {
        if (chat.getSender().getId().equals(authentication.getName())) {
            return chat.getSender().getId();
        }
        return chat.getRecipient().getId();
    }

    private String getRecipientId(Chat chat, Authentication authentication) {
        if (chat.getSender().getId().equals(authentication.getName())) {
            return chat.getRecipient().getId();
        }
        return chat.getSender().getId();
    }
}
