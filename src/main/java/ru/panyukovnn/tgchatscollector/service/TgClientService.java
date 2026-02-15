package ru.panyukovnn.tgchatscollector.service;

import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.panyukovnn.tgchatscollector.dto.ChatInfoDto;
import ru.panyukovnn.tgchatscollector.dto.TgMessageDto;
import ru.panyukovnn.tgchatscollector.dto.telegram.ChatInfo;
import ru.panyukovnn.tgchatscollector.dto.telegram.TopicInfo;
import ru.panyukovnn.tgchatscollector.exception.BusinessException;
import ru.panyukovnn.tgchatscollector.property.TgChatLoaderProperty;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

@Slf4j
@ApplicationScoped
public class TgClientService {

    @Inject
    SimpleTelegramClient tgClient;

    @Inject
    TgChatLoaderProperty tgChatLoaderProperty;

    @SneakyThrows
    public ChatInfo searchChats(Long chatId, String publicChatName) {
        if (chatId == null && publicChatName == null) {
            throw new BusinessException("46ea", "Отсутствуют chatId и chatName для идентификации чата");
        }

        TdApi.Chat chat = chatId != null
            ? tgClient.send(new TdApi.GetChat(chatId)).get()
            : tgClient.send(new TdApi.SearchPublicChat(publicChatName)).get();


        String chatType = defineChatType(chat);

        return new ChatInfo(chat.id, fetchChannelPublicName(chat), chatType, chat.title);
    }

    @SneakyThrows
    public ChatInfo findChatById(Long chatId) {
        TdApi.Chat chat = tgClient.send(new TdApi.GetChat(chatId)).get();

        return new ChatInfo(chat.id, fetchChannelPublicName(chat), chat.type.getClass().getSimpleName(), chat.title);
    }

    /**
     * @param chatId              идентификатор чата
     * @param publicChatName      полное публичное имя (например имя канала)
     * @param privateChatNamePart часть имени приватного чата
     * @return чат
     */
    public List<ChatInfo> searchChats(Long chatId, String publicChatName, String privateChatNamePart) {
        if (chatId != null || (publicChatName != null && !publicChatName.isEmpty())) {
            return List.of(searchChats(chatId, publicChatName));
        }

        if (privateChatNamePart != null) {
            return findPersonalChatByNamePart(privateChatNamePart);
        }

        return List.of();
    }

    /**
     * Фасадный метод для сбора сообщений из чата
     * Важно, чтобы даты передавались в UTC
     *
     * @param chatId   идентификатор чата
     * @param topic    топик
     * @param limit    предельное количество сообщений
     * @param dateFrom дата начала периода в UTC
     * @param dateTo   дата окончания периода в UTC
     * @return список сообщений из чата
     */
    @SneakyThrows
    public List<TgMessageDto> collectMessages(Long chatId,
                                              TopicInfo topic,
                                              @Nullable Integer limit,
                                              @Nullable LocalDateTime dateFrom,
                                              @Nullable LocalDateTime dateTo) {
        // Открываем чат, чтобы TDLib синхронизировал последние сообщения из облака
        tgClient.send(new TdApi.OpenChat(chatId)).get();

        try {
            if (dateFrom != null) {
                return collectMessagesByDateRange(chatId, topic, 0L, dateFrom, dateTo);
            }

            int effectiveLimit = limit != null ? limit : tgChatLoaderProperty.defaultMessagesLimit();

            return collectMessagesByLimit(chatId, topic, 0L, effectiveLimit);
        } finally {
            tgClient.send(new TdApi.CloseChat(chatId)).get();
        }
    }


    /**
     * Собирает последние N сообщений из чата
     *
     * @param chatId идентификатор чата
     * @param topic  топик (может быть null)
     * @param limit  количество сообщений для сбора
     * @return список сообщений
     */
    public List<TgMessageDto> collectMessagesByLimit(Long chatId, TopicInfo topic, long lastMessageId, int limit) {
        List<TgMessageDto> result = new ArrayList<>();
        Set<Long> loadedMessageIds = new HashSet<>();
        long fromMessageId = lastMessageId;

        while (result.size() < limit && !Thread.interrupted()) {
            TdApi.Messages messages = fetchChatMessagesBatch(chatId, topic, fromMessageId);

            if (isEmptyBatch(messages)) {
                log.info("Достигнут конец истории чата, собрано сообщений: {}", result.size());

                break;
            }

            long oldestMessageIdInBatch = processMessagesBatch(
                messages, topic, null, null, limit, result, loadedMessageIds
            );

            if (oldestMessageIdInBatch == fromMessageId) {
                log.info("Пагинация завершена, нет новых сообщений, собрано: {}", result.size());

                break;
            }

            fromMessageId = oldestMessageIdInBatch;
            log.debug("Загружена пачка из {} сообщений, всего собрано: {}", messages.messages.length, result.size());
        }

        log.info("Сбор сообщений завершен, всего извлечено: {}", result.size());

        return result;
    }

