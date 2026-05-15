package ru.panyukovnn.tgchatscollector.dto.topics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Информация о топике форума
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatTopicDto {

    private Long topicId;
    private String title;
    /**
     * Признак того, что это служебный топик "General"
     */
    private boolean general;
}
