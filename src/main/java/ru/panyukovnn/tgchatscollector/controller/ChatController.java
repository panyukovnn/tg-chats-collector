package ru.panyukovnn.tgchatscollector.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import ru.panyukovnn.tgchatscollector.dto.lastchats.LastChatsResponse;
import ru.panyukovnn.tgchatscollector.dto.searchchat.SearchChatsResponse;
import ru.panyukovnn.tgchatscollector.dto.searchchat.SearchPrivateChatRequest;
import ru.panyukovnn.tgchatscollector.dto.searchchat.SearchPublicChannelByIdRequest;
import ru.panyukovnn.tgchatscollector.service.handler.TgCollectorHandler;

/**
 * Контроллер для работы с чатами
 */
@Path("/api/v1/chats")
public class ChatController {

    @Inject
    TgCollectorHandler tgCollectorHandler;

    /**
     * Получить последние N чатов
     *
     * @param count количество чатов
     * @return список последних чатов
     */
    @GET
    @Path("/last")
    public LastChatsResponse getLastChats(@QueryParam("count") Integer count) {
        return tgCollectorHandler.handleLastChats(count);
    }

    /**
     * Поиск приватного чата по части имени
     *
     * @param request запрос на поиск приватного чата
     * @return результат поиска чатов
     */
    @POST
    @Path("/search-private")
    public SearchChatsResponse searchPrivateChat(SearchPrivateChatRequest request) {
        return tgCollectorHandler.handleFindPrivateChat(request);
    }

    /**
     * Поиск публичного канала по имени
     *
     * @param request запрос на поиск публичного канала
     * @return результат поиска чатов
     */
    @POST
    @Path("/search-public")
    public SearchChatsResponse searchPublicChannel(SearchPublicChannelByIdRequest request) {
        return tgCollectorHandler.handleFindPublicChannelById(request);
    }
}