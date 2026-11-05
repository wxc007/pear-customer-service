package com.pear.customerservice.conversation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class GuestConversationResponse {

    private final Long id;
    private final String guestToken;
    private final Instant expiresAt;
}
