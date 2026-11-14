package com.alibou.whatsappclone.reminder;

import com.alibou.whatsappclone.chat.Chat;
import com.alibou.whatsappclone.chat.ChatRepository;
import com.alibou.whatsappclone.message.Message;
import com.alibou.whatsappclone.message.MessageRepository;
import com.alibou.whatsappclone.message.MessageState;
import com.alibou.whatsappclone.notification.Notification;
import com.alibou.whatsappclone.notification.NotificationService;
import com.alibou.whatsappclone.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageReminderScheduler {

    private final MessageReminderRepository reminderRepository;
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void processReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<MessageReminder> pendingReminders = reminderRepository.findByActiveTrueAndNextRunAtLessThanEqual(now);

        for (MessageReminder reminder : pendingReminders) {
            Message message = messageRepository.findById(reminder.getMessageId()).orElse(null);

            if (message == null || message.getState() == MessageState.SEEN) {
                reminder.setActive(false);
                reminderRepository.save(reminder);
                continue;
            }

            int iteration = reminder.getTotalIterationsSent() + 1;
            log.info("Sending priority reminder #{} for message {} to user {}",
                    iteration, reminder.getMessageId(), reminder.getReceiverId());

            Chat chat = chatRepository.findById(reminder.getChatId()).orElse(null);
            String chatName = chat != null ? chat.getTargetChatName(reminder.getSenderId()) : "Important Message";

            Notification reminderNotification = Notification.builder()
                    .chatId(reminder.getChatId())
                    .messageType(message.getType())
                    .content("[Reminder " + iteration + "] " + message.getContent())
                    .senderId(reminder.getSenderId())
                    .receiverId(reminder.getReceiverId())
                    .type(NotificationType.PRIORITY_REMINDER)
                    .chatName(chatName)
                    .isPriority(true)
                    .reminderIteration(iteration)
                    .build();

            notificationService.sendNotification(reminder.getReceiverId(), reminderNotification);

            reminder.setTotalIterationsSent(iteration);
            reminder.setRemainingRepeats(reminder.getRemainingRepeats() - 1);

            if (reminder.getRemainingRepeats() <= 0) {
                reminder.setActive(false);
            } else {
                reminder.setNextRunAt(now.plusMinutes(reminder.getIntervalMinutes()));
            }

            reminderRepository.save(reminder);
        }
    }
}
