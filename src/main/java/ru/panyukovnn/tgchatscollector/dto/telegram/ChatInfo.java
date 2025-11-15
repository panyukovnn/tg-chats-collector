package ru.panyukovnn.tgchatscollector.dto.telegram;

public record ChatInfo(
    Long chatId,
    String chatPublicName,
    String type,
    String title) {
}
