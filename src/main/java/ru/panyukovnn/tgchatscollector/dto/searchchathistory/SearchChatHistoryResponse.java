package ru.panyukovnn.tgchatscollector.dto.searchchathistory;

import lombok.Builder;
import lombok.Data;
import ru.panyukovnn.tgchatscollector.dto.TgMessageDto;

import java.util.List;

@Data
@Builder
public class SearchChatHistoryResponse {

    private Long chatId;
    private String chatTitle;
    private String chatPublicName;
    private String topicName;
    private Long topicId;
    private Integer totalCount;
    private List<TgMessageDto> messages;
}
