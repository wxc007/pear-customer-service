package com.pear.customerservice.conversation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ConversationExchange {

    private final Long conversationId;
    private final Long assistantMessageId;
    private final String contextSummary;
    private final List<MessageResponse> recentMessages;
}
