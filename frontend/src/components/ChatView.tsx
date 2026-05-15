import { useEffect, useRef, useState } from "react";
import type { ChatTopic, ChatWithPreview, TgMessage } from "../api/types";
import { fetchChatTopics, searchChatHistory } from "../api/client";
import { DownloadDialog } from "./DownloadDialog";

interface Props {
    chat: ChatWithPreview;
}

const RECENT_MESSAGES_LIMIT = 50;

function formatDateTime(value: string): string {
    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return date.toLocaleString("ru-RU", { dateStyle: "short", timeStyle: "short" });
}

function sortAscending(messages: TgMessage[]): TgMessage[] {
    return [...messages].sort((a, b) => {
        const left = new Date(a.dateTime).getTime();
        const right = new Date(b.dateTime).getTime();

        return left - right;
    });
}

export function ChatView({ chat }: Props) {
    const [dialogOpen, setDialogOpen] = useState(false);
    const [messages, setMessages] = useState<TgMessage[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [topics, setTopics] = useState<ChatTopic[]>([]);
    const [topicsLoading, setTopicsLoading] = useState(false);
    const [selectedTopicId, setSelectedTopicId] = useState<number | null>(null);
    const [attachPhoto, setAttachPhoto] = useState(false);
    const bodyRef = useRef<HTMLDivElement | null>(null);

    useEffect(() => {
        setTopics([]);
        setSelectedTopicId(null);

        if (!chat.forum) {
            return;
        }

        let cancelled = false;
        setTopicsLoading(true);

        fetchChatTopics(chat.chatId)
            .then((response) => {
                if (cancelled) {
                    return;
                }

                setTopics(response.topics);
            })
            .catch(() => {
                if (cancelled) {
                    return;
                }

                setTopics([]);
            })
            .finally(() => {
                if (!cancelled) {
                    setTopicsLoading(false);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [chat.chatId, chat.forum]);

    useEffect(() => {
        let cancelled = false;

        setLoading(true);
        setError(null);
        setMessages([]);

        searchChatHistory({
            chatId: chat.chatId,
            topicId: selectedTopicId,
            limit: RECENT_MESSAGES_LIMIT,
            attachPhoto,
        })
            .then((response) => {
                if (cancelled) {
                    return;
                }

                setMessages(sortAscending(response.messages));
            })
            .catch((err) => {
                if (cancelled) {
                    return;
                }

                setError(err instanceof Error ? err.message : "Не удалось загрузить сообщения");
            })
            .finally(() => {
                if (!cancelled) {
                    setLoading(false);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [chat.chatId, selectedTopicId, attachPhoto]);

    useEffect(() => {
        if (!loading && messages.length > 0 && bodyRef.current) {
            bodyRef.current.scrollTop = bodyRef.current.scrollHeight;
        }
    }, [loading, messages]);

    return (
        <div className="chat-view">
            <div className="chat-view-header">
                <div>
                    <div className="chat-view-title">{chat.title}</div>
                    <div className="chat-view-type">
                        {chat.type}
                        {chat.forum ? " · форум" : ""}
                    </div>
                </div>
                <div className="chat-view-actions">
                    <label className="toggle">
                        <input
                            type="checkbox"
                            checked={attachPhoto}
                            onChange={(event) => setAttachPhoto(event.target.checked)}
                        />
                        <span>Прикладывать фото</span>
                    </label>
                    <button className="primary" onClick={() => setDialogOpen(true)}>
                        Скачать
                    </button>
                </div>
            </div>

            {chat.forum ? (
                <div className="topics-bar">
                    <label className="topics-label">Топик:</label>
                    <select
                        className="topics-select"
                        value={selectedTopicId === null ? "" : String(selectedTopicId)}
                        onChange={(event) => {
                            const value = event.target.value;
                            setSelectedTopicId(value === "" ? null : Number(value));
                        }}
                        disabled={topicsLoading}
                    >
                        <option value="">Весь форум</option>
                        {topics.map((topic) => (
                            <option key={topic.topicId} value={topic.topicId}>
                                {topic.general ? "General" : topic.title}
                            </option>
                        ))}
                    </select>
                    {topicsLoading ? <span className="topics-hint">загрузка...</span> : null}
                </div>
            ) : null}

            <div className="chat-view-body" ref={bodyRef}>
                {loading ? (
                    <div className="empty">Загрузка сообщений...</div>
                ) : error ? (
                    <div className="error">{error}</div>
                ) : messages.length === 0 ? (
                    <div className="empty">Сообщений нет</div>
                ) : (
                    <ul className="message-list">
                        {messages.map((message) => (
                            <li key={message.messageId} className="message">
                                <div className="message-meta">
                                    <span className="message-sender">
                                        {message.senderName ?? "—"}
                                    </span>
                                    {message.senderUsername ? (
                                        <span className="message-username">
                                            {" "}@{message.senderUsername}
                                        </span>
                                    ) : null}
                                    {message.senderId ? (
                                        <span className="message-id">
                                            {" "}· id {message.senderId}
                                        </span>
                                    ) : null}
                                    <span> · {formatDateTime(message.dateTime)}</span>
                                </div>
                                {message.replyToText ? (
                                    <div className="message-reply">{message.replyToText}</div>
                                ) : null}
                                {message.photoBase64 ? (
                                    <img
                                        className="message-photo"
                                        src={`data:image/jpeg;base64,${message.photoBase64}`}
                                        alt="Приложенное фото"
                                    />
                                ) : null}
                                <div className="message-text">{message.text}</div>
                            </li>
                        ))}
                    </ul>
                )}
            </div>

            {dialogOpen ? (
                <DownloadDialog
                    chat={chat}
                    topicId={selectedTopicId}
                    topicTitle={
                        selectedTopicId !== null
                            ? topics.find((it) => it.topicId === selectedTopicId)?.title ?? null
                            : null
                    }
                    attachPhoto={attachPhoto}
                    onClose={() => setDialogOpen(false)}
                />
            ) : null}
        </div>
    );
}