    /**
     * Собирает сообщения за указанный период
     *
     * @param chatId   идентификатор чата
     * @param topic    топик (может быть null)
     * @param dateFrom дата начала периода в UTC (включительно)
     * @param dateTo   дата окончания периода в UTC (включительно, может быть null)
     * @return список сообщений за период
     */
    // TODO здесь тоже нужен лимит
    public List<TgMessageDto> collectMessagesByDateRange(Long chatId,
                                                         TopicInfo topic,
                                                         long lastMessageId,
                                                         LocalDateTime dateFrom,
                                                         @Nullable LocalDateTime dateTo) {
        List<TgMessageDto> result = new ArrayList<>();
        Set<Long> loadedMessageIds = new HashSet<>();
        long fromMessageId = lastMessageId;
        boolean reachedDateFrom = false;

        while (!reachedDateFrom && !Thread.interrupted()) {
            TdApi.Messages messages = fetchChatMessagesBatch(chatId, topic, fromMessageId);

            if (isEmptyBatch(messages)) {
                log.info("Достигнут конец истории чата, собрано сообщений: {}", result.size());

                break;
            }

            long oldestMessageIdInBatch = processMessagesBatch(
                messages, topic, dateFrom, dateTo, Integer.MAX_VALUE, result, loadedMessageIds
            );

            // Проверяем, достигли ли мы даты начала периода
            TdApi.Message oldestMessage = messages.messages[messages.messages.length - 1];
            LocalDateTime oldestMessageDate = LocalDateTime.ofEpochSecond(oldestMessage.date, 0, ZoneOffset.UTC);

            if (oldestMessageDate.isBefore(dateFrom)) {
                reachedDateFrom = true;
                log.info("Достигнута дата начала периода: {}", dateFrom);
            }

            if (oldestMessageIdInBatch == fromMessageId) {
                log.info("Пагинация завершена, нет новых сообщений, собрано: {}", result.size());

                break;
            }

            fromMessageId = oldestMessageIdInBatch;
            log.debug("Загружена пачка, всего собрано: {}", result.size());
        }

        log.info("Сбор сообщений за период завершен, извлечено: {}", result.size());

        return result;
    }

    /**
     * Обрабатывает пачку сообщений и добавляет подходящие в результат
     *
     * @return ID самого старого сообщения в пачке для пагинации
     */
    private long processMessagesBatch(TdApi.Messages messages,
                                      TopicInfo topic,
                                      @Nullable LocalDateTime dateFrom,
                                      @Nullable LocalDateTime dateTo,
                                      int limit,
                                      List<TgMessageDto> result,
                                      Set<Long> loadedMessageIds) {
        for (TdApi.Message message : messages.messages) {
            if (result.size() >= limit) {
                break;
            }

            if (loadedMessageIds.contains(message.id)) {
                continue;
            }

            loadedMessageIds.add(message.id);

            if (!isMessageFromTopic(message, topic)) {
                continue;
            }

            LocalDateTime messageDateUtc = LocalDateTime.ofEpochSecond(message.date, 0, ZoneOffset.UTC);

            if (dateFrom != null && messageDateUtc.isBefore(dateFrom)) {
                continue;
            }

            if (dateTo != null && messageDateUtc.isAfter(dateTo)) {
                continue;
            }

            TgMessageDto messageDto = mapToMessageDto(message);
            result.add(messageDto);
        }

        return messages.messages[messages.messages.length - 1].id;
    }

    /**
     * Проверяет, относится ли сообщение к указанному топику
     */
    private boolean isMessageFromTopic(TdApi.Message message, TopicInfo topic) {
        if (topic == null) {
            return true;
        }

        // Если это general топик, пропускаем сообщения из других топиков
        if (topic.isGeneral()) {
            return !message.isTopicMessage;
        }

        return true;
    }

