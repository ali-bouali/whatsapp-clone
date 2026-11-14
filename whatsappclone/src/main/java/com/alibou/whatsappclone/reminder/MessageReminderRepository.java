package com.alibou.whatsappclone.reminder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageReminderRepository extends JpaRepository<MessageReminder, Long> {

    List<MessageReminder> findByActiveTrueAndNextRunAtLessThanEqual(LocalDateTime dateTime);

    @Modifying
    @Query("UPDATE MessageReminder r SET r.active = false WHERE r.messageId = :messageId")
    void deactivateByMessageId(@Param("messageId") Long messageId);

    @Modifying
    @Query("UPDATE MessageReminder r SET r.active = false WHERE r.chatId = :chatId AND r.receiverId = :receiverId")
    void deactivateAllForChatAndReceiver(@Param("chatId") String chatId, @Param("receiverId") String receiverId);
}
