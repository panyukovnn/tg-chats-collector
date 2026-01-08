package ru.panyukovnn.tgchatscollector;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import picocli.CommandLine;
import ru.panyukovnn.tgchatscollector.command.LastChatsCommand;
import ru.panyukovnn.tgchatscollector.command.SearchChatHistoryCommand;
import ru.panyukovnn.tgchatscollector.command.SearchPrivateChatCommand;
import ru.panyukovnn.tgchatscollector.command.SearchPublicChannelCommand;

@QuarkusMain
@CommandLine.Command(
    name = "tg-chats-collector",
    mixinStandardHelpOptions = true,
    version = "1.0.0-RC1",
    description = "Telegram chats collector CLI tool",
    subcommands = {
        LastChatsCommand.class,
        SearchPrivateChatCommand.class,
        SearchPublicChannelCommand.class,
        SearchChatHistoryCommand.class
    }
)
public class TgChatsCollectorCli implements QuarkusApplication {

    @Inject
    CommandLine.IFactory factory;

    @Override
    public int run(String... args) {
        return new CommandLine(this, factory).execute(args);
    }
}