    /**
     * Преобразует TdApi.Message в TgMessageDto
     *
     * @return DTO сообщения или null, если сообщение не содержит текста
     */
    private TgMessageDto mapToMessageDto(TdApi.Message message) {
        String text = extractMessageTextSafely(message.content);

        LocalDateTime messageDateTimeUtc = LocalDateTime.ofEpochSecond(message.date, 0, ZoneOffset.UTC);
        LocalDateTime messageDateTime = messageDateTimeUtc.plusHours(3); // Московское время

        ReplyToMessage replyToMessage = fetchReplyToMessage(message);

        return TgMessageDto.builder()
            .senderId(extractSenderId(message))
            .dateTime(messageDateTime)
            .messageId(message.id)
            .replyToText(replyToMessage != null ? replyToMessage.text : null)
            .replyToMessageId(replyToMessage != null ? replyToMessage.id : null)
            .text(text)
            .build();
    }

    private boolean isEmptyBatch(TdApi.Messages messages) {
        return messages == null || messages.messages == null || messages.messages.length == 0;
    }

    @SneakyThrows
    public List<ChatInfo> findPersonalChatByNamePart(String namePart) {
        TdApi.Chats mainChats = tgClient.send(new TdApi.GetChats(new TdApi.ChatListMain(), Integer.MAX_VALUE))
            .get();
        TdApi.Chats archiveChats = tgClient.send(new TdApi.GetChats(new TdApi.ChatListArchive(), Integer.MAX_VALUE))
            .get();

        List<CompletableFuture<TdApi.Chat>> chatsWithInfo = Stream.concat(Arrays.stream(mainChats.chatIds).boxed(), Arrays.stream(archiveChats.chatIds).boxed())
            .map(chatId -> tgClient.send(new TdApi.GetChat(chatId)))
            .toList();

        for (CompletableFuture<TdApi.Chat> chatCompletableFuture : chatsWithInfo) {
            chatCompletableFuture.get();
        }

        return chatsWithInfo.stream()
            .map(cf -> {
                try {
                    return cf.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            })
            .filter(it -> it.title.toLowerCase().contains(namePart.toLowerCase()))
            .map(chat -> {
                String chatPublicName = fetchChannelPublicName(chat);

                return new ChatInfo(chat.id, chatPublicName, defineChatType(chat), chat.title);
            })
            .toList();
    }

    @SneakyThrows
    public List<TopicInfo> findTopicsByName(long chatId, String topicNamePart) {
        return tgClient.send(new TdApi.GetForumTopics(chatId, topicNamePart, 0, 0L, 0L, 100))
            .thenApply(topics -> Arrays.stream(topics.topics)
                .filter(ft -> ft.info.name.toLowerCase()
                    .contains(topicNamePart.toLowerCase()))
                .map(topic -> new TopicInfo(topic.info.isGeneral, topic.info.messageThreadId, topic.info.name, topic.lastMessage.id))
                .toList())
            .get();
    }

    @Nullable
    @SneakyThrows
    public TopicInfo findTopicInfoById(long chatId, Long topicId) {
        if (topicId == null) {
            return null;
        }

        return tgClient.send(new TdApi.GetForumTopic(chatId, topicId))
            .thenApply(topic -> new TopicInfo(topic.info.isGeneral, topic.info.messageThreadId, topic.info.name, topic.lastMessage.id))
            .get();
    }

    @SneakyThrows
    public List<ChatInfoDto> findLastChats(Integer count) {
        TdApi.Chats chats = tgClient.send(new TdApi.GetChats(new TdApi.ChatListMain(), count))
            .get();

        List<ChatInfoDto> list = new ArrayList<>();
        for (long chatId : chats.chatIds) {
            CompletableFuture<TdApi.Chat> send = tgClient.send(new TdApi.GetChat(chatId));
            TdApi.Chat chat = send.get();
            ChatInfoDto chatInfoDto = new ChatInfoDto(chat.id, chat.type.getClass().getSimpleName(), chat.title);
            list.add(chatInfoDto);
        }

        return list;
    }

    private static Long extractSenderId(TdApi.Message message) {
        Long senderId = null;
        if (message.senderId instanceof TdApi.MessageSenderChat chatSender) {
            senderId = chatSender.chatId;
        } else if (message.senderId instanceof TdApi.MessageSenderUser userSender) {
            senderId = userSender.userId;
        }
        return senderId;
    }

    @Nullable
    private ReplyToMessage fetchReplyToMessage(TdApi.Message message) {
        if (message.replyTo instanceof TdApi.MessageReplyToMessage replyToMessageInfo) {
            try {
                TdApi.Message replyToMessage = tgClient.send(new TdApi.GetMessage(replyToMessageInfo.chatId, replyToMessageInfo.messageId))
                    .get();

                String messageText = extractMessageTextSafely(replyToMessage.content);

                return new ReplyToMessage(replyToMessage.id, messageText);
            } catch (InterruptedException | ExecutionException e) {
                return null;
            }
        }

        return null;
    }

    private String fetchChannelPublicName(TdApi.Chat chat) {
        if (chat.type instanceof TdApi.ChatTypeSupergroup supergroup) {
            try {
                TdApi.Supergroup supergroupInfo = tgClient.send(new TdApi.GetSupergroup(supergroup.supergroupId))
                    .get();

                return Optional.ofNullable(supergroupInfo.usernames)
                    .map(usernames -> usernames.activeUsernames)
                    .filter(activeUsernames -> activeUsernames.length > 0)
                    .map(activeUsernames -> activeUsernames[0])
                    .orElse(null);
            } catch (InterruptedException | ExecutionException e) {
                log.warn("Не удалось получить информацию о супергруппе: {}", chat.title, e);
            }
        }

        return null;
    }

    /**
     * Загружает пачку сообщений из чата для пагинации
     *
     * @param chatId        идентификатор чата
     * @param topic         топик (может быть null)
     * @param fromMessageId ID сообщения, с которого начинать загрузку (0 для последних сообщений)
     * @return пачка сообщений
     */
    private TdApi.Messages fetchChatMessagesBatch(long chatId, TopicInfo topic, long fromMessageId) {
        try {
            // Если это general топик, читаем все сообщения и фильтруем те, что не в топиках
            TdApi.Function<TdApi.Messages> chatHistory = topic != null && !topic.isGeneral()
                ? new TdApi.GetMessageThreadHistory(chatId, topic.lastMessageId(), fromMessageId, 0, 100)
                : new TdApi.GetChatHistory(chatId, fromMessageId, 0, 100, false);

            return tgClient.send(chatHistory)
                .exceptionally(e -> {
                    log.error("Ошибка при выгрузке истории чата: {}", e.getMessage(), e);

                    throw new RuntimeException(e);
                })
                .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private String extractMessageTextSafely(TdApi.MessageContent content) {
        if (content instanceof TdApi.MessageText messageText) {
            return messageText.text.text;
        }

        if (content instanceof TdApi.MessagePhoto photo) {
            return buildMediaText("<Приложено фото>", photo.caption.text);
        }

        if (content instanceof TdApi.MessageVideo messageVideo) {
            return buildMediaText("<Приложено видео>", messageVideo.caption.text);
        }

        if (content instanceof TdApi.MessageAudio audio) {
            return buildMediaText("<Приложено аудио>", audio.caption.text);
        }

        if (content instanceof TdApi.MessageDocument document) {
            return buildMediaText("<Приложен документ>", document.caption.text);
        }

        if (content instanceof TdApi.MessageVoiceNote voiceNote) {
            return buildMediaText("<Приложено голосовое сообщение>", voiceNote.caption.text);
        }

        if (content instanceof TdApi.MessageVideoNote) {
            return "<Приложено видеосообщение>";
        }

        if (content instanceof TdApi.MessageSticker) {
            return "<Приложен стикер>";
        }

        if (content instanceof TdApi.MessageAnimation animation) {
            return buildMediaText("<Приложена анимация>", animation.caption.text);
        }

        return "<Неизвестный тип сообщения>";
    }

    private String buildMediaText(String mediaLabel, String caption) {
        if (caption == null || caption.isEmpty()) {
            return mediaLabel;
        }

        return mediaLabel + "\n" + caption;
    }

    private static String defineChatType(TdApi.Chat chat) {
        if (chat.type instanceof TdApi.ChatTypePrivate) {
            return "private";
        }

        if (chat.type instanceof TdApi.ChatTypeBasicGroup) {
            return "group";
        }

        if (chat.type instanceof TdApi.ChatTypeSupergroup sg) {
            return sg.isChannel ? "channel" : "supergroup";
        }

        if (chat.type instanceof TdApi.ChatTypeSecret) {
            return "secret";
        }

        return "undefined";
    }

    private record ReplyToMessage(Long id, String text) {
    }
}
