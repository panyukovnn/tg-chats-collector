package ru.panyukovnn.tgchatscollector.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tg_messages")
public class TgMessage extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Идентификатор чата
     */
    private Long chatId;
    /**
     * Идентификатор топика
     */
    private Long topicId;
    /**
     * Идентификатор сообщения тг
     */
    private Long externalId;
    /**
     * Дата и время публикации сообщения в UTC
     */
    private LocalDateTime dateTime;
    /**
     * Идентификатор отправителя
     */
    private Long senderId;
    /**
     * Текст, изображение, видео
     */
    @Enumerated(EnumType.STRING)
    private TgMessageType type;
    /**
     * Содержимое сообщения
     */
    private String content;
    /**
     * Текст сообщения, на который отвечает текущее сообщение
     */
    private String replyToText;
    /**
     * Идентификатор сообщения, на которое отвечает текущее сообщение
     */
    private Long replyToMessageId;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TgMessage subscribedChannel = (TgMessage) o;
        return Objects.equals(id, subscribedChannel.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
