package com.shupai.multimodal.appentry.service.ppt;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shupai.multimodal.appentry.mapper.FileAssetMapper;
import com.shupai.multimodal.appentry.mapper.ModelServiceMapper;
import com.shupai.multimodal.appentry.model.entity.FileAssetEntity;
import com.shupai.multimodal.appentry.model.entity.ModelServiceEntity;
import com.shupai.multimodal.appentry.model.ppt.AiPptDeckPlan;
import com.shupai.multimodal.appentry.model.ppt.AiPptSlidePlan;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiPptWorkspaceService {

    private final ObjectMapper objectMapper;
    private final FileAssetMapper fileAssetMapper;
    private final ModelServiceMapper modelServiceMapper;
    private final PptRenderService pptRenderService;
    private final AiPptGenerationService aiPptGenerationService;
    private final PptTemplateService pptTemplateService;

    @Value("${multimodal.storage.root:./runtime-assets}")
    private String storageRoot;

    public Map<String, Object> render(AiPptDeckPlan deckPlan) {
        GeneratedPptx generated = pptRenderService.render(deckPlan);
        FileAssetEntity fileAsset = persistPptx(deckPlan, generated);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "PPT 已生成");
        payload.put("summary", "已根据 AI PPT 设计稿生成可编辑 PPTX 文件。");
        payload.put("chips", List.of("mode: ai_full", "file: pptx"));
        payload.put("actionLabel", "下载 PPTX");
        payload.put("kind", "file");
        payload.put("fileId", fileAsset.getFileId());
        payload.put("fileName", fileAsset.getFileName());
        payload.put("mimeType", fileAsset.getMimeType());
        payload.put("downloadUrl", fileAsset.getDownloadUrl());
        payload.put("previewImageUrl", fileAsset.getPreviewUrl());
        return payload;
    }

    public Map<String, Object> rewrite(AiPptDeckPlan deckPlan, String slideId, String instruction) {
        AiPptDeckPlan updated = aiPptGenerationService.rewriteSlide(deckPlan, slideId, instruction, resolveDeckModel(deckPlan));
        AiPptSlidePlan slide = updated.slides().stream().filter(item -> slideId.equals(item.id())).findFirst().orElse(null);
        return Map.of("deckPlan", updated, "slide", slide == null ? Map.of() : slide);
    }

    public Map<String, Object> optimize(AiPptDeckPlan deckPlan, String instruction) {
        return Map.of("deckPlan", aiPptGenerationService.optimizeDeck(deckPlan, instruction, resolveDeckModel(deckPlan)));
    }

    public Map<String, Object> changeTemplate(AiPptDeckPlan deckPlan, String templateCode) {
        return Map.of("deckPlan", pptTemplateService.applyTemplate(deckPlan, templateCode));
    }

    private ModelServiceEntity resolveDeckModel(AiPptDeckPlan deckPlan) {
        String serviceCode = deckPlan != null && deckPlan.deck() != null ? deckPlan.deck().serviceCode() : null;
        if (!StringUtils.hasText(serviceCode)) {
            return null;
        }
        return modelServiceMapper.selectOne(new LambdaQueryWrapper<ModelServiceEntity>()
                .eq(ModelServiceEntity::getServiceCode, serviceCode)
                .last("limit 1"));
    }

    private FileAssetEntity persistPptx(AiPptDeckPlan deckPlan, GeneratedPptx generated) {
        try {
            String fileId = generateId("file");
            Path relativePath = Paths.get("ppt-generation", LocalDate.now().toString(), fileId + ".pptx");
            Path absolutePath = resolveStorageRoot().resolve(relativePath);
            Files.createDirectories(absolutePath.getParent());
            Files.write(absolutePath, generated.bytes());

            String title = deckPlan != null && deckPlan.deck() != null && StringUtils.hasText(deckPlan.deck().title())
                    ? deckPlan.deck().title()
                    : "ai-ppt";
            FileAssetEntity fileAsset = new FileAssetEntity();
            fileAsset.setFileId(fileId);
            fileAsset.setRelatedType("ppt_deck");
            fileAsset.setRelatedId(fileId);
            fileAsset.setFileRole("result");
            fileAsset.setFileType("file");
            fileAsset.setFileName(sanitizeFileName(title) + ".pptx");
            fileAsset.setStorageKey(relativePath.toString().replace('\\', '/'));
            fileAsset.setFileSize((long) generated.bytes().length);
            fileAsset.setMimeType(generated.mimeType());
            fileAsset.setDownloadUrl("/api/workbench/files/" + fileId + "/download");
            fileAsset.setExtraMeta(toJson(Map.of("source", "ai_full", "deckTitle", title)));
            fileAssetMapper.insert(fileAsset);
            return fileAsset;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to persist AI PPTX", ex);
        }
    }

    private Path resolveStorageRoot() {
        return Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    private String sanitizeFileName(String value) {
        String cleaned = value.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return cleaned.isEmpty() ? "ai-ppt" : cleaned.substring(0, Math.min(80, cleaned.length()));
    }

    private String generateId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
