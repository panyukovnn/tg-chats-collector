export interface ChatWithPreview {
    chatId: number;
    type: string;
    title: string;
    forum?: boolean;
    lastMessageText?: string | null;
    lastMessageSenderName?: string | null;
    lastMessageDateTime?: string | null;
}

export interface ChatTopic {
    topicId: number;
    title: string;
    general: boolean;
}

export interface ChatTopicsResponse {
    topics: ChatTopic[];
}

export interface LastChatsWithPreviewResponse {
    chats: ChatWithPreview[];
}

export interface TgMessage {
    messageId: number;
    senderId: number | null;
    senderName: string | null;
    senderUsername: string | null;
    dateTime: string;
    text: string;
    replyToText: string | null;
    replyToMessageId: number | null;
    photoBase64: string | null;
}

export interface SearchChatHistoryRequest {
    chatId: number;
    topicId?: number | null;
    limit?: number | null;
    dateFrom?: string | null;
    dateTo?: string | null;
    attachPhoto?: boolean | null;
}

export interface SearchChatHistoryResponse {
    chatId: number;
    chatTitle: string;
    chatPublicName: string | null;
    topicId: number | null;
    topicName: string | null;
    totalCount: number;
    messages: TgMessage[];
}
