package ru.panyukovnn.tgchatscollector.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import ru.panyukovnn.tgchatscollector.dto.searchchat.SearchChatsResponse;
import ru.panyukovnn.tgchatscollector.dto.searchchat.SearchPrivateChatRequest;
import ru.panyukovnn.tgchatscollector.service.handler.TgCollectorHandler;

@Slf4j
@CommandLine.Command(
    name = "search-private-chat",
    description = "Поиск приватного чата по части имени"
)
public class SearchPrivateChatCommand implements Runnable {

    @CommandLine.Option(
        names = {"-n", "--name"},
        description = "Часть имени приватного чата для поиска",
        required = true
    )
    String privateChatNamePart;

    @CommandLine.Option(
        names = {"-t", "--topic"},
        description = "Часть имени топика в форуме (опционально)"
    )
    String topicNamePart;

    @Inject
    TgCollectorHandler tgCollectorHandler;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public void run() {
        try {
            log.info("Executing search-private-chat command with name={}, topic={}",
                    privateChatNamePart, topicNamePart);

            SearchPrivateChatRequest request = SearchPrivateChatRequest.builder()
                .privateChatNamePart(privateChatNamePart)
                .topicNamePart(topicNamePart)
                .build();

            SearchChatsResponse response = tgCollectorHandler.handleFindPrivateChat(request);

            String json = objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(response);

            System.out.println(json);

            log.info("Successfully found {} chats", response.getChats().size());
        } catch (Exception e) {
            log.error("Error executing search-private-chat command", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
