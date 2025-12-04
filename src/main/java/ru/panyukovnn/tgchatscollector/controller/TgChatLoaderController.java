package ru.panyukovnn.tgchatscollector.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.panyukovnn.tgchatscollector.dto.searchchat.SearchPublicChannelByIdRequest;
import ru.panyukovnn.tgchatscollector.dto.searchchat.SearchChatsResponse;
import ru.panyukovnn.tgchatscollector.dto.searchchat.SearchPrivateChatRequest;
import ru.panyukovnn.tgchatscollector.dto.searchchathistory.SearchChatHistoryResponse;
import ru.panyukovnn.tgchatscollector.dto.searchchathistory.SearchChatHistoryRequest;
import ru.panyukovnn.tgchatscollector.dto.common.CommonRequest;
import ru.panyukovnn.tgchatscollector.dto.common.CommonResponse;
import ru.panyukovnn.tgchatscollector.dto.lastchats.LastChatsResponse;
import ru.panyukovnn.tgchatscollector.service.handler.TgCollectorHandler;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TgChatLoaderController {

    private final TgCollectorHandler tgCollectorHandler;

    @GetMapping("/last-chats")
    public CommonResponse<LastChatsResponse> getLastChats(@RequestParam(name = "count", required = false, defaultValue = "10") Integer count) {
        LastChatsResponse lastChatsResponse = tgCollectorHandler.handleLastChats(count);

        return CommonResponse.<LastChatsResponse>builder()
            .body(lastChatsResponse)
            .build();
    }

    @PostMapping("/search-private-chat")
    public CommonResponse<SearchChatsResponse> postSearchPrivateChat(@RequestBody @Valid CommonRequest<SearchPrivateChatRequest> request) {
        SearchChatsResponse searchChatsResponse = tgCollectorHandler.handleFindPrivateChat(request.getBody());

        return CommonResponse.<SearchChatsResponse>builder()
            .body(searchChatsResponse)
            .build();
    }

    @PostMapping("/search-public-channel-by-id")
    public CommonResponse<SearchChatsResponse> postSearchPublicChannelById(@RequestBody @Valid CommonRequest<SearchPublicChannelByIdRequest> request) {
        SearchChatsResponse searchChatsResponse = tgCollectorHandler.handleFindPublicChannelById(request.getBody());

        return CommonResponse.<SearchChatsResponse>builder()
            .body(searchChatsResponse)
            .build();
    }

    @PostMapping("/search-chat-history")
    public CommonResponse<SearchChatHistoryResponse> postSearchChatHistory(@RequestBody @Valid CommonRequest<SearchChatHistoryRequest> searchChatHistory) {
        SearchChatHistoryResponse searchChatHistoryResponse = tgCollectorHandler.handleSearchChatHistoryByPeriod(searchChatHistory.getBody());

        return CommonResponse.<SearchChatHistoryResponse>builder()
            .body(searchChatHistoryResponse)
            .build();
    }
}
