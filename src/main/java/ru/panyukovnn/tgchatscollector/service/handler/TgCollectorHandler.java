package ru.panyukovnn.tgchatscollector.service.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.panyukovnn.tgchatscollector.dto.ChatInfoDto;
import ru.panyukovnn.tgchatscollector.dto.TgMessageDto;
import ru.panyukovnn.tgchatscollector.dto.lastchats.LastChatsResponse;
import ru.panyukovnn.tgchatscollector.dto.searchchat.SearchChatRequest;
import ru.panyukovnn.tgchatscollector.dto.searchchat.SearchChatsResponse;
import ru.panyukovnn.tgchatscollector.dto.searchchathistory.SearchChatHistoryRequest;
import ru.panyukovnn.tgchatscollector.dto.searchchathistory.SearchChatHistoryResponse;
import ru.panyukovnn.tgchatscollector.dto.telegram.ChatInfo;
import ru.panyukovnn.tgchatscollector.dto.telegram.TopicInfo;
import ru.panyukovnn.tgchatscollector.exception.TgChatsCollectorException;
import ru.panyukovnn.tgchatscollector.mapper.TgMessageMapper;
import ru.panyukovnn.tgchatscollector.model.TgMessage;
import ru.panyukovnn.tgchatscollector.service.TgClientService;
import ru.panyukovnn.tgchatscollector.service.domain.TgMessagesDomainService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TgCollectorHandler {

    private final TgClientService tgClientService;
    private final TgMessageMapper tgMessageMapper;
    private final TgMessagesDomainService tgMessagesDomainService;

    public LastChatsResponse handleLastChats(Integer count) {
        List<ChatInfoDto> lastChatDtos = tgClientService.findLastChats(count);

        return new LastChatsResponse(lastChatDtos);
    }

    public SearchChatsResponse handleFindChat(SearchChatRequest searchChatRequest) {
        String publicChatName = searchChatRequest.getPublicChatName();
        String privateChatNamePart = searchChatRequest.getPrivateChatNamePart();
        String topicNamePart = searchChatRequest.getTopicNamePart();

        if (!StringUtils.hasText(publicChatName)
            && !StringUtils.hasText(privateChatNamePart)
            && !StringUtils.hasText(topicNamePart)) {
            throw new IllegalArgumentException("publicChatName, privateChatNamePart, topicNamePart не могут быть одновременно пустыми");
        }

        List<ChatInfo> chats = tgClientService.findChats(null, publicChatName, privateChatNamePart);

        if (CollectionUtils.isEmpty(chats)) {
            throw new TgChatsCollectorException("31ff", "Не удалось найти ни одного чата по заданным параметрам");
        }

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
            .toList();

        return new SearchChatsResponse(chatInfos);
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

        if (Boolean.TRUE.equals(searchChatHistoryRequest.getReturnFromDb())) {
            List<TgMessageDto> messagesFromDate = tgMessagesDomainService.findMessagesFromDate(chatId, topicId, dateFrom).stream()
                .sorted(Comparator.comparing(TgMessage::getDateTime))
                .map(tgMessageMapper::toDto)
                .toList();

            return SearchChatHistoryResponse.builder()
                .chatId(chatInfo.chatId())
                .chatTitle(chatInfo.title())
                .chatPublicName(chatInfo.chatPublicName())
                .topicId(topicInfo != null ? topicInfo.topicId() : null)
                .topicName(topicInfo != null ? topicInfo.title() : null)
                .totalCount(messagesFromDate.size())
                .messages(messagesFromDate)
                .build();
        }

        List<TgMessageDto> messageDtos = tgClientService.collectAllMessagesFromPublicChat(chatId, topicInfo, null, dateFrom, null).stream()
            .sorted(Comparator.comparing(TgMessageDto::getDateTime))
            .toList();

        tgMessagesDomainService.saveMessages(chatId, topicId, messageDtos);

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
}
