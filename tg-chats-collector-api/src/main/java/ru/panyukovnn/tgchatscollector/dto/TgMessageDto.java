package ru.panyukovnn.tgchatscollector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TgMessageDto {

    private Long messageId;
    private Long senderId;
    /**
     * Отображаемое имя отправителя (для пользователя — имя и фамилия; для чата — название)
     */
    private String senderName;
    /**
     * Публичный username отправителя без префикса @ (для пользователя), null для чатов и пользователей без username
     */
    private String senderUsername;
    private LocalDateTime dateTime;
    private String text;
    /**
     * Текст сообщения, на который отвечает текущее сообщение
     */
    private String replyToText;
    private Long replyToMessageId;
    /**
     * Содержимое приложенного фото в base64 (image/jpeg), заполняется только если запрошено через attachPhoto
     */
    private String photoBase64;
}