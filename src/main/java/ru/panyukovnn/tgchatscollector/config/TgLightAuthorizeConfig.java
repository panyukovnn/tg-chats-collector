package ru.panyukovnn.tgchatscollector.config;

import it.tdlight.client.SimpleTelegramClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import io.quarkus.runtime.StartupEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@ApplicationScoped
public class TgLightAuthorizeConfig {

    @Inject
    SimpleTelegramClient tgClient;

    /**
     * При первом обращении к сервису необходимо выполнить автоирзацию для сохранения сессии.
     *
     * @param event событие старта приложения
     */
    void onStart(@Observes StartupEvent event) {
        try {
            log.info("Authorizing Telegram client...");
            tgClient.getMeAsync()
                .get(10, TimeUnit.MINUTES);
            log.info("Telegram client authorized successfully");
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.error("Failed to authorize Telegram client", e);
            throw new RuntimeException("Failed to authorize Telegram client", e);
        }
    }
}
