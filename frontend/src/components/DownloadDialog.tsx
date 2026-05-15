import { useState } from "react";
import type { ChatWithPreview } from "../api/types";
import { searchChatHistory } from "../api/client";

interface Props {
    chat: ChatWithPreview;
    topicId?: number | null;
    topicTitle?: string | null;
    attachPhoto?: boolean;
    onClose: () => void;
}

function toBackendDateTime(localValue: string): string {
    return `${localValue}:00`;
}

function buildFileName(chat: ChatWithPreview, dateFrom: string, dateTo: string): string {
    const safeTitle = chat.title.replace(/[^\p{L}\p{N}_-]+/gu, "_");

    return `chat_${safeTitle}_${dateFrom}_${dateTo}.json`;
}

function toLocalDatetimeInputValue(date: Date): string {
    const pad = (value: number) => value.toString().padStart(2, "0");

    return (
        `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
        `T${pad(date.getHours())}:${pad(date.getMinutes())}`
    );
}

function defaultDateRange() {
    const now = new Date();
    const weekAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);

    return {
        dateFrom: toLocalDatetimeInputValue(weekAgo),
        dateTo: toLocalDatetimeInputValue(now),
    };
}

export function DownloadDialog({ chat, topicId, topicTitle, attachPhoto, onClose }: Props) {
    const initial = defaultDateRange();
    const [dateFrom, setDateFrom] = useState<string>(initial.dateFrom);
    const [dateTo, setDateTo] = useState<string>(initial.dateTo);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    async function handleDownload() {
        if (!dateFrom || !dateTo) {
            setError("Укажите начало и конец периода");

            return;
        }

        setLoading(true);
        setError(null);

        try {
            const response = await searchChatHistory({
                chatId: chat.chatId,
                topicId: topicId ?? null,
                dateFrom: toBackendDateTime(dateFrom),
                dateTo: toBackendDateTime(dateTo),
                attachPhoto: attachPhoto ?? false,
            });

            const blob = new Blob([JSON.stringify(response, null, 2)], {
                type: "application/json",
            });
            const url = URL.createObjectURL(blob);
            const anchor = document.createElement("a");

            anchor.href = url;
            anchor.download = buildFileName(chat, dateFrom, dateTo);
            document.body.appendChild(anchor);
            anchor.click();
            document.body.removeChild(anchor);
            URL.revokeObjectURL(url);
            onClose();
        } catch (err) {
            setError(err instanceof Error ? err.message : "Не удалось скачать историю");
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="modal-backdrop" onClick={onClose}>
            <div className="modal" onClick={(event) => event.stopPropagation()}>
                <div className="modal-title">
                    Скачать историю «{chat.title}»
                    {topicId !== null && topicId !== undefined ? (
                        <div className="modal-subtitle">Топик: {topicTitle ?? topicId}</div>
                    ) : null}
                </div>

                <label className="field">
                    <span>Начало периода</span>
                    <input
                        type="datetime-local"
                        value={dateFrom}
                        onChange={(event) => setDateFrom(event.target.value)}
                    />
                </label>

                <label className="field">
                    <span>Конец периода</span>
                    <input
                        type="datetime-local"
                        value={dateTo}
                        onChange={(event) => setDateTo(event.target.value)}
                    />
                </label>

                {error ? <div className="error">{error}</div> : null}

                <div className="modal-actions">
                    <button onClick={onClose} disabled={loading}>
                        Отмена
                    </button>
                    <button className="primary" onClick={handleDownload} disabled={loading}>
                        {loading ? "Загрузка..." : "Скачать"}
                    </button>
                </div>
            </div>
        </div>
    );
}
