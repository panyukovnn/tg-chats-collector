package ru.panyukovnn.tgchatscollector.service.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.panyukovnn.tgchatscollector.dto.lastchats.ChatWithPreviewDto;
import ru.panyukovnn.tgchatscollector.dto.lastchats.LastChatsWithPreviewResponse;
import ru.panyukovnn.tgchatscollector.service.TgClientService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Тесты обработчика {@link TgCollectorHandler}
 */
@ExtendWith(MockitoExtension.class)
class TgCollectorHandlerTest {

    private static final Integer DEFAULT_COUNT = 10;

    @Mock
    private TgClientService tgClientService;

    @InjectMocks
    private TgCollectorHandler tgCollectorHandler;

    @Test
    void shouldReturnLastChatsWithPreviewFromService() {
        ChatWithPreviewDto chat = ChatWithPreviewDto.builder()
            .chatId(100L)
            .type("private")
            .title("Иван Петров")
            .lastMessageText("Привет")
            .lastMessageSenderName("Иван Петров")
            .lastMessageDateTime(LocalDateTime.of(2026, 5, 15, 12, 0))
            .build();

        when(tgClientService.findLastChatsWithPreview(DEFAULT_COUNT))
            .thenReturn(List.of(chat));

        LastChatsWithPreviewResponse response = tgCollectorHandler.handleLastChatsWithPreview(DEFAULT_COUNT);

        assertThat(response).isNotNull();
        assertThat(response.getChats())
            .hasSize(1)
            .first()
            .satisfies(actual -> {
                assertThat(actual.getChatId()).isEqualTo(100L);
                assertThat(actual.getTitle()).isEqualTo("Иван Петров");
                assertThat(actual.getLastMessageText()).isEqualTo("Привет");
                assertThat(actual.getLastMessageSenderName()).isEqualTo("Иван Петров");
            });
    }

    @Test
    void shouldReturnEmptyResponseWhenServiceReturnsEmptyList() {
        when(tgClientService.findLastChatsWithPreview(DEFAULT_COUNT))
            .thenReturn(List.of());

        LastChatsWithPreviewResponse response = tgCollectorHandler.handleLastChatsWithPreview(DEFAULT_COUNT);

        assertThat(response).isNotNull();
        assertThat(response.getChats()).isEmpty();
    }
}
