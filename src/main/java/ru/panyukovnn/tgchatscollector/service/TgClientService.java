package ru.panyukovnn.tgchatscollector.service;

import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.jni.TdApi;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.panyukovnn.tgchatscollector.dto.ChatInfoDto;
import ru.panyukovnn.tgchatscollector.dto.TgMessageDto;
import ru.panyukovnn.tgchatscollector.dto.telegram.ChatInfo;
import ru.panyukovnn.tgchatscollector.dto.telegram.TopicInfo;
import ru.panyukovnn.tgchatscollector.exception.TgChatsCollectorException;
import ru.panyukovnn.tgchatscollector.property.TgChatLoaderProperty;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TgClientService {

    private final SimpleTelegramClient tgClient;
    private final TgChatLoaderProperty tgChatLoaderProperty;

    @SneakyThrows
    public ChatInfo searchChats(Long chatId, String publicChatName) {
        if (chatId == null && publicChatName == null) {
            throw new TgChatsCollectorException("46ea", "Отсутствуют chatId и chatName для идентификации чата");
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
        if (chatId != null || StringUtils.hasText(publicChatName)) {
            return List.of(searchChats(chatId, publicChatName));
        }

        if (privateChatNamePart != null) {
            return findPersonalChatByNamePart(privateChatNamePart);
        }

        return List.of();
    }

    /**
     * Важно, чтобы даты передавались в UTC
     *
     * @param chatId   идентификатор чата
     * @param topic    топик
     * @param limit    предельное количество сообщений
     * @param dateFrom дата начала периода в UTC
     * @param dateTo   дата окончания периода в UTC
     * @return список сообщений из чата
     */
    public List<TgMessageDto> collectAllMessagesFromPublicChat(Long chatId,
                                                               TopicInfo topic,
                                                               @Nullable Integer limit,
                                                               @Nullable LocalDateTime dateFrom,
                                                               @Nullable LocalDateTime dateTo) {
        if (limit == null && dateFrom == null) {
            // Если не заданы ограничения, то задаем дефолтный лимит
            limit = tgChatLoaderProperty.defaultMessagesLimit();
        }

        List<TgMessageDto> messageDtos = new ArrayList<>();
        long fromMessageId = 0L;

        int limitMessagesToLoad = limit == null ? Integer.MAX_VALUE : limit;

        Long previousFirstMessageId = null;
        Long previousLastMessageId = null;
        while (messageDtos.size() < limitMessagesToLoad && !Thread.interrupted()) {
            TdApi.Messages messages = collectChatMessages(chatId, topic, fromMessageId);

            if (messages.totalCount == 0) {
                log.info("Извлечено сообщений: {}", messageDtos.size());

                return messageDtos;
            }

            // Проверяем что выгрузилась новая группа сообщений
            long firstMessageId = messages.messages[0].id;
            long lastMessageId = messages.messages[messages.messages.length - 1].id;
            if (Objects.equals(firstMessageId, previousFirstMessageId) && Objects.equals(lastMessageId, previousLastMessageId)) {
                log.info("Извлечено сообщений: {}", messageDtos.size());

                return messageDtos;
            }
            previousFirstMessageId = firstMessageId;
            previousLastMessageId = lastMessageId;

            log.info("Загружено сообщений в пачке: {}", messages.messages.length);

            for (TdApi.Message message : messages.messages) {
                // Если это default топик и сообщение относится к какому-либо из топиков, то его пропускаем
                if (topic != null && topic.isGeneral() && message.isTopicMessage) {
                    continue;
                }

                TdApi.MessageContent content = message.content;

                String text = extractMessageTextSafely(content);

                if (!StringUtils.hasText(text)) {
                    continue;
                }

                LocalDateTime messageDateTimeUtc = LocalDateTime.ofEpochSecond(message.date, 0, ZoneOffset.UTC);
                LocalDateTime messageDateTime = messageDateTimeUtc
                    .plusHours(3); // Московское время

                if (dateFrom != null && messageDateTimeUtc.isBefore(dateFrom)) {
                    log.info("Извлечено сообщений: {}", messageDtos.size());

                    return messageDtos;
                }

                if (dateTo != null && messageDateTimeUtc.isAfter(dateTo)) {
                    continue;
                }

                ReplyToMessage replyToMessage = fetchReplyToMessage(message);

                messageDtos.add(TgMessageDto.builder()
                    .senderId(extractSenderId(message))
                    .dateTime(messageDateTime)
                    .messageId(message.id)
                    .replyToText(replyToMessage != null ? replyToMessage.text : null)
                    .replyToMessageId(replyToMessage != null ? replyToMessage.id : null)
                    .text(text)
                    .build());
            }

            // Обновляем fromMessageId на последнее сообщение в пачке для загрузки следующей пачки
            fromMessageId = messages.messages[messages.messages.length - 1].id;
        }

        log.info("Извлечено сообщений: {}", messageDtos.size());

        return messageDtos;
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
                .filter(ft -> org.apache.commons.lang3.StringUtils.containsIgnoreCase(ft.info.name, topicNamePart))
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

    private TdApi.Messages collectChatMessages(long chatId, TopicInfo topic, long fromMessageId) {
        try {
            // Если это default (general) топик, то читаем вообще все сообщения из чата, и затем будут фильтроваться только те сообщения, которые не относятся ни к одному из топиков
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
        if (content instanceof TdApi.MessageVideo messageVideo) {
            return messageVideo.caption.text;
        }
        if (content instanceof TdApi.MessagePhoto photo) {
            return photo.caption.text;
        }
        if (content instanceof TdApi.MessageAudio audio) {
            return audio.caption.text;
        }
        if (content instanceof TdApi.MessageText messageText) {
            return messageText.text.text;
        }

        return null;
    }

    private static String defineChatType(TdApi.Chat chat) {
        return switch (chat.type) {
            case TdApi.ChatTypePrivate ignored -> "private";
            case TdApi.ChatTypeBasicGroup ignored -> "group";
            case TdApi.ChatTypeSupergroup sg -> sg.isChannel ? "channel" : "supergroup";
            case TdApi.ChatTypeSecret ignored -> "secret";
        };
    }

    private record ReplyToMessage(Long id, String text) {
    }
}
