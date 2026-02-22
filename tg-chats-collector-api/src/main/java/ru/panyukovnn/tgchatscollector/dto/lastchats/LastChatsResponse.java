package ru.panyukovnn.tgchatscollector.dto.lastchats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.panyukovnn.tgchatscollector.dto.ChatInfoDto;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LastChatsResponse {

    private List<ChatInfoDto> chats;
}