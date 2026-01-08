package ru.panyukovnn.tgchatscollector.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import ru.panyukovnn.tgchatscollector.dto.searchchathistory.SearchChatHistoryRequest;
import ru.panyukovnn.tgchatscollector.dto.searchchathistory.SearchChatHistoryResponse;
import ru.panyukovnn.tgchatscollector.service.handler.TgCollectorHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@CommandLine.Command(
    name = "search-history",
    description = "Поиск истории сообщений в чате начиная с заданной даты"
)
public class SearchChatHistoryCommand implements Runnable {

    @CommandLine.Option(
        names = {"--chat-id"},
        description = "Идентификатор чата",
        required = true
    )
    Long chatId;

    @CommandLine.Option(
        names = {"--topic-id"},
        description = "Идентификатор топика (опционально)"
    )
    Long topicId;

    @CommandLine.Option(
        names = {"--from"},
        description = "Дата начала периода в формате ISO (yyyy-MM-ddTHH:mm:ss), в UTC",
        required = true
    )
    String dateFromStr;

    @Inject
    TgCollectorHandler tgCollectorHandler;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void run() {
        try {
            log.info("Executing search-history command with chatId={}, topicId={}, from={}",
                    chatId, topicId, dateFromStr);

            LocalDateTime dateFrom = LocalDateTime.parse(
                dateFromStr,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            );

            SearchChatHistoryRequest request = SearchChatHistoryRequest.builder()
                .chatId(chatId)
                .topicId(topicId)
                .dateFrom(dateFrom)
                .build();

            SearchChatHistoryResponse response = tgCollectorHandler.handleSearchChatHistoryByPeriod(request);

            String json = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(response);

            System.out.println(json);

            log.info("Successfully retrieved {} messages", response.getTotalCount());
        } catch (Exception e) {
            log.error("Error executing search-history command", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
