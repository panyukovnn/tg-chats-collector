package ru.panyukovnn.tgchatscollector.controller;

import ru.panyukovnn.tgchatscollector.dto.searchchathistory.SearchChatHistoryRequest;
import ru.panyukovnn.tgchatscollector.dto.searchchathistory.SearchChatHistoryResponse;

/**
 * Контракт для работы с историей сообщений
 */
public interface ChatHistoryApi {

    /**
     * Поиск истории сообщений в чате
     *
     * @param request запрос на поиск истории сообщений
     * @return результат поиска истории
     */
    SearchChatHistoryResponse searchHistory(SearchChatHistoryRequest request);
}
