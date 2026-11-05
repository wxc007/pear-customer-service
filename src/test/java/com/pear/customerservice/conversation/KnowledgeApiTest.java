package com.pear.customerservice.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pear.customerservice.conversation.ConversationApiTest.StubChatModel;
import com.pear.customerservice.knowledge.dao.KnowledgeChunkRepository;
import com.pear.customerservice.knowledge.dao.KnowledgeVersionRepository;
import com.pear.customerservice.knowledge.entity.KnowledgeChunkEntity;
import com.pear.customerservice.knowledge.entity.KnowledgePublicationStatus;
import com.pear.customerservice.knowledge.entity.KnowledgeVersionEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.StringUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.qdrant.QdrantContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.ai.model.embedding=openai",
        "spring.ai.vectorstore.qdrant.collection-name=pear_knowledge_test",
        "spring.ai.vectorstore.qdrant.initialize-schema=true",
        "app.agent.model-timeout=1s",
        "app.knowledge.similarity-threshold=0.75"
})
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Testcontainers
@Import({ConversationApiTest.StubModelConfiguration.class, KnowledgeApiTest.StubEmbeddingConfiguration.class})
class KnowledgeApiTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("pear_customer_service_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    @ServiceConnection
    static QdrantContainer qdrant = new QdrantContainer("qdrant/qdrant:v1.12.6");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StubChatModel chatModel;

    @Autowired
    private StubEmbeddingModel embeddingModel;

    @Autowired
    private KnowledgeVersionRepository versionRepository;

    @Autowired
    private KnowledgeChunkRepository chunkRepository;

    @Test
    void publishedPolicyAnswerIncludesTraceableDocumentVersion() throws Exception {
        GuestSession guest = createGuestSession();
        chatModel.respondWith("鲜梨品质问题可以申请售后，最终以人工审核为准");

        String events = sendMessage(guest, "鲜梨售后怎么处理");

        assertThat(events).contains("event:complete", "来源", "鲜梨品质售后政策", "2026.1");
        SystemMessage systemMessage = chatModel.getLastPrompt().getInstructions().stream()
                .filter(SystemMessage.class::isInstance)
                .map(SystemMessage.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(StringUtils.countOccurrencesOf(systemMessage.getText(), "[资料片段")).isEqualTo(5);

        mockMvc.perform(get("/api/v1/conversations/{id}/messages", guest.id)
                        .header("X-Guest-Token", guest.guestToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].content").value(
                        "鲜梨品质问题可以申请售后，最终以人工审核为准\n\n来源：\n- 《鲜梨品质售后政策》v2026.1"
                ));
    }

    @ParameterizedTest
    @CsvSource({
            "鲜梨如何储存, 鲜梨储存与食用指南",
            "鲜梨怎么保存, 鲜梨储存与食用指南",
            "梨膏怎么食用, 梨制品售后与食用说明"
    })
    void storageAndEatingAnswersUsePublishedGuideSources(String question, String expectedSource) throws Exception {
        GuestSession guest = createGuestSession();
        chatModel.respondWith("根据已发布资料回答");

        String events = sendMessage(guest, question);

        assertThat(events).contains("event:complete", expectedSource, "2026.1");
    }

    @Test
    void lowRelevanceRetrievalReturnsUncertaintyWithoutCallingModel() throws Exception {
        GuestSession guest = createGuestSession();
        chatModel.respondWith("不应使用无关资料生成答案");

        String events = sendMessage(guest, "鲜梨储存能用微波炉处理吗");

        assertThat(events).contains("event:complete", "现有已发布资料不足", "无法可靠回答");
        assertThat(events).doesNotContain("0-4°C", "不应使用无关资料");
        assertThat(chatModel.getLastPrompt()).isNull();
    }

    @Test
    void dynamicOrderRefundQuestionBypassesStaticKnowledgeRetrieval() throws Exception {
        GuestSession guest = createGuestSession();
        embeddingModel.clearEmbeddedTexts();
        chatModel.respondWith("订单退款状态需要通过实时业务记录查询");

        String events = sendMessage(guest, "订单退款状态");

        assertThat(events).contains("订单退款状态需要通过实时业务记录查询");
        assertThat(embeddingModel.getEmbeddedTexts()).isEmpty();
    }

    @Test
    void disabledVersionCannotProvideAnswerEvenWhenVectorRemains() throws Exception {
        GuestSession guest = createGuestSession();
        chatModel.respondWith("不应使用已停用版本");
        KnowledgeVersionEntity version = versionRepository.findById(1L).orElseThrow();
        version.setStatus(KnowledgePublicationStatus.DISABLED);
        versionRepository.save(version);
        try {
            String events = sendMessage(guest, "鲜梨售后怎么处理");

            assertThat(events).contains("现有已发布资料不足");
            assertThat(events).doesNotContain("不应使用已停用版本");
            assertThat(chatModel.getLastPrompt()).isNull();
        } finally {
            version.setStatus(KnowledgePublicationStatus.PUBLISHED);
            versionRepository.save(version);
        }
    }

    @Test
    void staleVectorTextCannotOverrideMysqlChunkFacts() throws Exception {
        GuestSession guest = createGuestSession();
        chatModel.respondWith("不应使用与事实源不一致的向量正文");
        List<KnowledgeChunkEntity> chunks = chunkRepository.findByVersionIdOrderByChunkIndexAsc(1L);
        List<String> originalContents = chunks.stream().map(KnowledgeChunkEntity::getContent).toList();
        for (int index = 0; index < chunks.size(); index++) {
            chunks.get(index).setContent("与 Qdrant 不一致的过期正文" + index);
        }
        chunkRepository.saveAll(chunks);
        try {
            String events = sendMessage(guest, "鲜梨售后怎么处理");

            assertThat(events).contains("现有已发布资料不足");
            assertThat(events).doesNotContain("过期正文", "不应使用与事实源不一致的向量正文");
            assertThat(chatModel.getLastPrompt()).isNull();
        } finally {
            for (int index = 0; index < chunks.size(); index++) {
                chunks.get(index).setContent(originalContents.get(index));
            }
            chunkRepository.saveAll(chunks);
        }
    }

    @Test
    void maliciousKnowledgeTextCannotGrantAdditionalToolCapability() throws Exception {
        GuestSession guest = createGuestSession();
        chatModel.requestUnknownTool();

        String events = sendMessage(guest, "鲜梨售后怎么处理");

        assertThat(events).contains("event:error", "智能客服暂时不可用");
        assertThat(events).doesNotContain("event:complete", "已执行", "赔付成功");
    }

    @Test
    void qdrantFailureReturnsHonestKnowledgeUnavailableResult() throws Exception {
        GuestSession guest = createGuestSession();
        chatModel.respondWith("不应伪造政策答案");
        try {
            qdrant.stop();

            String events = sendMessage(guest, "鲜梨如何储存");

            assertThat(events).contains("event:error", "知识资料暂时无法可靠检索");
            assertThat(events).doesNotContain("event:complete", "0-4°C", "不应伪造政策答案");
            assertThat(chatModel.getLastPrompt()).isNull();
        } finally {
            qdrant.start();
        }
    }

    private GuestSession createGuestSession() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/conversations/guest"))
                .andExpect(status().isCreated())
                .andReturn();
        String body = created.getResponse().getContentAsString();
        Number idValue = com.jayway.jsonpath.JsonPath.read(body, "$.id");
        String guestToken = com.jayway.jsonpath.JsonPath.read(body, "$.guestToken");
        return new GuestSession(idValue.longValue(), guestToken);
    }

    private String sendMessage(GuestSession guest, String message) throws Exception {
        MvcResult streaming = mockMvc.perform(post("/api/v1/conversations/{id}/messages/stream", guest.id)
                        .header("X-Guest-Token", guest.guestToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(objectMapper.writeValueAsString(Map.of("content", message))))
                .andExpect(request().asyncStarted())
                .andReturn();
        byte[] eventBytes = mockMvc.perform(asyncDispatch(streaming))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        return new String(eventBytes, StandardCharsets.UTF_8);
    }

    private static class GuestSession {

        private final Long id;
        private final String guestToken;

        GuestSession(Long id, String guestToken) {
            this.id = id;
            this.guestToken = guestToken;
        }
    }

    @TestConfiguration
    static class StubEmbeddingConfiguration {

        @Bean
        @Primary
        StubEmbeddingModel stubEmbeddingModel() {
            return new StubEmbeddingModel();
        }
    }

    static class StubEmbeddingModel implements EmbeddingModel {

        private final List<String> embeddedTexts = new ArrayList<>();

        void clearEmbeddedTexts() {
            embeddedTexts.clear();
        }

        List<String> getEmbeddedTexts() {
            return List.copyOf(embeddedTexts);
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            List<Embedding> embeddings = new ArrayList<>();
            for (int index = 0; index < request.getInstructions().size(); index++) {
                embeddings.add(new Embedding(vector(request.getInstructions().get(index)), index));
            }
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return vector(document.getText());
        }

        @Override
        public int dimensions() {
            return 8;
        }

        private float[] vector(String text) {
            embeddedTexts.add(text);
            float[] vector = new float[8];
            if (text.contains("微波炉")) {
                vector[7] = 1;
            } else if (text.contains("梨制品") || text.contains("梨膏")) {
                vector[3] = 1;
            } else if (text.contains("储存") || text.contains("保存") || text.contains("冷藏")) {
                vector[1] = 1;
            } else if (text.contains("食用") || text.contains("配料") || text.contains("保质期")) {
                vector[2] = 1;
            } else if (text.contains("恶意指令示例")) {
                vector[0] = 1;
            } else if (text.contains("售后") || text.contains("坏果") || text.contains("品质")
                    || text.contains("退款") || text.contains("补发") || text.contains("审核")) {
                vector[0] = 0.8f;
                vector[6] = 0.6f;
            } else {
                vector[6] = 1;
            }
            return vector;
        }
    }
}
