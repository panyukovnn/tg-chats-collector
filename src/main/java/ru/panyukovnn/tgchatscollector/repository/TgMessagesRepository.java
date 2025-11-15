package ru.panyukovnn.tgchatscollector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.panyukovnn.tgchatscollector.model.TgMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TgMessagesRepository extends JpaRepository<TgMessage, UUID> {

    @Query(value = """
        SELECT *
        FROM tg_messages m
        WHERE m.chat_id = :chatId
         AND (:topicId IS NULL OR m.topic_id = :topicId)
        ORDER BY m.date_time ASC
        LIMIT 1
        """,
        nativeQuery = true)
    Optional<TgMessage> findEarliestByChatIdAndTopicId(Long chatId, Long topicId);

    @Query(value = """
        SELECT *
        FROM tg_messages m
        WHERE m.chat_id = :chatId
         AND (:topicId IS NULL OR m.topic_id = :topicId)
        ORDER BY m.date_time DESC
        LIMIT 1
        """,
        nativeQuery = true)
    Optional<TgMessage> findLatestByChatIdAndTopicId(Long chatId, Long topicId);

    @Query("""
        DELETE FROM TgMessage m 
        WHERE m.chatId = :chatId
        AND (:topicId IS NULL OR m.topicId = :topicId)
        AND m.dateTime >= :dateFrom
    """)
    @Modifying
    int deleteAllByChatIdAndTopicIdAndDateTimeFrom(Long chatId, Long topicId, LocalDateTime dateFrom);

    Optional<TgMessage> findByChatIdAndTopicIdAndExternalId(Long chatId, Long topicId, Long externalId);

    @Query("""
        FROM TgMessage m
        WHERE m.chatId = :chatId
        AND (:topicId IS NULL OR m.topicId = :topicId)
        AND m.dateTime >= :dateFrom
    """)
    List<TgMessage> findAllByChatIdAndTopicIdAndDateTimeFrom(Long chatId, Long topicId, LocalDateTime dateFrom);
}
