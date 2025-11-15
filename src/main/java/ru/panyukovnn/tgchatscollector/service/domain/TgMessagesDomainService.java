package ru.panyukovnn.tgchatscollector.service.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.panyukovnn.tgchatscollector.dto.TgMessageDto;
import ru.panyukovnn.tgchatscollector.model.TgMessage;
import ru.panyukovnn.tgchatscollector.model.TgMessageType;
import ru.panyukovnn.tgchatscollector.repository.TgMessagesRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TgMessagesDomainService {

    private final TgMessagesRepository tgMessagesRepository;

    /**
     * Сохраняет сообщение в базу данных
     */
    @Transactional
    public TgMessage saveMessage(Long chatId, Long topicId, TgMessageDto messageDto) {
        // Проверяем, существует ли уже такое сообщение
        Optional<TgMessage> existing = tgMessagesRepository.findByChatIdAndTopicIdAndExternalId(
            chatId, topicId, messageDto.getMessageId()
        );

        if (existing.isPresent()) {
            log.debug("Сообщение уже существует: chatId={}, topicId={}, externalId={}",
                chatId, topicId, messageDto.getMessageId());
            return existing.get();
        }

        TgMessage message = TgMessage.builder()
            .chatId(chatId)
            .topicId(topicId)
            .externalId(messageDto.getMessageId())
            .dateTime(messageDto.getDateTime())
            .senderId(messageDto.getSenderId())
            .type(TgMessageType.TEXT)
            .content(messageDto.getText())
            .replyToText(messageDto.getReplyToText())
            .replyToMessageId(messageDto.getReplyToMessageId())
            .build();

        TgMessage saved = tgMessagesRepository.save(message);
        log.debug("Сохранено сообщение: chatId={}, topicId={}, externalId={}",
            chatId, topicId, messageDto.getMessageId());
        return saved;
    }

    /**
     * Сохраняет список сообщений в базу данных
     */
    @Transactional
    public void saveMessages(Long chatId, Long topicId, List<TgMessageDto> messageDtos) {
        for (TgMessageDto messageDto : messageDtos) {
            saveMessage(chatId, topicId, messageDto);
        }

        log.info("Сохранено {} сообщений для chatId={}, topicId={}",
            messageDtos.size(), chatId, topicId);
    }

    public Optional<TgMessage> findEarliestByChatAndTopic(Long chatId, Long topicId) {
        return tgMessagesRepository.findEarliestByChatIdAndTopicId(chatId, topicId);
    }

    public Optional<TgMessage> findLatestByChatAndTopic(Long chatId, Long topicId) {
        return tgMessagesRepository.findLatestByChatIdAndTopicId(chatId, topicId);
    }

    public void deleteMessagesFromDate(Long chatId, Long topicId, LocalDateTime dateFrom) {
        int messagesRemoved = tgMessagesRepository.deleteAllByChatIdAndTopicIdAndDateTimeFrom(chatId, topicId, dateFrom);
        log.info("Удалено {} сообщений для chatId={}, topicId={}, dateFrom={}", messagesRemoved, chatId, topicId, dateFrom);
    }

    public List<TgMessage> findMessagesFromDate(Long chatId, Long topicId, LocalDateTime dateFrom) {
        return tgMessagesRepository.findAllByChatIdAndTopicIdAndDateTimeFrom(chatId, topicId, dateFrom);
    }
}
