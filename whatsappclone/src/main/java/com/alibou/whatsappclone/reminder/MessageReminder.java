package com.alibou.whatsappclone.reminder;

import com.alibou.whatsappclone.common.BaseAuditingEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "message_reminders", indexes = {
        @Index(name = "idx_reminder_active_next_run", columnList = "is_active, next_run_at")
})
public class MessageReminder extends BaseAuditingEntity {

    @Id
    @SequenceGenerator(name = "reminder_seq", sequenceName = "reminder_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reminder_seq")
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "receiver_id", nullable = false)
    private String receiverId;

    @Column(name = "chat_id", nullable = false)
    private String chatId;

    @Column(name = "next_run_at", nullable = false)
    private LocalDateTime nextRunAt;

    @Column(name = "interval_minutes", nullable = false)
    private int intervalMinutes;

    @Column(name = "remaining_repeats", nullable = false)
    private int remainingRepeats;

    @Column(name = "total_iterations_sent", nullable = false)
    private int totalIterationsSent;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
