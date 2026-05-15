package ru.panyukovnn.tgchatscollector.dto.lastchats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ответ со списком последних чатов и предпросмотром последнего сообщения в каждом
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LastChatsWithPreviewResponse {

    private List<ChatWithPreviewDto> chats;
}