package ru.panyukovnn.tgchatscollector.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import ru.panyukovnn.tgchatscollector.dto.lastchats.LastChatsResponse;
import ru.panyukovnn.tgchatscollector.service.handler.TgCollectorHandler;

@Slf4j
@CommandLine.Command(
    name = "last-chats",
    description = "Получить список последних N чатов из Telegram"
)
public class LastChatsCommand implements Runnable {

    @CommandLine.Option(
        names = {"-c", "--count"},
        description = "Количество чатов для получения (по умолчанию: 10)",
        defaultValue = "10"
    )
    Integer count;

    @Inject
    TgCollectorHandler tgCollectorHandler;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void run() {
        try {
            log.info("Executing last-chats command with count={}", count);

            LastChatsResponse response = tgCollectorHandler.handleLastChats(count);

            String json = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(response);

            System.out.println(json);

            log.info("Successfully retrieved {} chats", response.chats().size());
        } catch (Exception e) {
            log.error("Error executing last-chats command", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
