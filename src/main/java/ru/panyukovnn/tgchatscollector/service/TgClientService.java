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
import ru.panyukovnn.tgchatscollector.dto.telegram.ChatShort;
import ru.panyukovnn.tgchatscollector.dto.telegram.TopicShort;
import ru.panyukovnn.tgchatscollector.exception.TgChatsCollectorException;
import ru.panyukovnn.tgchatscollector.property.TgChatLoaderProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    public ChatShort findChat(Long chatId, String publicChatName) {
        if (chatId == null && publicChatName == null) {
            throw new TgChatsCollectorException("46ea", "Отсутствуют chatId и chatName для идентификации чата");
        }

        try {
            CompletableFuture<TdApi.Chat> chatCompletableFuture = chatId != null
                ? tgClient.send(new TdApi.GetChat(chatId))
                : tgClient.send(new TdApi.SearchPublicChat(publicChatName));

            return chatCompletableFuture
                .thenApply(chat -> new ChatShort(chat.id, fetchChannelPublicName(chat), chat.title))
                .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new TgChatsCollectorException("9a1d",
                "Ошибка при поиске чата по идентификатору: %s. Или имени: %s. Сообщение об ошибке: %s"
                    .formatted(chatId, publicChatName, e.getMessage()),
                e);
        }
    }

    /**
     * @param chatId              идентификатор чата
     * @param publicChatName      полное публичное имя (например имя канала)
     * @param privateChatNamePart часть имени приватного чата
     * @return чат
     */
    public Optional<ChatShort> findChat(Long chatId, String publicChatName, String privateChatNamePart) {
        if (chatId != null || StringUtils.hasText(publicChatName)) {
            return Optional.of(findChat(chatId, publicChatName));
        }

        if (privateChatNamePart != null) {
            List<ChatShort> personalChatsByNamePart = findPersonalChatByNamePart(privateChatNamePart);

            if (personalChatsByNamePart.size() > 1) {
                throw new TgChatsCollectorException("4a4a", "Конфликт, найдено более одного чата по части имени персонального чата: " + privateChatNamePart);
            }

            if (personalChatsByNamePart.size() == 1) {
                return Optional.of(personalChatsByNamePart.get(0));
            }
        }

        return Optional.empty();
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
                                                               TopicShort topic,
                                                               @Nullable Integer limit,
                                                               @Nullable LocalDateTime dateFrom,
                                                               @Nullable LocalDateTime dateTo) {
        List<TgMessageDto> messageDtos = new ArrayList<>();
        long fromMessageId = 0L;
        if (dateFrom == null) {
            dateFrom = LocalDateTime.of(
                LocalDate.now(ZoneOffset.UTC).minusDays(5000),
                LocalTime.MIN);
        }

        int limitMessagesToLoad = limit == null ? tgChatLoaderProperty.defaultMessagesLimit() : limit;

        Long previousFirstMessageId = null;
        Long previousLastMessageId = null;
        while (messageDtos.size() < limitMessagesToLoad || Thread.interrupted()) {
            TdApi.Messages messages = collectPublicChatMessages(chatId, topic, fromMessageId);

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
                    fromMessageId = messages.messages[messages.messages.length - 1].id;

                    continue;
                }

                TdApi.MessageContent content = message.content;

                String text = extractText(content);

                if (!StringUtils.hasText(text)) {
                    continue;
                }

                LocalDateTime messageDateTimeUtc = LocalDateTime.ofEpochSecond(message.date, 0, ZoneOffset.UTC);
                LocalDateTime messageDateTime = messageDateTimeUtc
                    .plusHours(3); // Московское время

                if (messageDateTimeUtc.isBefore(dateFrom)) {
                    log.info("Извлечено сообщений: {}", messageDtos.size());

                    return messageDtos;
                }

                if (dateTo != null && messageDateTimeUtc.isAfter(dateTo)) {
                    continue;
                }

                messageDtos.add(TgMessageDto.builder()
                    .senderId(extractSenderId(message))
                    .dateTime(messageDateTime)
                    .messageId(message.id)
                    .replyToText(fetchReplyToMessageText(message))
                    .text(text)
                    .build());

                if (messages.messages.length == 0) {
                    break;
                }

                fromMessageId = messages.messages[messages.messages.length - 1].id;
            }
        }

        log.info("Извлечено сообщений: {}", messageDtos.size());

        return messageDtos;
    }

    @SneakyThrows
    public List<ChatShort> findPersonalChatByNamePart(String namePart) {
        TdApi.Chats mainChats = tgClient.send(new TdApi.GetChats(new TdApi.ChatListMain(), Integer.MAX_VALUE))
            .get();
        TdApi.Chats archiveChats = tgClient.send(new TdApi.GetChats(new TdApi.ChatListArchive(), Integer.MAX_VALUE))
            .get();

        List<CompletableFuture<TdApi.Chat>> chatsWithInfo = Stream.concat(Arrays.stream(mainChats.chatIds).boxed(), Arrays.stream(archiveChats.chatIds).boxed())
            .map(chatId -> tgClient.send(new TdApi.GetChat(chatId)))
            .toList();

        chatsWithInfo.forEach(CompletableFuture::join);

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

                return new ChatShort(chat.id, chatPublicName, chat.title);
            })
            .toList();
    }

    @SneakyThrows
    public TopicShort findTopicByName(long chatId, String topicNamePart) {
        return tgClient.send(new TdApi.GetForumTopics(chatId, topicNamePart, 0, 0L, 0L, 100))
            .thenApply(topics -> {
                TdApi.ForumTopic topic = Arrays.stream(topics.topics)
                    .filter(ft -> ft.info.name.equalsIgnoreCase(topicNamePart))
                    .findFirst()
                    .orElseGet(() -> {
                        if (topics.topics.length == 0) {
                            throw new TgChatsCollectorException("1689", "Не удалось найти топик по имени: " + topicNamePart);
                        } else {
                            return topics.topics[0];
                        }
                    });

                return new TopicShort(topic.info.isGeneral, topic.info.messageThreadId, topic.info.name, topic.lastMessage.id);
            })
            .get();
    }

    public List<ChatInfoDto> findLastChats(Integer count) {
        TdApi.Chats chats = tgClient.send(new TdApi.GetChats(new TdApi.ChatListMain(), count))
            .join();

        return Arrays.stream(chats.chatIds).boxed()
            .map(chatId -> tgClient.send(new TdApi.GetChat(chatId)))
            .map(CompletableFuture::join)
            .map(chat -> new ChatInfoDto(chat.id, chat.type.getClass().getSimpleName(), chat.title))
            .toList();
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

    private String fetchReplyToMessageText(TdApi.Message message) {
        if (message.replyTo instanceof TdApi.MessageReplyToMessage replyToMessageInfo) {
            try {
                TdApi.Message replyToMessage = tgClient.send(new TdApi.GetMessage(replyToMessageInfo.chatId, replyToMessageInfo.messageId))
                    .get();
                return extractText(replyToMessage.content);
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

    private TdApi.Messages collectPublicChatMessages(long chatId, TopicShort topic, long fromMessageId) {
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

    private String extractText(TdApi.MessageContent content) {
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
}
