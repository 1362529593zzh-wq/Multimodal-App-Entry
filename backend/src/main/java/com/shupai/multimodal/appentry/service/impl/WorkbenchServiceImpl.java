package com.shupai.multimodal.appentry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shupai.multimodal.appentry.mapper.ChatMessageMapper;
import com.shupai.multimodal.appentry.mapper.ChatSessionMapper;
import com.shupai.multimodal.appentry.mapper.FileAssetMapper;
import com.shupai.multimodal.appentry.mapper.FunctionConfigMapper;
import com.shupai.multimodal.appentry.mapper.ModelServiceMapper;
import com.shupai.multimodal.appentry.mapper.TaskMapper;
import com.shupai.multimodal.appentry.mapper.TaskResultMapper;
import com.shupai.multimodal.appentry.model.dto.ConversationCreateRequest;
import com.shupai.multimodal.appentry.model.dto.WorkbenchChatRequest;
import com.shupai.multimodal.appentry.model.entity.ChatMessageEntity;
import com.shupai.multimodal.appentry.model.entity.ChatSessionEntity;
import com.shupai.multimodal.appentry.model.entity.FileAssetEntity;
import com.shupai.multimodal.appentry.model.entity.FunctionConfigEntity;
import com.shupai.multimodal.appentry.model.entity.ModelServiceEntity;
import com.shupai.multimodal.appentry.model.entity.TaskEntity;
import com.shupai.multimodal.appentry.model.entity.TaskResultEntity;
import com.shupai.multimodal.appentry.model.vo.ChatMessageVO;
import com.shupai.multimodal.appentry.model.vo.ConversationVO;
import com.shupai.multimodal.appentry.model.vo.TaskVO;
import com.shupai.multimodal.appentry.model.vo.WorkbenchChatSubmitVO;
import com.shupai.multimodal.appentry.model.vo.WorkbenchFileResource;
import com.shupai.multimodal.appentry.model.vo.WorkbenchFunctionVO;
import com.shupai.multimodal.appentry.service.WorkbenchService;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class WorkbenchServiceImpl implements WorkbenchService {

    private static final String DEFAULT_CONVERSATION_TITLE = "新建工作台会话";
    private static final String CAPABILITY_IMAGE_GENERATION = "image_generation";
    private static final String DEFAULT_IMAGE_MIME_TYPE = "image/png";
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };

    private final FunctionConfigMapper functionConfigMapper;
    private final ModelServiceMapper modelServiceMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final TaskMapper taskMapper;
    private final TaskResultMapper taskResultMapper;
    private final FileAssetMapper fileAssetMapper;
    private final ObjectMapper objectMapper;
    @Qualifier("workbenchTaskExecutor")
    private final Executor workbenchTaskExecutor;

    @Value("${multimodal.storage.root:./runtime-assets}")
    private String storageRoot;

    @Value("${multimodal.image.demo-endpoint:https://image.pollinations.ai/prompt}")
    private String imageDemoEndpoint;

    @Value("${multimodal.image.demo-model:flux}")
    private String imageDemoModel;

    @Override
    public List<WorkbenchFunctionVO> listFunctions() {
        LambdaQueryWrapper<FunctionConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FunctionConfigEntity::getEnabled, true)
                .orderByAsc(FunctionConfigEntity::getSortOrder)
                .orderByDesc(FunctionConfigEntity::getCreatedAt);
        List<FunctionConfigEntity> functions = functionConfigMapper.selectList(wrapper);
        if (functions.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> serviceCodes = functions.stream()
                .map(FunctionConfigEntity::getDefaultServiceCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        Map<String, ModelServiceEntity> serviceMap = serviceCodes.isEmpty()
                ? Collections.emptyMap()
                : modelServiceMapper.selectList(new LambdaQueryWrapper<ModelServiceEntity>()
                        .in(ModelServiceEntity::getServiceCode, serviceCodes))
                .stream()
                .collect(Collectors.toMap(ModelServiceEntity::getServiceCode, Function.identity()));

        return functions.stream()
                .map(entity -> toWorkbenchFunctionVO(entity, serviceMap.get(entity.getDefaultServiceCode())))
                .toList();
    }

    @Override
    @Transactional
    public ConversationVO createConversation(ConversationCreateRequest request) {
        ChatSessionEntity entity = new ChatSessionEntity();
        entity.setSessionId(generateId("conv"));
        entity.setTitle(StringUtils.hasText(request.title()) ? request.title().trim() : DEFAULT_CONVERSATION_TITLE);
        entity.setStatus("ACTIVE");
        chatSessionMapper.insert(entity);
        return toConversationVO(entity);
    }

    @Override
    public ConversationVO getConversation(String conversationId) {
        return toConversationVO(findConversation(conversationId));
    }

    @Override
    public List<ChatMessageVO> listMessages(String conversationId) {
        findConversation(conversationId);
        taskMapper.selectList(new LambdaQueryWrapper<TaskEntity>()
                        .eq(TaskEntity::getSessionId, conversationId)
                        .orderByAsc(TaskEntity::getCreatedAt))
                .forEach(this::syncTaskLifecycle);
        return chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessageEntity>()
                        .eq(ChatMessageEntity::getSessionId, conversationId)
                        .orderByAsc(ChatMessageEntity::getSequenceNo)
                        .orderByAsc(ChatMessageEntity::getCreatedAt))
                .stream()
                .map(this::toChatMessageVO)
                .toList();
    }

    @Override
    @Transactional
    public WorkbenchChatSubmitVO submitChat(WorkbenchChatRequest request) {
        ChatSessionEntity conversation = resolveConversation(request.conversationId());
        FunctionConfigEntity capability = resolveCapability(request.capability(), conversation);
        ModelServiceEntity service = resolveService(capability, request.model());

        int userSequenceNo = nextSequenceNo(conversation.getSessionId());
        ChatMessageEntity userMessage = new ChatMessageEntity();
        userMessage.setMessageId(generateId("msg"));
        userMessage.setSessionId(conversation.getSessionId());
        userMessage.setMessageType("user");
        userMessage.setContentType("text");
        userMessage.setContentText(request.inputText().trim());
        userMessage.setSequenceNo(userSequenceNo);
        chatMessageMapper.insert(userMessage);

        TaskEntity task = new TaskEntity();
        task.setTaskId(generateId("task"));
        task.setSessionId(conversation.getSessionId());
        task.setMessageId(userMessage.getMessageId());
        task.setParentTaskId(request.parentTaskId());
        task.setFunctionCode(capability.getFunctionCode());
        task.setSelectionMode(StringUtils.hasText(request.selectionMode()) ? request.selectionMode() : "manual");
        task.setResolvedIntent(capability.getFunctionCode());
        task.setServiceCode(service.getServiceCode());
        task.setModelName(service.getModelName());
        task.setInputText(request.inputText().trim());
        task.setRequestParams(StringUtils.hasText(request.options()) ? request.options() : "{}");
        task.setInputAssetIds("[]");
        task.setInheritanceMode(StringUtils.hasText(request.inheritanceMode()) ? request.inheritanceMode() : "none");
        task.setSourceAssetIds(StringUtils.hasText(request.sourceAssetIds()) ? request.sourceAssetIds() : "[]");
        task.setStatus("PENDING");
        taskMapper.insert(task);

        ChatMessageEntity taskMessage = new ChatMessageEntity();
        taskMessage.setMessageId(generateId("msg"));
        taskMessage.setSessionId(conversation.getSessionId());
        taskMessage.setMessageType("system");
        taskMessage.setContentType("status");
        taskMessage.setContentText("任务已创建，正在进入工作台主链路。");
        taskMessage.setRelatedTaskId(task.getTaskId());
        taskMessage.setSequenceNo(userSequenceNo + 1);
        chatMessageMapper.insert(taskMessage);

        conversation.setLastFunctionCode(capability.getFunctionCode());
        conversation.setLastServiceCode(service.getServiceCode());
        chatSessionMapper.updateById(conversation);

        dispatchTaskAfterCommit(task.getTaskId());

        return WorkbenchChatSubmitVO.builder()
                .conversationId(conversation.getSessionId())
                .userMessageId(userMessage.getMessageId())
                .taskMessageId(taskMessage.getMessageId())
                .taskId(task.getTaskId())
                .resolvedCapability(capability.getFunctionCode())
                .resolvedServiceCode(service.getServiceCode())
                .resolvedModel(service.getModelName())
                .status(task.getStatus())
                .build();
    }

    @Override
    public TaskVO getTask(String taskId) {
        TaskEntity entity = findTask(taskId);
        syncTaskLifecycle(entity);
        return toTaskVO(entity);
    }

    @Override
    public WorkbenchFileResource previewFile(String fileId) {
        return loadFileResource(fileId);
    }

    @Override
    public WorkbenchFileResource downloadFile(String fileId) {
        return loadFileResource(fileId);
    }

    private void dispatchTaskAfterCommit(String taskId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                workbenchTaskExecutor.execute(() -> executeTask(taskId));
            }
        });
    }

    private void executeTask(String taskId) {
        TaskEntity task = findTask(taskId);
        try {
            if (CAPABILITY_IMAGE_GENERATION.equals(task.getFunctionCode())) {
                executeImageGenerationTask(task);
                return;
            }
            simulateTaskLifecycle(task);
        } catch (Exception ex) {
            markTaskFailed(task, ex.getMessage() == null ? "任务执行失败" : ex.getMessage(), null);
        }
    }

    private void executeImageGenerationTask(TaskEntity task) {
        task = findTask(task.getTaskId());
        updateTaskToRunning(task);

        ModelServiceEntity service = modelServiceMapper.selectOne(new LambdaQueryWrapper<ModelServiceEntity>()
                .eq(ModelServiceEntity::getServiceCode, task.getServiceCode())
                .last("limit 1"));
        if (service == null) {
            throw new IllegalStateException("未找到图像生成服务配置");
        }

        Map<String, Object> options = parseRequestParams(task.getRequestParams());
        GeneratedImagePayload payload = generateImagePayload(task, service, options);
        FileAssetEntity fileAsset = persistGeneratedImage(task, service, options, payload);
        String resultPayload = buildResultPayload(task, service, options, fileAsset, payload.revisedPrompt());
        upsertTaskResult(task, fileAsset, resultPayload, payload.rawResponse());
        markTaskSucceeded(task, resultPayload);
    }

    private GeneratedImagePayload generateImagePayload(TaskEntity task, ModelServiceEntity service, Map<String, Object> options) {
        if (useDemoImageProvider(service)) {
            return generateViaDemoProvider(task, service, options);
        }
        return generateViaRemoteService(task, service, options);
    }

    private GeneratedImagePayload generateViaDemoProvider(TaskEntity task, ModelServiceEntity service, Map<String, Object> options) {
        String ratio = readStringOption(options, "ratio");
        ImageSize size = resolveImageSize(ratio);
        String prompt = task.getInputText().trim();
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(trimTrailingSlash(imageDemoEndpoint))
                .append("/")
                .append(URLEncoder.encode(prompt, StandardCharsets.UTF_8));
        List<String> queryParts = new ArrayList<>();
        queryParts.add("width=" + size.width());
        queryParts.add("height=" + size.height());
        queryParts.add("model=" + URLEncoder.encode(imageDemoModel, StandardCharsets.UTF_8));
        String style = readStringOption(options, "style");
        if (StringUtils.hasText(style)) {
            queryParts.add("style=" + URLEncoder.encode(style, StandardCharsets.UTF_8));
        }
        queryParts.add("nologo=true");
        queryParts.add("seed=" + Math.abs(prompt.hashCode()));
        urlBuilder.append("?").append(String.join("&", queryParts));

        DownloadedImage image = downloadBinary(URI.create(urlBuilder.toString()), Duration.ofSeconds(resolveTimeoutSeconds(service)));
        Map<String, Object> rawResponse = new LinkedHashMap<>();
        rawResponse.put("provider", "demo");
        rawResponse.put("endpoint", imageDemoEndpoint);
        rawResponse.put("model", imageDemoModel);
        rawResponse.put("mimeType", image.mimeType());
        rawResponse.put("size", size.width() + "x" + size.height());

        return new GeneratedImagePayload(image.bytes(), image.mimeType(), null, toJson(rawResponse));
    }

    private GeneratedImagePayload generateViaRemoteService(TaskEntity task, ModelServiceEntity service, Map<String, Object> options) {
        URI endpoint = URI.create(service.getEndpoint());
        Map<String, Object> requestBody = buildRemoteRequestBody(task, service, options);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(resolveTimeoutSeconds(service)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(requestBody)));

        if ("bearer".equalsIgnoreCase(service.getAuthType())) {
            requestBuilder.header("Authorization", "Bearer " + resolveBearerToken(service));
        }

        HttpResponse<String> response = sendTextRequest(requestBuilder.build());
        JsonNode root = parseJsonNode(response.body());

        String revisedPrompt = extractText(root, "revised_prompt");
        byte[] imageBytes = extractImageBytes(root);
        String mimeType = extractImageMimeType(root);
        if (imageBytes == null) {
            String imageUrl = extractImageUrl(root);
            if (!StringUtils.hasText(imageUrl)) {
                throw new IllegalStateException("图像生成服务未返回可识别的图片结果");
            }
            DownloadedImage downloadedImage = downloadBinary(URI.create(imageUrl), Duration.ofSeconds(resolveTimeoutSeconds(service)));
            imageBytes = downloadedImage.bytes();
            mimeType = downloadedImage.mimeType();
        }

        return new GeneratedImagePayload(imageBytes, normalizeMimeType(mimeType), revisedPrompt, response.body());
    }

    private Map<String, Object> buildRemoteRequestBody(TaskEntity task, ModelServiceEntity service, Map<String, Object> options) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", StringUtils.hasText(service.getModelCode()) ? service.getModelCode() : service.getModelName());
        requestBody.put("prompt", task.getInputText());

        String ratio = readStringOption(options, "ratio");
        if (StringUtils.hasText(ratio)) {
            ImageSize size = resolveImageSize(ratio);
            requestBody.put("size", size.width() + "x" + size.height());
        }
        String style = readStringOption(options, "style");
        if (StringUtils.hasText(style)) {
            requestBody.put("style", style);
        }
        String template = readStringOption(options, "template");
        if (StringUtils.hasText(template)) {
            requestBody.put("template", template);
        }
        requestBody.put("n", 1);
        requestBody.put("response_format", "b64_json");
        return requestBody;
    }

    private FileAssetEntity persistGeneratedImage(
            TaskEntity task,
            ModelServiceEntity service,
            Map<String, Object> options,
            GeneratedImagePayload payload
    ) {
        try {
            Path storageDirectory = resolveStorageRoot()
                    .resolve("image-generation")
                    .resolve(LocalDate.now().toString());
            Files.createDirectories(storageDirectory);

            String fileId = generateId("file");
            String extension = mimeTypeToExtension(payload.mimeType());
            String fileName = task.getTaskId() + "." + extension;
            Path relativePath = Paths.get("image-generation", LocalDate.now().toString(), fileId + "." + extension);
            Path absolutePath = resolveStorageRoot().resolve(relativePath);
            Files.write(absolutePath, payload.bytes());

            FileAssetEntity fileAsset = new FileAssetEntity();
            fileAsset.setFileId(fileId);
            fileAsset.setRelatedType("task");
            fileAsset.setRelatedId(task.getTaskId());
            fileAsset.setFileRole("result");
            fileAsset.setFileType("image");
            fileAsset.setFileName(fileName);
            fileAsset.setStorageKey(relativePath.toString().replace('\\', '/'));
            fileAsset.setFileSize((long) payload.bytes().length);
            fileAsset.setMimeType(payload.mimeType());
            fileAsset.setPreviewUrl("/api/workbench/files/" + fileId + "/preview");
            fileAsset.setDownloadUrl("/api/workbench/files/" + fileId + "/download");
            fileAsset.setSourceTaskId(task.getTaskId());

            Map<String, Object> extraMeta = new LinkedHashMap<>();
            extraMeta.put("serviceCode", service.getServiceCode());
            extraMeta.put("modelName", service.getModelName());
            extraMeta.put("prompt", task.getInputText());
            extraMeta.put("options", options);
            if (StringUtils.hasText(payload.revisedPrompt())) {
                extraMeta.put("revisedPrompt", payload.revisedPrompt());
            }
            fileAsset.setExtraMeta(toJson(extraMeta));
            fileAssetMapper.insert(fileAsset);
            return fileAsset;
        } catch (IOException ex) {
            throw new IllegalStateException("生成图片保存失败", ex);
        }
    }

    private void upsertTaskResult(TaskEntity task, FileAssetEntity fileAsset, String resultPayload, String rawResponse) {
        TaskResultEntity existing = taskResultMapper.selectOne(new LambdaQueryWrapper<TaskResultEntity>()
                .eq(TaskResultEntity::getTaskId, task.getTaskId())
                .last("limit 1"));
        TaskResultEntity result = existing == null ? new TaskResultEntity() : existing;
        if (existing == null) {
            result.setResultId(generateId("result"));
            result.setTaskId(task.getTaskId());
        }
        result.setResultType("image");
        result.setStatus("SUCCESS");
        result.setTextResult("文生图任务已完成");
        result.setFileIds(toJson(List.of(fileAsset.getFileId())));
        result.setStructuredResult(resultPayload);
        result.setRawResponse(StringUtils.hasText(rawResponse) ? rawResponse : "{}");
        result.setErrorDetail("{}");
        if (existing == null) {
            taskResultMapper.insert(result);
        } else {
            taskResultMapper.updateById(result);
        }
    }

    private void markTaskSucceeded(TaskEntity originalTask, String resultPayload) {
        TaskEntity task = findTask(originalTask.getTaskId());
        OffsetDateTime finishedAt = OffsetDateTime.now();
        task.setStatus("SUCCESS");
        task.setResultType("image");
        if (task.getStartedAt() == null) {
            task.setStartedAt(task.getCreatedAt() == null ? finishedAt : task.getCreatedAt());
        }
        task.setFinishedAt(finishedAt);
        task.setDurationMs(Math.max(0L, Duration.between(task.getStartedAt(), finishedAt).toMillis()));
        task.setErrorMessage(null);
        taskMapper.updateById(task);
        ensureResultMessage(task, resultPayload);
    }

    private void markTaskFailed(TaskEntity originalTask, String errorMessage, String errorDetailJson) {
        TaskEntity task = findTask(originalTask.getTaskId());
        OffsetDateTime finishedAt = OffsetDateTime.now();
        task.setStatus("FAILED");
        if (task.getStartedAt() == null) {
            task.setStartedAt(task.getCreatedAt() == null ? finishedAt : task.getCreatedAt());
        }
        task.setFinishedAt(finishedAt);
        task.setDurationMs(Math.max(0L, Duration.between(task.getStartedAt(), finishedAt).toMillis()));
        task.setErrorMessage(errorMessage);
        taskMapper.updateById(task);

        TaskResultEntity existing = taskResultMapper.selectOne(new LambdaQueryWrapper<TaskResultEntity>()
                .eq(TaskResultEntity::getTaskId, task.getTaskId())
                .last("limit 1"));
        if (existing == null) {
            TaskResultEntity result = new TaskResultEntity();
            result.setResultId(generateId("result"));
            result.setTaskId(task.getTaskId());
            result.setResultType("image");
            result.setStatus("FAILED");
            result.setTextResult(errorMessage);
            result.setFileIds("[]");
            result.setStructuredResult("{}");
            result.setRawResponse("{}");
            result.setErrorDetail(StringUtils.hasText(errorDetailJson) ? errorDetailJson : toJson(Map.of("message", errorMessage)));
            taskResultMapper.insert(result);
        } else {
            existing.setStatus("FAILED");
            existing.setTextResult(errorMessage);
            existing.setErrorDetail(StringUtils.hasText(errorDetailJson) ? errorDetailJson : toJson(Map.of("message", errorMessage)));
            taskResultMapper.updateById(existing);
        }

        ChatMessageEntity existingMessage = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getSessionId, task.getSessionId())
                .eq(ChatMessageEntity::getRelatedTaskId, task.getTaskId())
                .eq(ChatMessageEntity::getContentType, "error")
                .last("limit 1"));
        if (existingMessage == null) {
            ChatMessageEntity errorMessageEntity = new ChatMessageEntity();
            errorMessageEntity.setMessageId(generateId("msg"));
            errorMessageEntity.setSessionId(task.getSessionId());
            errorMessageEntity.setMessageType("system");
            errorMessageEntity.setContentType("error");
            errorMessageEntity.setContentText("任务执行失败：" + errorMessage);
            errorMessageEntity.setRelatedTaskId(task.getTaskId());
            errorMessageEntity.setSequenceNo(nextSequenceNo(task.getSessionId()));
            chatMessageMapper.insert(errorMessageEntity);
        }
    }

    private void updateTaskToRunning(TaskEntity originalTask) {
        TaskEntity task = findTask(originalTask.getTaskId());
        if ("RUNNING".equals(task.getStatus()) || "SUCCESS".equals(task.getStatus())) {
            return;
        }
        task.setStatus("RUNNING");
        if (task.getStartedAt() == null) {
            task.setStartedAt(OffsetDateTime.now());
        }
        taskMapper.updateById(task);
    }

    private void simulateTaskLifecycle(TaskEntity task) {
        try {
            Thread.sleep(800L);
            updateTaskToRunning(task);
            Thread.sleep(2200L);
            TaskEntity latestTask = findTask(task.getTaskId());
            if ("SUCCESS".equals(latestTask.getStatus()) || "FAILED".equals(latestTask.getStatus())) {
                return;
            }
            String resultPayload = buildMockResultPayload(latestTask);
            upsertTaskResult(latestTask, persistMockPlaceholderFile(latestTask), resultPayload, "{}");
            markTaskSucceeded(latestTask, resultPayload);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            markTaskFailed(task, "任务执行被中断", toJson(Map.of("type", "interrupted")));
        }
    }

    private FileAssetEntity persistMockPlaceholderFile(TaskEntity task) {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 1024 1024">
                  <defs>
                    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stop-color="#0f766e"/>
                      <stop offset="100%" stop-color="#eda648"/>
                    </linearGradient>
                  </defs>
                  <rect width="1024" height="1024" fill="url(#bg)"/>
                  <text x="512" y="460" text-anchor="middle" font-size="54" font-family="Segoe UI, Microsoft YaHei" fill="white">Mock Result</text>
                  <text x="512" y="560" text-anchor="middle" font-size="30" font-family="Segoe UI, Microsoft YaHei" fill="rgba(255,255,255,0.82)">%s</text>
                </svg>
                """.formatted(escapeXml(task.getInputText()));
        GeneratedImagePayload payload = new GeneratedImagePayload(svg.getBytes(StandardCharsets.UTF_8), "image/svg+xml", null, "{}");
        return persistGeneratedImage(task, resolveService(resolveCapability(task.getFunctionCode(), findConversation(task.getSessionId())), task.getServiceCode()), Collections.emptyMap(), payload);
    }

    private String buildMockResultPayload(TaskEntity task) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "任务结果 / 模拟回流");
        payload.put("summary", "该能力仍使用模拟结果，但已经接入真实任务执行链路。");
        payload.put("chips", List.of("status: success", "mode: mock"));
        payload.put("actionLabel", "基于该结果继续");
        return toJson(payload);
    }

    private String buildResultPayload(
            TaskEntity task,
            ModelServiceEntity service,
            Map<String, Object> options,
            FileAssetEntity fileAsset,
            String revisedPrompt
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "文生图结果 / 已生成");
        payload.put("summary", buildImageSummary(task.getInputText(), revisedPrompt));
        payload.put("chips", buildResultChips(service, options));
        payload.put("actionLabel", "基于该图继续");
        payload.put("previewImageUrl", fileAsset.getPreviewUrl());
        payload.put("downloadUrl", fileAsset.getDownloadUrl());
        payload.put("fileId", fileAsset.getFileId());
        payload.put("kind", "image");
        return toJson(payload);
    }

    private String buildImageSummary(String prompt, String revisedPrompt) {
        if (StringUtils.hasText(revisedPrompt) && !revisedPrompt.equals(prompt)) {
            return "已根据你的描述完成图片生成，并同步记录模型修订后的提示词。";
        }
        return "已根据当前输入完成一张图片生成，可直接预览或下载。";
    }

    private List<String> buildResultChips(ModelServiceEntity service, Map<String, Object> options) {
        List<String> chips = new ArrayList<>();
        chips.add("service: " + service.getServiceCode());
        if (StringUtils.hasText(service.getModelName())) {
            chips.add("model: " + service.getModelName());
        }
        String ratio = readStringOption(options, "ratio");
        if (StringUtils.hasText(ratio)) {
            chips.add("ratio: " + ratio);
        }
        String style = readStringOption(options, "style");
        if (StringUtils.hasText(style)) {
            chips.add("style: " + style);
        }
        return chips;
    }

    private void syncTaskLifecycle(TaskEntity task) {
        if (task.getCreatedAt() == null || "FAILED".equals(task.getStatus()) || "CANCELED".equals(task.getStatus())) {
            return;
        }
        if (CAPABILITY_IMAGE_GENERATION.equals(task.getFunctionCode())) {
            return;
        }
        if ("SUCCESS".equals(task.getStatus())) {
            ensureResultMessage(task, findStructuredResult(task.getTaskId()));
        }
    }

    private void ensureResultMessage(TaskEntity task, String payloadJson) {
        Long existing = chatMessageMapper.selectCount(new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getSessionId, task.getSessionId())
                .eq(ChatMessageEntity::getRelatedTaskId, task.getTaskId())
                .eq(ChatMessageEntity::getContentType, "result"));
        if (existing != null && existing > 0) {
            return;
        }
        ChatMessageEntity resultMessage = new ChatMessageEntity();
        resultMessage.setMessageId(generateId("msg"));
        resultMessage.setSessionId(task.getSessionId());
        resultMessage.setMessageType("assistant");
        resultMessage.setContentType("result");
        resultMessage.setContentText(StringUtils.hasText(payloadJson) ? payloadJson : "{}");
        resultMessage.setRelatedTaskId(task.getTaskId());
        resultMessage.setSequenceNo(nextSequenceNo(task.getSessionId()));
        chatMessageMapper.insert(resultMessage);
    }

    private String findStructuredResult(String taskId) {
        TaskResultEntity taskResult = taskResultMapper.selectOne(new LambdaQueryWrapper<TaskResultEntity>()
                .eq(TaskResultEntity::getTaskId, taskId)
                .last("limit 1"));
        return taskResult == null ? null : taskResult.getStructuredResult();
    }

    private WorkbenchFileResource loadFileResource(String fileId) {
        FileAssetEntity entity = fileAssetMapper.selectOne(new LambdaQueryWrapper<FileAssetEntity>()
                .eq(FileAssetEntity::getFileId, fileId)
                .last("limit 1"));
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "file not found");
        }
        Path absolutePath = resolveStorageRoot().resolve(entity.getStorageKey()).normalize();
        if (!absolutePath.startsWith(resolveStorageRoot()) || !Files.exists(absolutePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "file content not found");
        }
        return new WorkbenchFileResource(
                entity.getFileName(),
                normalizeMimeType(entity.getMimeType()),
                new FileSystemResource(absolutePath)
        );
    }

    private ChatSessionEntity resolveConversation(String conversationId) {
        if (StringUtils.hasText(conversationId)) {
            return findConversation(conversationId);
        }
        return findConversation(createConversation(new ConversationCreateRequest(null)).conversationId());
    }

    private FunctionConfigEntity resolveCapability(String capabilityCode, ChatSessionEntity conversation) {
        String resolvedCode = capabilityCode;
        if (!StringUtils.hasText(resolvedCode) && StringUtils.hasText(conversation.getLastFunctionCode())) {
            resolvedCode = conversation.getLastFunctionCode();
        }
        if (!StringUtils.hasText(resolvedCode)) {
            FunctionConfigEntity defaultCapability = functionConfigMapper.selectOne(new LambdaQueryWrapper<FunctionConfigEntity>()
                    .eq(FunctionConfigEntity::getEnabled, true)
                    .eq(FunctionConfigEntity::getIsDefault, true)
                    .last("limit 1"));
            if (defaultCapability != null) {
                return defaultCapability;
            }
            FunctionConfigEntity firstCapability = functionConfigMapper.selectOne(new LambdaQueryWrapper<FunctionConfigEntity>()
                    .eq(FunctionConfigEntity::getEnabled, true)
                    .orderByAsc(FunctionConfigEntity::getSortOrder)
                    .last("limit 1"));
            if (firstCapability != null) {
                return firstCapability;
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "no enabled capability config found");
        }

        FunctionConfigEntity capability = functionConfigMapper.selectOne(new LambdaQueryWrapper<FunctionConfigEntity>()
                .eq(FunctionConfigEntity::getFunctionCode, resolvedCode)
                .eq(FunctionConfigEntity::getEnabled, true)
                .last("limit 1"));
        if (capability == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "capability config not found");
        }
        return capability;
    }

    private ModelServiceEntity resolveService(FunctionConfigEntity capability, String serviceCode) {
        if (StringUtils.hasText(serviceCode)) {
            ModelServiceEntity service = modelServiceMapper.selectOne(new LambdaQueryWrapper<ModelServiceEntity>()
                    .eq(ModelServiceEntity::getServiceCode, serviceCode)
                    .eq(ModelServiceEntity::getEnabled, true)
                    .last("limit 1"));
            if (service != null) {
                return service;
            }
        }
        if (StringUtils.hasText(capability.getDefaultServiceCode())) {
            ModelServiceEntity service = modelServiceMapper.selectOne(new LambdaQueryWrapper<ModelServiceEntity>()
                    .eq(ModelServiceEntity::getServiceCode, capability.getDefaultServiceCode())
                    .eq(ModelServiceEntity::getEnabled, true)
                    .last("limit 1"));
            if (service != null) {
                return service;
            }
        }
        ModelServiceEntity fallback = modelServiceMapper.selectOne(new LambdaQueryWrapper<ModelServiceEntity>()
                .eq(ModelServiceEntity::getFunctionCode, capability.getFunctionCode())
                .eq(ModelServiceEntity::getEnabled, true)
                .orderByAsc(ModelServiceEntity::getCreatedAt)
                .last("limit 1"));
        if (fallback == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "no enabled model service found for capability");
        }
        return fallback;
    }

    private ChatSessionEntity findConversation(String conversationId) {
        ChatSessionEntity entity = chatSessionMapper.selectOne(new LambdaQueryWrapper<ChatSessionEntity>()
                .eq(ChatSessionEntity::getSessionId, conversationId)
                .last("limit 1"));
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "conversation not found");
        }
        return entity;
    }

    private TaskEntity findTask(String taskId) {
        TaskEntity entity = taskMapper.selectOne(new LambdaQueryWrapper<TaskEntity>()
                .eq(TaskEntity::getTaskId, taskId)
                .last("limit 1"));
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found");
        }
        return entity;
    }

    private int nextSequenceNo(String conversationId) {
        ChatMessageEntity latest = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessageEntity>()
                .eq(ChatMessageEntity::getSessionId, conversationId)
                .orderByDesc(ChatMessageEntity::getSequenceNo)
                .last("limit 1"));
        return latest == null || latest.getSequenceNo() == null ? 1 : latest.getSequenceNo() + 1;
    }

    private Path resolveStorageRoot() {
        return Paths.get(storageRoot).toAbsolutePath().normalize();
    }

    private boolean useDemoImageProvider(ModelServiceEntity service) {
        return !StringUtils.hasText(service.getEndpoint()) || service.getEndpoint().contains("mock-api");
    }

    private int resolveTimeoutSeconds(ModelServiceEntity service) {
        if (service.getTimeoutMs() == null || service.getTimeoutMs() <= 0) {
            return 60;
        }
        return Math.max(10, (int) Math.ceil(service.getTimeoutMs() / 1000.0));
    }

    private String resolveBearerToken(ModelServiceEntity service) {
        String normalizedServiceCode = service.getServiceCode() == null
                ? ""
                : service.getServiceCode().replaceAll("[^A-Za-z0-9]", "_").toUpperCase();
        List<String> candidateKeys = List.of(
                "MODEL_SERVICE_API_KEY__" + normalizedServiceCode,
                "MODEL_SERVICE_API_KEY",
                "OPENAI_API_KEY"
        );
        for (String key : candidateKeys) {
            String value = System.getenv(key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        throw new IllegalStateException("未找到 Bearer Token，请配置环境变量 MODEL_SERVICE_API_KEY__" + normalizedServiceCode);
    }

    private Map<String, Object> parseRequestParams(String requestParams) {
        if (!StringUtils.hasText(requestParams)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(requestParams, MAP_TYPE_REFERENCE);
        } catch (JsonProcessingException ex) {
            return Collections.emptyMap();
        }
    }

    private JsonNode parseJsonNode(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("图像生成服务返回了无法解析的 JSON", ex);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON 序列化失败", ex);
        }
    }

    private String extractImageUrl(JsonNode root) {
        List<JsonNode> candidates = List.of(
                root.path("data"),
                root.path("images"),
                root.path("output")
        );
        for (JsonNode candidate : candidates) {
            if (candidate.isArray() && candidate.size() > 0) {
                JsonNode first = candidate.get(0);
                if (first.hasNonNull("url")) {
                    return first.get("url").asText();
                }
                if (first.hasNonNull("image_url")) {
                    return first.get("image_url").asText();
                }
            }
        }
        if (root.hasNonNull("url")) {
            return root.get("url").asText();
        }
        return null;
    }

    private byte[] extractImageBytes(JsonNode root) {
        List<JsonNode> candidates = List.of(
                root.path("data"),
                root.path("images"),
                root.path("output")
        );
        for (JsonNode candidate : candidates) {
            if (candidate.isArray() && candidate.size() > 0) {
                JsonNode first = candidate.get(0);
                if (first.hasNonNull("b64_json")) {
                    return Base64.getDecoder().decode(first.get("b64_json").asText());
                }
                if (first.hasNonNull("b64")) {
                    return Base64.getDecoder().decode(first.get("b64").asText());
                }
                if (first.hasNonNull("base64")) {
                    return Base64.getDecoder().decode(first.get("base64").asText());
                }
            }
        }
        return null;
    }

    private String extractImageMimeType(JsonNode root) {
        if (root.hasNonNull("mime_type")) {
            return root.get("mime_type").asText();
        }
        List<JsonNode> candidates = List.of(root.path("data"), root.path("images"), root.path("output"));
        for (JsonNode candidate : candidates) {
            if (candidate.isArray() && candidate.size() > 0) {
                JsonNode first = candidate.get(0);
                if (first.hasNonNull("mime_type")) {
                    return first.get("mime_type").asText();
                }
            }
        }
        return DEFAULT_IMAGE_MIME_TYPE;
    }

    private String extractText(JsonNode root, String fieldName) {
        if (root.hasNonNull(fieldName)) {
            return root.get(fieldName).asText();
        }
        JsonNode dataNode = root.path("data");
        if (dataNode.isArray() && dataNode.size() > 0 && dataNode.get(0).hasNonNull(fieldName)) {
            return dataNode.get(0).get(fieldName).asText();
        }
        return null;
    }

    private DownloadedImage downloadBinary(URI uri, Duration timeout) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Accept", "image/*")
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("图像下载失败，HTTP " + response.statusCode());
            }
            String mimeType = response.headers().firstValue("Content-Type").orElse(DEFAULT_IMAGE_MIME_TYPE);
            return new DownloadedImage(response.body(), normalizeMimeType(mimeType));
        } catch (IOException ex) {
            throw new IllegalStateException("图像下载失败", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("图像下载被中断", ex);
        }
    }

    private HttpResponse<String> sendTextRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("图像生成服务调用失败，HTTP " + response.statusCode() + "，响应：" + response.body());
            }
            return response;
        } catch (IOException ex) {
            throw new IllegalStateException("图像生成服务调用失败", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("图像生成服务调用被中断", ex);
        }
    }

    private String readStringOption(Map<String, Object> options, String key) {
        Object value = options.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private ImageSize resolveImageSize(String ratio) {
        if (!StringUtils.hasText(ratio)) {
            return new ImageSize(1024, 1024);
        }
        return switch (ratio) {
            case "16:9" -> new ImageSize(1536, 1024);
            case "9:16" -> new ImageSize(1024, 1536);
            case "4:3" -> new ImageSize(1365, 1024);
            case "3:4" -> new ImageSize(1024, 1365);
            default -> new ImageSize(1024, 1024);
        };
    }

    private String mimeTypeToExtension(String mimeType) {
        return switch (normalizeMimeType(mimeType)) {
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            case "image/svg+xml" -> "svg";
            default -> "png";
        };
    }

    private String normalizeMimeType(String mimeType) {
        return StringUtils.hasText(mimeType) ? mimeType.split(";")[0].trim() : DEFAULT_IMAGE_MIME_TYPE;
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String escapeXml(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String generateId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private WorkbenchFunctionVO toWorkbenchFunctionVO(FunctionConfigEntity entity, ModelServiceEntity defaultService) {
        return WorkbenchFunctionVO.builder()
                .id(entity.getId())
                .functionCode(entity.getFunctionCode())
                .functionName(entity.getFunctionName())
                .icon(entity.getIcon())
                .sortOrder(entity.getSortOrder())
                .allowManualModelSelect(entity.getAllowManualModelSelect())
                .showInMainBar(entity.getShowInMainBar())
                .showInMoreMenu(entity.getShowInMoreMenu())
                .defaultServiceCode(entity.getDefaultServiceCode())
                .defaultServiceName(defaultService == null ? null : defaultService.getServiceName())
                .description(entity.getDescription())
                .build();
    }

    private ConversationVO toConversationVO(ChatSessionEntity entity) {
        return ConversationVO.builder()
                .id(entity.getId())
                .conversationId(entity.getSessionId())
                .title(entity.getTitle())
                .lastCapabilityCode(entity.getLastFunctionCode())
                .lastServiceCode(entity.getLastServiceCode())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private ChatMessageVO toChatMessageVO(ChatMessageEntity entity) {
        return ChatMessageVO.builder()
                .id(entity.getId())
                .messageId(entity.getMessageId())
                .conversationId(entity.getSessionId())
                .messageType(entity.getMessageType())
                .contentType(entity.getContentType())
                .contentText(entity.getContentText())
                .relatedTaskId(entity.getRelatedTaskId())
                .relatedRecordId(entity.getRelatedRecordId())
                .sequenceNo(entity.getSequenceNo())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private TaskVO toTaskVO(TaskEntity entity) {
        return TaskVO.builder()
                .id(entity.getId())
                .taskId(entity.getTaskId())
                .conversationId(entity.getSessionId())
                .messageId(entity.getMessageId())
                .parentTaskId(entity.getParentTaskId())
                .capabilityCode(entity.getFunctionCode())
                .selectionMode(entity.getSelectionMode())
                .resolvedIntent(entity.getResolvedIntent())
                .serviceCode(entity.getServiceCode())
                .modelName(entity.getModelName())
                .inputText(entity.getInputText())
                .requestParams(entity.getRequestParams())
                .inputAssetIds(entity.getInputAssetIds())
                .inheritanceMode(entity.getInheritanceMode())
                .sourceAssetIds(entity.getSourceAssetIds())
                .status(entity.getStatus())
                .resultType(entity.getResultType())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .startedAt(entity.getStartedAt())
                .finishedAt(entity.getFinishedAt())
                .durationMs(entity.getDurationMs())
                .build();
    }

    private record GeneratedImagePayload(
            byte[] bytes,
            String mimeType,
            String revisedPrompt,
            String rawResponse
    ) {
    }

    private record DownloadedImage(
            byte[] bytes,
            String mimeType
    ) {
    }

    private record ImageSize(
            int width,
            int height
    ) {
    }
}
