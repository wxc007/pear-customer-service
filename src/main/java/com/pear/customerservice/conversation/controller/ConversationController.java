package com.pear.customerservice.conversation.controller;

import com.pear.customerservice.agent.service.AgentService;
import com.pear.customerservice.conversation.dto.ConversationEvent;
import com.pear.customerservice.conversation.dto.ConversationExchange;
import com.pear.customerservice.conversation.dto.GuestConversationResponse;
import com.pear.customerservice.conversation.dto.MessageResponse;
import com.pear.customerservice.conversation.dto.SendMessageRequest;
import com.pear.customerservice.conversation.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final AgentService agentService;

    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public GuestConversationResponse createGuestConversation() {
        //签发游客凭证并创建匿名会话
        return conversationService.createGuestConversation();
    }

    @PostMapping(value = "/{conversationId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public Flux<ServerSentEvent<ConversationEvent>> streamMessage(@PathVariable Long conversationId,
                                                                  @RequestHeader("X-Guest-Token") String guestToken,
                                                                  @Valid @RequestBody SendMessageRequest request) {
        //1.先持久化本轮用户消息和待回复消息，并校验游客会话归属
        ConversationExchange exchange = conversationService.startExchange(conversationId, guestToken, request.getContent());

        //2.通过 SSE 返回智能客服文本增量与完成事件
        return agentService.streamReply(exchange);
    }

    @GetMapping("/{conversationId}/messages")
    public List<MessageResponse> listMessages(@PathVariable Long conversationId,
                                              @RequestHeader("X-Guest-Token") String guestToken) {
        //校验游客归属后返回会话消息历史
        return conversationService.listMessages(conversationId, guestToken);
    }
}
