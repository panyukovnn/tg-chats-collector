import type { ChatWithPreview } from "../api/types";

interface Props {
    chats: ChatWithPreview[];
    selectedChatId: number | null;
    onSelect: (chat: ChatWithPreview) => void;
}

function formatTime(dateTime: string | null | undefined): string {
    if (!dateTime) {
        return "";
    }

    const date = new Date(dateTime);

    if (Number.isNaN(date.getTime())) {
        return "";
    }

    return date.toLocaleString("ru-RU", { dateStyle: "short", timeStyle: "short" });
}

export function ChatList({ chats, selectedChatId, onSelect }: Props) {
    if (chats.length === 0) {
        return <div className="empty">Чаты не найдены</div>;
    }

    return (
        <ul className="chat-list">
            {chats.map((chat) => {
                const isActive = chat.chatId === selectedChatId;

                return (
                    <li
                        key={chat.chatId}
                        className={`chat-item${isActive ? " active" : ""}`}
                        onClick={() => onSelect(chat)}
                    >
                        <div className="chat-item-header">
                            <span className="chat-title">
                                {chat.title}
                                {chat.forum ? <span className="chat-badge">форум</span> : null}
                            </span>
                            <span className="chat-time">{formatTime(chat.lastMessageDateTime)}</span>
                        </div>
                        <div className="chat-preview">
                            {chat.lastMessageSenderName ? (
                                <span className="chat-sender">{chat.lastMessageSenderName}: </span>
                            ) : null}
                            <span className="chat-text">{chat.lastMessageText ?? "Нет сообщений"}</span>
                        </div>
                    </li>
                );
            })}
        </ul>
    );
}
