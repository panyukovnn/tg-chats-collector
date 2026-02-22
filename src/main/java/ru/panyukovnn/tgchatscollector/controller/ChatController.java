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
public class ChatController implements ChatApi {

    @Inject
    TgCollectorHandler tgCollectorHandler;

    @GET
    @Path("/last")
    @Override
    public LastChatsResponse getLastChats(@QueryParam("count") Integer count) {
        return tgCollectorHandler.handleLastChats(count);
    }

    @POST
    @Path("/search-private")
    @Override
    public SearchChatsResponse searchPrivateChat(SearchPrivateChatRequest request) {
        return tgCollectorHandler.handleFindPrivateChat(request);
    }

    @POST
    @Path("/search-public")
    @Override
    public SearchChatsResponse searchPublicChannel(SearchPublicChannelByIdRequest request) {
        return tgCollectorHandler.handleFindPublicChannelById(request);
    }
}