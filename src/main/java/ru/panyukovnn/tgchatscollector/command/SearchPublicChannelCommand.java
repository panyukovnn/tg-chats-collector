package ru.panyukovnn.tgchatscollector.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import ru.panyukovnn.tgchatscollector.dto.searchchat.SearchChatsResponse;
import ru.panyukovnn.tgchatscollector.dto.searchchat.SearchPublicChannelByIdRequest;
import ru.panyukovnn.tgchatscollector.service.handler.TgCollectorHandler;

@Slf4j
@CommandLine.Command(
    name = "search-public-channel",
    description = "Поиск публичного канала по полному имени"
)
public class SearchPublicChannelCommand implements Runnable {

    @CommandLine.Option(
        names = {"-n", "--name"},
        description = "Полное имя публичного чата/канала (начинается с @)",
        required = true
    )
    String publicChatName;

    @Inject
    TgCollectorHandler tgCollectorHandler;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void run() {
        try {
            log.info("Executing search-public-channel command with name={}", publicChatName);

            SearchPublicChannelByIdRequest request = SearchPublicChannelByIdRequest.builder()
                .publicChatName(publicChatName)
                .build();

            SearchChatsResponse response = tgCollectorHandler.handleFindPublicChannelById(request);

            String json = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(response);

            System.out.println(json);

            log.info("Successfully found {} chats", response.getChats().size());
        } catch (Exception e) {
            log.error("Error executing search-public-channel command", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
