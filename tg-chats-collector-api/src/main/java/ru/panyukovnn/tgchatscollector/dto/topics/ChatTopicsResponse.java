package ru.panyukovnn.tgchatscollector.dto.topics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ответ со списком топиков форума
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatTopicsResponse {

    private List<ChatTopicDto> topics;
}
