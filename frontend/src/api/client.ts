import type {
    ChatTopicsResponse,
    LastChatsWithPreviewResponse,
    SearchChatHistoryRequest,
    SearchChatHistoryResponse,
} from "./types";

const API_BASE = "/tg-chats-collector/api/v1";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await fetch(`${API_BASE}${path}`, {
        ...init,
        headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
            ...(init?.headers ?? {}),
        },
    });

    if (!response.ok) {
        const body = await response.text();
        throw new Error(`Запрос ${path} завершился ошибкой ${response.status}: ${body}`);
    }

    return response.json() as Promise<T>;
}

export function fetchLastChatsWithPreview(count: number): Promise<LastChatsWithPreviewResponse> {
    return request<LastChatsWithPreviewResponse>(`/chats/last-with-preview?count=${count}`);
}

export function fetchChatTopics(chatId: number): Promise<ChatTopicsResponse> {
    return request<ChatTopicsResponse>(`/chats/${chatId}/topics`);
}

export function searchChatHistory(
    request_: SearchChatHistoryRequest,
): Promise<SearchChatHistoryResponse> {
    return request<SearchChatHistoryResponse>("/chat-history/search", {
        method: "POST",
        body: JSON.stringify(request_),
    });
}
