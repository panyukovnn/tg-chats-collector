import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { fetchLastChatsWithPreview } from "./api/client";
import type { ChatWithPreview } from "./api/types";
import { ChatList } from "./components/ChatList";
import { ChatView } from "./components/ChatView";

const PAGE_SIZE = 30;
const SCROLL_THRESHOLD_PX = 200;

export default function App() {
    const [chats, setChats] = useState<ChatWithPreview[]>([]);
    const [selected, setSelected] = useState<ChatWithPreview | null>(null);
    const [loading, setLoading] = useState(true);
    const [hasMore, setHasMore] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [query, setQuery] = useState("");
    const countRef = useRef(PAGE_SIZE);
    const inFlightRef = useRef(false);

    const filteredChats = useMemo(() => {
        const normalized = query.trim().toLowerCase();

        if (normalized === "") {
            return chats;
        }

        return chats.filter((chat) => chat.title.toLowerCase().includes(normalized));
    }, [chats, query]);

    const loadChats = useCallback(async (count: number) => {
        if (inFlightRef.current) {
            return;
        }

        inFlightRef.current = true;
        setLoading(true);

        try {
            const response = await fetchLastChatsWithPreview(count);

            setChats(response.chats);
            setHasMore(response.chats.length >= count);
            setError(null);
        } catch (err) {
            setError(err instanceof Error ? err.message : "Не удалось загрузить чаты");
        } finally {
            setLoading(false);
            inFlightRef.current = false;
        }
    }, []);

    useEffect(() => {
        loadChats(PAGE_SIZE);
    }, [loadChats]);

    function handleScroll(event: React.UIEvent<HTMLDivElement>) {
        if (!hasMore || loading || query.trim() !== "") {
            return;
        }

        const target = event.currentTarget;
        const distanceToBottom = target.scrollHeight - target.scrollTop - target.clientHeight;

        if (distanceToBottom < SCROLL_THRESHOLD_PX) {
            countRef.current += PAGE_SIZE;
            loadChats(countRef.current);
        }
    }

    return (
        <div className="app">
            <aside className="sidebar">
                <div className="sidebar-header">
                    <div>Чаты</div>
                    <input
                        className="search-input"
                        type="search"
                        placeholder="Поиск по названию"
                        value={query}
                        onChange={(event) => setQuery(event.target.value)}
                    />
                </div>
                <div className="sidebar-scroll" onScroll={handleScroll}>
                    {error ? <div className="error">{error}</div> : null}
                    <ChatList
                        chats={filteredChats}
                        selectedChatId={selected?.chatId ?? null}
                        onSelect={setSelected}
                    />
                    {loading ? <div className="empty">Загрузка...</div> : null}
                    {!loading && query.trim() !== "" && filteredChats.length === 0 ? (
                        <div className="empty">Ничего не найдено среди загруженных чатов</div>
                    ) : null}
                    {!loading && query.trim() === "" && !hasMore && chats.length > 0 ? (
                        <div className="empty">Это все чаты</div>
                    ) : null}
                </div>
            </aside>

            <main className="main">
                {selected ? (
                    <ChatView chat={selected} />
                ) : (
                    <div className="empty centered">Выберите чат слева</div>
                )}
            </main>
        </div>
    );
}
