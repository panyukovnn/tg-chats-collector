package ru.panyukovnn.tgchatscollector.service.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.panyukovnn.tgchatscollector.dto.ChatInfoDto;
import ru.panyukovnn.tgchatscollector.dto.TgMessageDto;
import ru.panyukovnn.tgchatscollector.dto.chathistory.ChatHistoryResponse;
import ru.panyukovnn.tgchatscollector.dto.chathistory.MessageDto;
import ru.panyukovnn.tgchatscollector.dto.chathistory.MessagesBatch;
import ru.panyukovnn.tgchatscollector.dto.lastchats.LastChatsResponse;
import ru.panyukovnn.tgchatscollector.dto.telegram.ChatShort;
import ru.panyukovnn.tgchatscollector.dto.telegram.TopicShort;
import ru.panyukovnn.tgchatscollector.exception.TgChatsCollectorException;
import ru.panyukovnn.tgchatscollector.service.TgClientService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TgCollectorHandler {

    private static final int MAX_BATCH_SIZE_KB = 190;

    private final ObjectMapper objectMapper;
    private final TgClientService tgClientService;

    public LastChatsResponse handleLastChats(Integer count) {
        List<ChatInfoDto> lastChatDtos = tgClientService.findLastChats(count);

        return new LastChatsResponse(lastChatDtos);
    }

    public ChatHistoryResponse handleChatHistory(String publicChatName,
                                                 String privateChatNamePart,
                                                 String topicNamePart,
                                                 @Nullable Integer limit,
                                                 @Nullable LocalDateTime dateFrom,
                                                 @Nullable LocalDateTime dateTo) {
        if (!StringUtils.hasText(publicChatName)
            && !StringUtils.hasText(privateChatNamePart)
            && !StringUtils.hasText(topicNamePart)) {
            throw new IllegalArgumentException("publicChatName, privateChatNamePart, topicNamePart не могут быть одновременно пустыми");
        }

        ChatShort chat = tgClientService.findChat(null, publicChatName, privateChatNamePart)
            .orElseThrow(() -> new TgChatsCollectorException("31ff", "Не удалось найти чат"));
        TopicShort topic = Optional.ofNullable(topicNamePart)
            .map(tn -> tgClientService.findTopicByName(chat.chatId(), tn))
            .orElse(null);

        List<TgMessageDto> messageDtos = tgClientService.collectAllMessagesFromPublicChat(chat.chatId(), topic, limit, dateFrom, dateTo);

        LocalDateTime firstMessageDateTime = !messageDtos.isEmpty() ? messageDtos.get(0).getDateTime() : null;
        LocalDateTime lastMessageDateTime = !messageDtos.isEmpty() ? messageDtos.get(messageDtos.size() - 1).getDateTime() : null;

        List<MessagesBatch> messageBatches = createMessageBatches(messageDtos);

        int totalCount = messageBatches.stream()
            .map(MessagesBatch::getCount)
            .reduce(0, Integer::sum);

        return ChatHistoryResponse.builder()
            .chatId(chat.chatId())
            .chatTitle(chat.title())
            .chatPublicName(chat.chatPublicName())
            .topicId(topic != null ? topic.topicId() : null)
            .topicName(topic != null ? topic.title() : null)
            .firstMessageDateTime(firstMessageDateTime)
            .lastMessageDateTime(lastMessageDateTime)
            .totalCount(totalCount)
            .messageBatches(messageBatches)
            .build();
    }

    private List<MessagesBatch> createMessageBatches(List<TgMessageDto> messageDtos) {
        List<MessagesBatch> batches = new ArrayList<>();
        List<MessageDto> currentBatch = new ArrayList<>();
        int currentBatchSizeBytes = 0;

        for (TgMessageDto message : messageDtos) {
            MessageDto messageDto = MessageDto.builder()
                .senderId(message.getSenderId())
                .replyToText(message.getReplyToText())
                .id(message.getMessageId())
                .text(message.getText())
                .build();

            try {
                int messageSizeBytes = objectMapper.writeValueAsString(messageDto).getBytes().length;

                if (currentBatchSizeBytes + messageSizeBytes > MAX_BATCH_SIZE_KB * 1024 && !currentBatch.isEmpty()) {
                    batches.add(MessagesBatch.builder()
                        .count(currentBatch.size())
                        .messages(new ArrayList<>(currentBatch))
                        .build());
                    currentBatch.clear();
                    currentBatchSizeBytes = 0;
                }

                currentBatch.add(messageDto);
                currentBatchSizeBytes += messageSizeBytes;
            } catch (Exception e) {
                log.error("Ошибка при сериализации сообщения в JSON: {}", e.getMessage());
            }
        }

        if (!currentBatch.isEmpty()) {
            batches.add(MessagesBatch.builder()
                .count(currentBatch.size())
                .messages(currentBatch)
                .build());
        }

        return batches;
    }
}
