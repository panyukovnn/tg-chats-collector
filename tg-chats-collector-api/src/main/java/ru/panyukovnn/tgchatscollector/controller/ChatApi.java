package ru.panyukovnn.tgchatscollector.controller;

import ru.panyukovnn.tgchatscollector.dto.lastchats.LastChatsResponse;
import ru.panyukovnn.tgchatscollector.dto.searchchat.SearchChatsResponse;
import ru.panyukovnn.tgchatscollector.dto.searchchat.SearchPrivateChatRequest;
import ru.panyukovnn.tgchatscollector.dto.searchchat.SearchPublicChannelByIdRequest;

/**
 * Контракт для работы с чатами
 */
public interface ChatApi {

    /**
     * Получить последние N чатов
     *
     * @param count количество чатов
     * @return список последних чатов
     */
    LastChatsResponse getLastChats(Integer count);

    /**
     * Поиск приватного чата по части имени
     *
     * @param request запрос на поиск приватного чата
     * @return результат поиска чатов
     */
    SearchChatsResponse searchPrivateChat(SearchPrivateChatRequest request);

    /**
     * Поиск публичного канала по имени
     *
     * @param request запрос на поиск публичного канала
     * @return результат поиска чатов
     */
    SearchChatsResponse searchPublicChannel(SearchPublicChannelByIdRequest request);
}
