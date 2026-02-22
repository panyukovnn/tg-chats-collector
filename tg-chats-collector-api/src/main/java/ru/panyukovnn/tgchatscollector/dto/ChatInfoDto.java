package ru.panyukovnn.tgchatscollector.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatInfoDto {

    private Long chatId;
    private String type;
    private String title;
}