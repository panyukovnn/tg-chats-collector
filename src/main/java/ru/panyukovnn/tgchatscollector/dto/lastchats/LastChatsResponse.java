package ru.panyukovnn.tgchatscollector.dto.lastchats;

import lombok.Builder;
import ru.panyukovnn.tgchatscollector.dto.ChatInfoDto;

import java.util.List;

@Builder
public record LastChatsResponse(
    List<ChatInfoDto> chats
) {
}