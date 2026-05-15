package ru.panyukovnn.tgchatscollector.dto.lastchats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Информация о чате с предпросмотром последнего сообщения
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatWithPreviewDto {

    private Long chatId;
    private String type;
    private String title;
    /**
     * Признак того, что чат является форумом с топиками
     */
    private boolean forum;
    /**
     * Текст последнего сообщения в чате (либо маркер вложения), может отсутствовать у пустых чатов
     */
    private String lastMessageText;
    /**
     * Отображаемое имя отправителя последнего сообщения
     */
    private String lastMessageSenderName;
    /**
     * Дата и время последнего сообщения (МСК)
     */
    private LocalDateTime lastMessageDateTime;
}