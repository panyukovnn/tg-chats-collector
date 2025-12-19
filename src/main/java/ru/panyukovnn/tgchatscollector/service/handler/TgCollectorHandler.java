package ru.panyukovnn.tgchatscollector.service.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.panyukovnn.referencemodelstarter.exception.BusinessException;
import ru.panyukovnn.tgchatscollector.dto.ChatInfoDto;
import ru.panyukovnn.tgchatscollector.dto.TgMessageDto;
import ru.panyukovnn.tgchatscollector.dto.lastchats.LastChatsResponse;
import ru.panyukovnn.tgchatscollector.dto.searchchat.SearchChatsResponse;
import ru.panyukovnn.tgchatscollector.dto.searchchat.SearchPrivateChatRequest;
import ru.panyukovnn.tgchatscollector.dto.searchchat.SearchPublicChannelByIdRequest;
import ru.panyukovnn.tgchatscollector.dto.searchchathistory.SearchChatHistoryRequest;
import ru.panyukovnn.tgchatscollector.dto.searchchathistory.SearchChatHistoryResponse;
import ru.panyukovnn.tgchatscollector.dto.telegram.ChatInfo;
import ru.panyukovnn.tgchatscollector.dto.telegram.TopicInfo;
import ru.panyukovnn.tgchatscollector.service.TgClientService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TgCollectorHandler {

    private static final int SEARCH_CHATS_LIMIT = 5;

    private final TgClientService tgClientService;

    public LastChatsResponse handleLastChats(Integer count) {
        List<ChatInfoDto> lastChatDtos = tgClientService.findLastChats(count);

        return new LastChatsResponse(lastChatDtos);
    }

    public SearchChatsResponse handleFindPrivateChat(SearchPrivateChatRequest searchRequest) {
        String privateChatNamePart = searchRequest.getPrivateChatNamePart();
        String topicNamePart = searchRequest.getTopicNamePart();

        List<ChatInfo> chats = tgClientService.searchChats(null, null, privateChatNamePart);

        if (CollectionUtils.isEmpty(chats)) {
            throw new BusinessException("31ff", "Не удалось найти ни одного чата по заданным параметрам");
        }

        return createSearchChatResponse(chats, topicNamePart);
    }

    public SearchChatsResponse handleFindPublicChannelById(SearchPublicChannelByIdRequest searchPublicChannelByIdRequest) {
        String publicChatName = searchPublicChannelByIdRequest.getPublicChatName();

        List<ChatInfo> chats = tgClientService.searchChats(null, publicChatName, null);

        if (CollectionUtils.isEmpty(chats)) {
            throw new BusinessException("31ff", "Не удалось найти ни одного чата по заданным параметрам");
        }

        return createSearchChatResponse(chats, null);
    }

    /**
     * Функционал намеренно предельно ограничен, можно грузить либо из бд, либо с последней даты, непосредственно из тг
     *
     * @param searchChatHistoryRequest запрос на поиск сообщений
     * @return история сообщений
     */
    public SearchChatHistoryResponse handleSearchChatHistoryByPeriod(SearchChatHistoryRequest searchChatHistoryRequest) {
        Long chatId = searchChatHistoryRequest.getChatId();
        Long topicId = searchChatHistoryRequest.getTopicId();
        LocalDateTime dateFrom = searchChatHistoryRequest.getDateFrom();

        ChatInfo chatInfo = tgClientService.findChatById(chatId);
        TopicInfo topicInfo = tgClientService.findTopicInfoById(chatId, topicId);

        List<TgMessageDto> messageDtos = tgClientService.collectAllMessagesFromPublicChat(chatId, topicInfo, null, dateFrom, null).stream()
            .sorted(Comparator.comparing(TgMessageDto::getDateTime))
            .toList();

        return SearchChatHistoryResponse.builder()
            .chatId(chatInfo.chatId())
            .chatTitle(chatInfo.title())
            .chatPublicName(chatInfo.chatPublicName())
            .topicId(topicInfo != null ? topicInfo.topicId() : null)
            .topicName(topicInfo != null ? topicInfo.title() : null)
            .totalCount(messageDtos.size())
            .messages(messageDtos)
            .build();
    }

    private SearchChatsResponse createSearchChatResponse(List<ChatInfo> chats, String topicNamePart) {
        List<SearchChatsResponse.ChatInfo> chatInfos = chats.stream()
            .map(chat -> {
                SearchChatsResponse.ChatInfo chatInfo = new SearchChatsResponse.ChatInfo();
                chatInfo.setId(chat.chatId());
                chatInfo.setTitle(chat.title());
                chatInfo.setType(chat.type());

                if (StringUtils.hasText(topicNamePart)) {
                    List<SearchChatsResponse.TopicInfo> topics = tgClientService.findTopicsByName(chat.chatId(), topicNamePart).stream()
                        .map(ts -> new SearchChatsResponse.TopicInfo(ts.topicId(), ts.title()))
                        .toList();

                    chatInfo.setTopics(topics);
                }

                return chatInfo;
            })
            .limit(SEARCH_CHATS_LIMIT)
            .toList();

        return new SearchChatsResponse(chatInfos);
    }
}
