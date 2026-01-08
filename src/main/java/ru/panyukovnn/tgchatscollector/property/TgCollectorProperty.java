package ru.panyukovnn.tgchatscollector.property;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "tg-collector.telegram.client")
public interface TgCollectorProperty {

    Integer apiId();

    String apiHash();

    String phone();
}
