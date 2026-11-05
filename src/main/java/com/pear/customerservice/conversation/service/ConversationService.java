package com.pear.customerservice.conversation.service;

import com.pear.customerservice.conversation.dao.ConversationMessageRepository;
import com.pear.customerservice.conversation.dao.ConversationRepository;
import com.pear.customerservice.conversation.dto.ConversationExchange;
import com.pear.customerservice.conversation.dto.GuestConversationResponse;
import com.pear.customerservice.conversation.dto.MessageResponse;
import com.pear.customerservice.conversation.entity.ConversationEntity;
import com.pear.customerservice.conversation.entity.ConversationMessageEntity;
import com.pear.customerservice.conversation.entity.MessageRole;
import com.pear.customerservice.conversation.entity.MessageStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final Duration GUEST_TOKEN_TTL = Duration.ofHours(24);
    private static final int RECENT_MESSAGE_LIMIT = 20;
    private static final int SUMMARY_MAX_LENGTH = 4000;
    private static final int SUMMARY_ITEM_MAX_LENGTH = 240;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final Clock clock;

    @Transactional
    public GuestConversationResponse createGuestConversation() {
        //1.生成不可预测的游客凭证，只在数据库中保存其摘要
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String guestToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        //计算不可逆摘要，避免保存原始游客凭证
        String guestTokenHash = hashToken(guestToken);

        //2.创建有效期 24 小时的匿名会话
        Instant now = clock.instant();
        ConversationEntity conversation = new ConversationEntity(guestTokenHash, now.plus(GUEST_TOKEN_TTL), now);
        //保存匿名会话及其过期时间
        ConversationEntity savedConversation = conversationRepository.save(conversation);

        //3.仅在创建响应中返回一次原始游客凭证
        return new GuestConversationResponse(savedConversation.getId(), guestToken, savedConversation.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(Long conversationId, String guestToken) {
        //1.校验游客凭证与当前会话的归属关系及有效期
        authenticateGuest(conversationId, guestToken);

        //2.按消息顺序返回当前会话的完整历史
        return messageRepository.findByConversationIdOrderByIdAsc(conversationId).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public ConversationExchange startExchange(Long conversationId, String guestToken, String userContent) {
        //1.校验当前消息只能写入游客自己的有效会话
        ConversationEntity conversation = authenticateGuest(conversationId, guestToken);

        //2.先持久化用户消息，再创建可关联 SSE 事件的待回复消息
        ConversationMessageEntity userMessage = new ConversationMessageEntity(
                conversationId, MessageRole.USER, userContent, MessageStatus.COMPLETED, clock.instant()
        );
        //持久化本轮用户消息
        messageRepository.save(userMessage);
        ConversationMessageEntity assistantMessage = new ConversationMessageEntity(
                conversationId, MessageRole.ASSISTANT, "", MessageStatus.STREAMING, clock.instant()
        );
        //持久化待回复占位消息并取得事件关联编号
        ConversationMessageEntity savedAssistantMessage = messageRepository.save(assistantMessage);

        //3.读取全量历史并排除待回复占位消息，只保留最近 20 条作为模型上下文
        List<ConversationMessageEntity> history = messageRepository.findByConversationIdOrderByIdAsc(conversationId)
                .stream()
                .filter(message -> message.getStatus() != MessageStatus.STREAMING)
                .toList();
        int recentStart = Math.max(0, history.size() - RECENT_MESSAGE_LIMIT);
        List<MessageResponse> recentMessages = history.subList(recentStart, history.size()).stream()
                .map(this::toMessageResponse)
                .toList();

        //4.将超出近期窗口的较早消息压缩为滚动摘要并持久化
        String contextSummary = buildRollingSummary(history.subList(0, recentStart));
        conversation.setContextSummary(contextSummary);
        //保存本轮滚动摘要
        conversationRepository.save(conversation);

        //5.返回已持久化的回复编号、近期消息与摘要供 Agent 编排
        return new ConversationExchange(conversationId, savedAssistantMessage.getId(), contextSummary, recentMessages);
    }

    @Transactional
    public void completeAssistantMessage(Long assistantMessageId, String content) {
        //将流式回复一次性补全为可恢复的完整消息
        updateAssistantMessage(assistantMessageId, content, MessageStatus.COMPLETED);
    }

    @Transactional
    public void failAssistantMessage(Long assistantMessageId, String content) {
        //保存可恢复的失败结果，避免将半段模型输出误认为成功答复
        updateAssistantMessage(assistantMessageId, content, MessageStatus.FAILED);
    }

    private void updateAssistantMessage(Long assistantMessageId, String content, MessageStatus status) {
        //1.查询已持久化的待回复消息，不存在时明确终止状态更新
        ConversationMessageEntity message = messageRepository.findById(assistantMessageId)
                .orElseThrow(() -> new IllegalStateException("待回复消息不存在，无法保存智能客服处理结果"));

        //2.写入最终文本与完成或失败状态，使历史接口可以恢复本轮结果
        message.setContent(content);
        message.setStatus(status);

        //3.保存回复消息终态
        messageRepository.save(message);
    }

    private String buildRollingSummary(List<ConversationMessageEntity> olderMessages) {
        //逐条压缩超出窗口的消息，并优先保留较晚背景，摘要仅用于对话连续性
        String summary = olderMessages.stream()
                .map(message -> {
                    String role = message.getRole() == MessageRole.USER ? "顾客" : "智能客服";
                    String content = message.getContent();
                    String shortened = content.length() <= SUMMARY_ITEM_MAX_LENGTH
                            ? content
                            : content.substring(0, SUMMARY_ITEM_MAX_LENGTH) + "…";
                    return role + "：" + shortened;
                })
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
        if (summary.length() <= SUMMARY_MAX_LENGTH) {
            return summary;
        }
        return "...\n" + summary.substring(summary.length() - (SUMMARY_MAX_LENGTH - 4));
    }

    private MessageResponse toMessageResponse(ConversationMessageEntity message) {
        return new MessageResponse(
                message.getId(), message.getRole(), message.getContent(), message.getStatus(), message.getCreatedAt()
        );
    }

    private ConversationEntity authenticateGuest(Long conversationId, String guestToken) {
        //查询会话并拒绝无效、跨会话或已过期的游客凭证
        ConversationEntity conversation = conversationRepository.findById(conversationId)
                .orElseThrow(this::invalidGuestToken);
        byte[] expectedHash = HexFormat.of().parseHex(conversation.getGuestTokenHash());
        //计算待校验凭证摘要以执行常量时间比较
        String actualTokenHash = hashToken(guestToken);
        byte[] actualHash = HexFormat.of().parseHex(actualTokenHash);
        if (!MessageDigest.isEqual(expectedHash, actualHash) || !conversation.getExpiresAt().isAfter(clock.instant())) {
            throw invalidGuestToken();
        }
        return conversation;
    }

    private ResponseStatusException invalidGuestToken() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "游客凭证无效或已过期，请重新创建会话");
    }

    private String hashToken(String guestToken) {
        try {
            //使用 SHA-256 避免明文游客凭证进入数据库
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(guestToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256，无法签发游客凭证", exception);
        }
    }
}
