package ru.panyukovnn.tgchatscollector.dto.telegram;

public record TopicInfo(
    Boolean isGeneral,
    Long topicId,
    String title,
    Long lastMessageId) {
}
