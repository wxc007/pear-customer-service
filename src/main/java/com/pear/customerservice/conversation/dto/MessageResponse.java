package com.pear.customerservice.conversation.dto;

import com.pear.customerservice.conversation.entity.MessageRole;
import com.pear.customerservice.conversation.entity.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class MessageResponse {

    private final Long id;
    private final MessageRole role;
    private final String content;
    private final MessageStatus status;
    private final Instant createdAt;
}
