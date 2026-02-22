package ru.panyukovnn.tgchatscollector;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class TgChatsCollectorApp {

    public static void main(String[] args) {
        Quarkus.run(args);
    }
}