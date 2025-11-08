package ru.panyukovnn.tgchatscollector.dto;

import lombok.Builder;

@Builder
public record ChatInfoDto(
    Long chatId,
    String type,
    String title
) {
}
