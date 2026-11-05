package com.pear.customerservice.conversation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConversationEvent {

    private final Long conversationId;
    private final Long messageId;
    private final String content;
}
