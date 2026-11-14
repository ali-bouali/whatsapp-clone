package com.alibou.whatsappclone.message;

import com.alibou.whatsappclone.file.FileUtils;
import org.springframework.stereotype.Service;

@Service
public class MessageMapper {
    public MessageResponse toMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .type(message.getType())
                .state(message.getState())
                .createdAt(message.getCreatedDate())
                .media(FileUtils.readFileFromLocation(message.getMediaFilePath()))
                .isPriority(message.isPriority())
                .reminderIntervalMinutes(message.getReminderIntervalMinutes())
                .reminderMaxRepeats(message.getReminderMaxRepeats())
                .build();
    }
}
