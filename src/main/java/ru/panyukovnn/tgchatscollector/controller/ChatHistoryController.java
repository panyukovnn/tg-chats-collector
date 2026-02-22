package ru.panyukovnn.tgchatscollector.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import ru.panyukovnn.tgchatscollector.dto.searchchathistory.SearchChatHistoryRequest;
import ru.panyukovnn.tgchatscollector.dto.searchchathistory.SearchChatHistoryResponse;
import ru.panyukovnn.tgchatscollector.service.handler.TgCollectorHandler;

/**
 * Контроллер для работы с историей сообщений
 */
@Path("/api/v1/chat-history")
public class ChatHistoryController {

    @Inject
    TgCollectorHandler tgCollectorHandler;

    /**
     * Поиск истории сообщений в чате
     *
     * @param request запрос на поиск истории сообщений
     * @return результат поиска истории
     */
    @POST
    @Path("/search")
    public SearchChatHistoryResponse searchHistory(SearchChatHistoryRequest request) {
        return tgCollectorHandler.handleSearchChatHistoryByPeriod(request);
    }
}