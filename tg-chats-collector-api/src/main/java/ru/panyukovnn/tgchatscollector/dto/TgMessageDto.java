package ru.panyukovnn.tgchatscollector.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TgMessageDto {

    private Long messageId;
    private Long senderId;
    private LocalDateTime dateTime;
    private String text;
    /**
     * Текст сообщения, на который отвечает текущее сообщение
     */
    private String replyToText;
    private Long replyToMessageId;
}