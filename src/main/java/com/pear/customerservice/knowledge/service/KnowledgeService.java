package com.pear.customerservice.knowledge.service;

import com.pear.customerservice.knowledge.dao.KnowledgeChunkRepository;
import com.pear.customerservice.knowledge.dao.KnowledgeDocumentRepository;
import com.pear.customerservice.knowledge.dao.KnowledgeVersionRepository;
import com.pear.customerservice.knowledge.dto.KnowledgeRetrievalResult;
import com.pear.customerservice.knowledge.entity.KnowledgeChunkEntity;
import com.pear.customerservice.knowledge.entity.KnowledgeDocumentEntity;
import com.pear.customerservice.knowledge.entity.KnowledgeIndexStatus;
import com.pear.customerservice.knowledge.entity.KnowledgePublicationStatus;
import com.pear.customerservice.knowledge.entity.KnowledgeRetrievalStatus;
import com.pear.customerservice.knowledge.entity.KnowledgeVersionEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class KnowledgeService {

    private static final int DEFAULT_TOP_K = 5;
    private static final List<String> KNOWLEDGE_KEYWORDS = List.of(
            "售后", "退货", "退款", "补发", "坏果", "变质", "储存", "保存", "冷藏",
            "食用", "怎么吃", "怎么喝", "营养", "保质期", "配料"
    );
    private static final List<String> DYNAMIC_FACT_KEYWORDS = List.of(
            "订单", "库存", "价格", "多少钱", "物流", "支付"
    );

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeVersionRepository versionRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final double similarityThreshold;

    public KnowledgeService(ObjectProvider<VectorStore> vectorStoreProvider,
                            KnowledgeDocumentRepository documentRepository,
                            KnowledgeVersionRepository versionRepository,
                            KnowledgeChunkRepository chunkRepository,
                            @Value("${app.knowledge.similarity-threshold:0.75}") double similarityThreshold) {
        this.vectorStoreProvider = vectorStoreProvider;
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.chunkRepository = chunkRepository;
        this.similarityThreshold = similarityThreshold;
    }

    public KnowledgeRetrievalResult retrieve(String query) {
        //1.仅政策、储存和食用问题进入知识链路，动态商品事实仍走实时工具
        if (!isKnowledgeQuestion(query)) {
            return KnowledgeRetrievalResult.empty(KnowledgeRetrievalStatus.NOT_REQUESTED);
        }

        //2.未配置向量库时明确降级，不用模型记忆替代知识检索
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            return KnowledgeRetrievalResult.empty(KnowledgeRetrievalStatus.UNAVAILABLE);
        }

        try {
            //3.按默认最相关 5 个片段和配置阈值执行向量检索
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(DEFAULT_TOP_K)
                    .similarityThreshold(similarityThreshold)
                    .build();
            List<Document> documents = vectorStore.similaritySearch(request);

            //4.结合 MySQL 发布与索引状态构造可靠上下文和版本来源
            return buildAvailableResult(documents);
        } catch (RuntimeException exception) {
            log.warn("知识向量检索不可用，请检查 Qdrant 与嵌入模型配置", exception);
            return KnowledgeRetrievalResult.empty(KnowledgeRetrievalStatus.UNAVAILABLE);
        }
    }

    private KnowledgeRetrievalResult buildAvailableResult(List<Document> documents) {
        //1.准备受限知识上下文与去重后的版本来源
        StringBuilder context = new StringBuilder();
        Set<String> sources = new LinkedHashSet<>();
        int fragmentNumber = 0;

        //2.逐片段核对 MySQL 正文、版本归属、发布状态和索引状态
        for (Document document : documents) {
            Object versionIdValue = document.getMetadata().get("versionId");
            Object chunkIdValue = document.getMetadata().get("chunkId");
            if (!(versionIdValue instanceof Number versionIdNumber) || !(chunkIdValue instanceof Number chunkIdNumber)) {
                continue;
            }

            //按 chunkId 回查事实源并核对向量正文与版本归属
            KnowledgeChunkEntity chunk = chunkRepository.findById(chunkIdNumber.longValue()).orElse(null);
            if (chunk == null || !chunk.getVersionId().equals(versionIdNumber.longValue())
                    || !chunk.getContent().equals(document.getText())) {
                continue;
            }

            //仅接受 MySQL 中仍为已发布且索引成功的版本
            KnowledgeVersionEntity version = versionRepository.findByIdAndStatusAndIndexStatus(
                    versionIdNumber.longValue(), KnowledgePublicationStatus.PUBLISHED, KnowledgeIndexStatus.INDEXED
            ).orElse(null);
            if (version == null) {
                continue;
            }

            //从事实源读取文档标题，不信任向量元数据中的展示信息
            KnowledgeDocumentEntity knowledgeDocument = documentRepository.findById(version.getDocumentId()).orElse(null);
            if (knowledgeDocument == null) {
                continue;
            }

            fragmentNumber++;
            context.append("[资料片段").append(fragmentNumber).append("] ")
                    .append(chunk.getContent()).append('\n');
            sources.add("《" + knowledgeDocument.getTitle() + "》v" + version.getVersionNumber());
        }

        //3.没有通过事实源校验的片段时按检索不足处理
        if (context.isEmpty()) {
            return KnowledgeRetrievalResult.empty(KnowledgeRetrievalStatus.NO_RELEVANT_CONTENT);
        }

        //4.使用 MySQL 文档标题与版本号构造可追溯来源
        String sourceAppendix = "\n\n来源：\n- " + String.join("\n- ", sources);
        return new KnowledgeRetrievalResult(KnowledgeRetrievalStatus.AVAILABLE, context.toString(), sourceAppendix);
    }

    private boolean isKnowledgeQuestion(String query) {
        //订单、价格、库存、支付和物流始终优先走实时业务工具
        if (DYNAMIC_FACT_KEYWORDS.stream().anyMatch(query::contains)) {
            return false;
        }
        return KNOWLEDGE_KEYWORDS.stream().anyMatch(query::contains);
    }
}
