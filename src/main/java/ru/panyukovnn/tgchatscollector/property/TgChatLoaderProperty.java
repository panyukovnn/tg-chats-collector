package ru.panyukovnn.tgchatscollector.property;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "tg-collector.chat-loader")
public interface TgChatLoaderProperty {

    /**
     * Количество дней за которые будут извлечены сообщения из чата
     */
    Integer defaultMessagesLimit();

    /**
     * Количество дней за которые будут извлечены сообщения из чата
     */
    Integer defaultDaysBeforeLimit();
}
