package com.shupai.multimodal.appentry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shupai.multimodal.appentry.mapper.ChatMessageMapper;
import com.shupai.multimodal.appentry.mapper.ChatSessionMapper;
import com.shupai.multimodal.appentry.mapper.CallRecordMapper;
import com.shupai.multimodal.appentry.mapper.FileAssetMapper;
import com.shupai.multimodal.appentry.mapper.FunctionConfigMapper;
import com.shupai.multimodal.appentry.mapper.FunctionModelBindingMapper;
import com.shupai.multimodal.appentry.mapper.ModelServiceMapper;
import com.shupai.multimodal.appentry.mapper.ParamTemplateMapper;
import com.shupai.multimodal.appentry.mapper.TaskMapper;
import com.shupai.multimodal.appentry.mapper.TaskResultMapper;
import com.shupai.multimodal.appentry.model.dto.ConversationCreateRequest;
import com.shupai.multimodal.appentry.model.dto.ConversationUpdateRequest;
import com.shupai.multimodal.appentry.model.dto.WorkbenchChatRequest;
import com.shupai.multimodal.appentry.model.entity.ChatMessageEntity;
import com.shupai.multimodal.appentry.model.entity.ChatSessionEntity;
import com.shupai.multimodal.appentry.model.entity.CallRecordEntity;
import com.shupai.multimodal.appentry.model.entity.FileAssetEntity;
import com.shupai.multimodal.appentry.model.entity.FunctionConfigEntity;
import com.shupai.multimodal.appentry.model.entity.FunctionModelBindingEntity;
import com.shupai.multimodal.appentry.model.entity.ModelServiceEntity;
import com.shupai.multimodal.appentry.model.entity.ParamTemplateEntity;
import com.shupai.multimodal.appentry.model.entity.TaskEntity;
import com.shupai.multimodal.appentry.model.entity.TaskResultEntity;
import com.shupai.multimodal.appentry.model.vo.ChatMessageVO;
import com.shupai.multimodal.appentry.model.vo.ConversationVO;
import com.shupai.multimodal.appentry.model.vo.TaskVO;
import com.shupai.multimodal.appentry.model.vo.WorkbenchChatSubmitVO;
import com.shupai.multimodal.appentry.model.vo.WorkbenchComposerFieldVO;
import com.shupai.multimodal.appentry.model.vo.WorkbenchComposerOptionVO;
import com.shupai.multimodal.appentry.model.vo.WorkbenchComposerSchemaVO;
import com.shupai.multimodal.appentry.model.vo.WorkbenchConversationSummaryVO;
import com.shupai.multimodal.appentry.model.vo.WorkbenchFileResource;
import com.shupai.multimodal.appentry.model.vo.WorkbenchFunctionVO;
import com.shupai.multimodal.appentry.model.vo.WorkbenchUploadedFileVO;
import com.shupai.multimodal.appentry.service.WorkbenchService;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
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
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkbenchServiceImpl implements WorkbenchService {

    private static final String DEFAULT_CONVERSATION_TITLE = "\u65b0\u5efa\u5de5\u4f5c\u53f0\u4f1a\u8bdd";
    private static final String CAPABILITY_IMAGE_GENERATION = "image_generation";
    private static final String CAPABILITY_IMAGE_RECOGNITION = "image_recognition";
    private static final String CAPABILITY_TEXT_TO_SPEECH = "text_to_speech";
    private static final String CAPABILITY_TEXT_TO_PPT = "text_to_ppt";
    private static final String CAPABILITY_TEXT_TO_VIDEO = "text_to_video";
    private static final String DEFAULT_IMAGE_MIME_TYPE = "image/png";
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };
    private static final Map<String, String> CAPABILITY_PLACEHOLDER_MAP = Map.of(
            CAPABILITY_IMAGE_GENERATION, "\u63cf\u8ff0\u4e00\u5f20\u4f60\u60f3\u751f\u6210\u7684\u56fe\u7247\u3002",
            CAPABILITY_IMAGE_RECOGNITION, "\u63cf\u8ff0\u4f60\u60f3\u5206\u6790\u7684\u56fe\u7247\u5185\u5bb9\u3002",
            CAPABILITY_TEXT_TO_SPEECH, "\u8f93\u5165\u8981\u5408\u6210\u8bed\u97f3\u7684\u6587\u672c\u3002",
            CAPABILITY_TEXT_TO_PPT, "\u8f93\u5165 PPT \u4e3b\u9898\u548c\u63d0\u7eb2\u3002",
            CAPABILITY_TEXT_TO_VIDEO, "\u63cf\u8ff0\u4f60\u60f3\u751f\u6210\u7684\u77ed\u89c6\u9891\u6216\u955c\u5934\u3002"
    );
    private static final Map<String, String> CAPABILITY_HELPER_MAP = Map.of(
            CAPABILITY_IMAGE_GENERATION, "\u53c2\u6570\u7531\u540e\u7aef\u914d\u7f6e\u4e0b\u53d1\uff0c\u53ef\u7ee7\u7eed\u6269\u5c55\u6a21\u578b\u670d\u52a1\u3002",
            CAPABILITY_IMAGE_RECOGNITION, "\u4e0a\u4f20\u6216\u5f15\u7528\u56fe\u7247\u540e\u8fd0\u884c\u89c6\u89c9\u5206\u6790\u3002",
            CAPABILITY_TEXT_TO_SPEECH, "\u7ed3\u679c\u4f1a\u4fdd\u5b58\u4e3a\u53ef\u4e0b\u8f7d\u7684\u97f3\u9891\u6587\u4ef6\u3002",
            CAPABILITY_TEXT_TO_PPT, "\u7ed3\u679c\u4f1a\u4fdd\u5b58\u4e3a\u53ef\u4e0b\u8f7d\u7684 PPTX \u6587\u4ef6\u3002",
            CAPABILITY_TEXT_TO_VIDEO, "\u5f53\u6a21\u578b\u670d\u52a1\u8fd4\u56de\u89c6\u9891\u540e\uff0c\u7ed3\u679c\u4f1a\u4fdd\u5b58\u4e3a\u53ef\u4e0b\u8f7d\u7684\u89c6\u9891\u6587\u4ef6\u3002"
    );
    private static final Map<String, String> FIELD_LABEL_MAP = Map.of(
            "model", "\u6a21\u578b\u670d\u52a1",
            "ratio", "\u753b\u5e45\u6bd4\u4f8b",
            "style", "\u98ce\u683c",
            "template", "\u6a21\u677f",
            "recognitionMode", "\u8bc6\u522b\u6a21\u5f0f",
            "voice", "\u97f3\u8272",
            "format", "\u683c\u5f0f",
            "pages", "\u9875\u6570",
            "duration", "\u65f6\u957f"
    );
    private static final Map<String, String> FIELD_PLACEHOLDER_MAP = Map.of(
            "model", "\u9009\u62e9\u6a21\u578b\u670d\u52a1",
            "ratio", "\u9009\u62e9\u6bd4\u4f8b",
            "style", "\u9009\u62e9\u98ce\u683c",
            "template", "\u9009\u62e9\u6a21\u677f",
            "recognitionMode", "\u9009\u62e9\u8bc6\u522b\u6a21\u5f0f",
            "voice", "\u9009\u62e9\u97f3\u8272",
            "format", "\u9009\u62e9\u683c\u5f0f",
            "pages", "\u9009\u62e9\u9875\u6570",
            "duration", "\u9009\u62e9\u65f6\u957f"
    );
    private static final Map<String, List<String>> CAPABILITY_FIELD_ORDER_MAP = Map.of(
            CAPABILITY_IMAGE_GENERATION, List.of("ratio", "style", "template"),
            CAPABILITY_IMAGE_RECOGNITION, List.of("recognitionMode"),
            CAPABILITY_TEXT_TO_SPEECH, List.of("voice", "format"),
            CAPABILITY_TEXT_TO_PPT, List.of("template", "pages"),
            CAPABILITY_TEXT_TO_VIDEO, List.of("duration", "ratio", "style")
    );
    private static final Map<String, Map<String, List<String>>> CAPABILITY_FALLBACK_OPTIONS_MAP = Map.of(
            CAPABILITY_IMAGE_GENERATION, Map.of(
                    "ratio", List.of("1:1", "4:3", "16:9"),
                    "style", List.of("realistic", "illustration", "anime"),
                    "template", List.of("poster", "product", "landscape")
            ),
            CAPABILITY_IMAGE_RECOGNITION, Map.of(
                    "recognitionMode", List.of("general", "ocr", "detail")
            ),
            CAPABILITY_TEXT_TO_SPEECH, Map.of(
                    "voice", List.of("alloy", "nova", "shimmer"),
                    "format", List.of("mp3", "pcm")
            ),
            CAPABILITY_TEXT_TO_PPT, Map.of(
                    "template", List.of("business", "tech", "minimal"),
                    "pages", List.of("8", "10", "12")
            ),
            CAPABILITY_TEXT_TO_VIDEO, Map.of(
                    "duration", List.of("4s", "6s", "8s"),
                    "ratio", List.of("16:9", "9:16"),
                    "style", List.of("cinematic", "anime", "documentary")
            )
    );

    private final FunctionConfigMapper functionConfigMapper;
    private final FunctionModelBindingMapper functionModelBindingMapper;
    private final ModelServiceMapper modelServiceMapper;
    private final ParamTemplateMapper paramTemplateMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final CallRecordMapper callRecordMapper;
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
    public List<WorkbenchConversationSummaryVO> listConversations(String scope, String keyword) {
        LambdaQueryWrapper<ChatSessionEntity> conversationWrapper = new LambdaQueryWrapper<>();
        switch (normalizeConversationScope(scope)) {
            case "archived" -> conversationWrapper.eq(ChatSessionEntity::getStatus, "ARCHIVED");
            case "all" -> {
                // keep all records
            }
            default -> conversationWrapper.ne(ChatSessionEntity::getStatus, "ARCHIVED");
        }
        conversationWrapper.orderByDesc(ChatSessionEntity::getUpdatedAt)
                .orderByDesc(ChatSessionEntity::getCreatedAt);

        List<ChatSessionEntity> conversations = chatSessionMapper.selectList(conversationWrapper);
        if (conversations.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> conversationIds = conversations.stream()
                .map(ChatSessionEntity::getSessionId)
                .toList();
        List<ChatMessageEntity> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessageEntity>()
                .in(ChatMessageEntity::getSessionId, conversationIds)
                .orderByDesc(ChatMessageEntity::getSequenceNo)
                .orderByDesc(ChatMessageEntity::getCreatedAt));

        Map<String, ChatMessageEntity> latestMessageMap = new LinkedHashMap<>();
        Map<String, Integer> messageCountMap = new LinkedHashMap<>();
        for (ChatMessageEntity message : messages) {
            latestMessageMap.putIfAbsent(message.getSessionId(), message);
            messageCountMap.merge(message.getSessionId(), 1, Integer::sum);
        }

        List<WorkbenchConversationSummaryVO> summaries = conversations.stream()
                .map(conversation -> toConversationSummaryVO(
                        conversation,
                        latestMessageMap.get(conversation.getSessionId()),
                        messageCountMap.getOrDefault(conversation.getSessionId(), 0)))
                .toList();
        if (!StringUtils.hasText(keyword)) {
            return summaries;
        }
        String normalizedKeyword = keyword.trim().toLowerCase();
        return summaries.stream()
                .filter(summary -> matchesConversationKeyword(summary, normalizedKeyword))
                .toList();
    }

    @Override
    public WorkbenchComposerSchemaVO getComposerSchema(String functionCode, String serviceCode) {
        FunctionConfigEntity capability = resolveCapability(functionCode, null);
        List<ModelServiceEntity> enabledServices = listEnabledServicesForCapability(capability.getFunctionCode(), serviceCode);

        ModelServiceEntity preferredService = resolvePreferredService(capability, enabledServices, serviceCode);
        Map<String, List<String>> supportedOptions = preferredService == null
                ? Collections.emptyMap()
                : parseSupportedOptions(preferredService.getSupportedOptions());
        List<ParamTemplateEntity> templates = paramTemplateMapper.selectList(new LambdaQueryWrapper<ParamTemplateEntity>()
                        .eq(ParamTemplateEntity::getFunctionCode, capability.getFunctionCode())
                        .eq(ParamTemplateEntity::getEnabled, true)
                        .orderByDesc(ParamTemplateEntity::getIsDefault)
                        .orderByAsc(ParamTemplateEntity::getSortOrder)
                        .orderByAsc(ParamTemplateEntity::getCreatedAt))
                .stream()
                .filter(template -> preferredService == null
                        || !StringUtils.hasText(template.getServiceCode())
                        || preferredService.getServiceCode().equals(template.getServiceCode()))
                .toList();

        List<WorkbenchComposerFieldVO> fields = new ArrayList<>();
        if (Boolean.TRUE.equals(capability.getAllowManualModelSelect()) && !enabledServices.isEmpty()) {
            fields.add(WorkbenchComposerFieldVO.builder()
                    .key("model")
                    .label(FIELD_LABEL_MAP.get("model"))
                    .placeholder(FIELD_PLACEHOLDER_MAP.get("model"))
                    .options(enabledServices.stream()
                            .map(service -> WorkbenchComposerOptionVO.builder()
                                    .label(service.getServiceName() + " / " + service.getServiceCode())
                                    .value(service.getServiceCode())
                                    .build())
                            .toList())
                    .build());
        }

        for (String fieldKey : CAPABILITY_FIELD_ORDER_MAP.getOrDefault(capability.getFunctionCode(), Collections.emptyList())) {
            List<WorkbenchComposerOptionVO> options = resolveComposerFieldOptions(
                    capability.getFunctionCode(),
                    fieldKey,
                    supportedOptions,
                    templates
            );
            if (options.isEmpty()) {
                continue;
            }
            fields.add(WorkbenchComposerFieldVO.builder()
                    .key(fieldKey)
                    .label(FIELD_LABEL_MAP.getOrDefault(fieldKey, fieldKey))
                    .placeholder(FIELD_PLACEHOLDER_MAP.getOrDefault(fieldKey, "璇烽€夋嫨"))
                    .options(options)
                    .build());
        }

        return WorkbenchComposerSchemaVO.builder()
                .capabilityCode(capability.getFunctionCode())
                .placeholder(CAPABILITY_PLACEHOLDER_MAP.getOrDefault(capability.getFunctionCode(), "\u8bf7\u8f93\u5165\u4efb\u52a1\u63cf\u8ff0"))
                .helper(CAPABILITY_HELPER_MAP.getOrDefault(capability.getFunctionCode(), "\u5f53\u524d\u80fd\u529b\u53c2\u6570\u7531\u540e\u7aef\u914d\u7f6e\u4e0b\u53d1\u3002"))
                .supportsUpload(supportsUpload(capability.getFunctionCode()))
                .fields(fields)
                .build();
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
    @Transactional
    public ConversationVO updateConversation(String conversationId, ConversationUpdateRequest request) {
        ChatSessionEntity conversation = findConversation(conversationId);
        conversation.setTitle(request.title().trim());
        chatSessionMapper.updateById(conversation);
        return toConversationVO(conversation);
    }

    @Override
    @Transactional
    public void archiveConversation(String conversationId) {
        ChatSessionEntity conversation = findConversation(conversationId);
        if ("ARCHIVED".equals(conversation.getStatus())) {
            return;
        }
        conversation.setStatus("ARCHIVED");
        chatSessionMapper.updateById(conversation);
    }

    @Override
    @Transactional
    public void restoreConversation(String conversationId) {
        ChatSessionEntity conversation = findConversation(conversationId);
        if (!"ARCHIVED".equals(conversation.getStatus())) {
            return;
        }
        conversation.setStatus("ACTIVE");
        chatSessionMapper.updateById(conversation);
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
        ModelServiceEntity service = resolveService(
                capability,
                StringUtils.hasText(request.serviceCode()) ? request.serviceCode() : request.model()
        );

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
        task.setRequestParams(resolveRequestParams(request));
        task.setInputAssetIds(resolveInputAssetIds(request));
        task.setInheritanceMode(StringUtils.hasText(request.inheritanceMode()) ? request.inheritanceMode() : "none");
        task.setSourceAssetIds(StringUtils.hasText(request.sourceAssetIds()) ? request.sourceAssetIds() : "[]");
        task.setStatus("PENDING");
        taskMapper.insert(task);
        String recordId = createCallRecord(task);

        ChatMessageEntity taskMessage = new ChatMessageEntity();
        taskMessage.setMessageId(generateId("msg"));
        taskMessage.setSessionId(conversation.getSessionId());
        taskMessage.setMessageType("system");
        taskMessage.setContentType("status");
        taskMessage.setContentText("Task created and queued for execution.");
        taskMessage.setRelatedTaskId(task.getTaskId());
        taskMessage.setRelatedRecordId(recordId);
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
    @Transactional
    public WorkbenchUploadedFileVO uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is empty");
        }
        String mimeType = normalizeMimeType(file.getContentType());
        if (!mimeType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "only image uploads are supported for now");
        }
        try {
            String fileId = generateId("file");
            String extension = mediaTypeToExtension(mimeType, "png");
            String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : fileId + "." + extension;
            Path relativePath = Paths.get("uploads", LocalDate.now().toString(), fileId + "." + extension);
            Path absolutePath = resolveStorageRoot().resolve(relativePath);
            Files.createDirectories(absolutePath.getParent());
            Files.write(absolutePath, file.getBytes());

            FileAssetEntity fileAsset = new FileAssetEntity();
            fileAsset.setFileId(fileId);
            fileAsset.setRelatedType("workbench");
            fileAsset.setRelatedId("upload");
            fileAsset.setFileRole("input");
            fileAsset.setFileType("image");
            fileAsset.setFileName(originalName);
            fileAsset.setStorageKey(relativePath.toString().replace('\\', '/'));
            fileAsset.setFileSize(file.getSize());
            fileAsset.setMimeType(mimeType);
            fileAsset.setPreviewUrl("/api/workbench/files/" + fileId + "/preview");
            fileAsset.setDownloadUrl("/api/workbench/files/" + fileId + "/download");
            fileAsset.setExtraMeta(toJson(Map.of("uploadType", "workbench_reference")));
            fileAssetMapper.insert(fileAsset);
            return toUploadedFileVO(fileAsset);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store uploaded file", ex);
        }
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
            ModelServiceEntity service = findModelService(task.getServiceCode());
            if (CAPABILITY_TEXT_TO_SPEECH.equals(task.getFunctionCode()) && isSpeechSynthesisService(service)) {
                executeSpeechSynthesisTask(task, service);
                return;
            }
            if (CAPABILITY_TEXT_TO_PPT.equals(task.getFunctionCode()) && isOpenAiCompatible(service)) {
                executePptGenerationTask(task, service);
                return;
            }
            if (CAPABILITY_TEXT_TO_VIDEO.equals(task.getFunctionCode()) && isVideoGenerationService(service)) {
                executeVideoGenerationTask(task, service);
                return;
            }
            if (isOpenAiCompatible(service)) {
                executeOpenAiCompatibleTask(task, service);
                return;
            }
            simulateTaskLifecycle(task);
        } catch (Exception ex) {
            log.warn("Workbench task execution failed, taskId={}, capability={}, service={}",
                    taskId, task.getFunctionCode(), task.getServiceCode(), ex);
            markTaskFailed(task, resolveErrorMessage(ex), null);
        }
    }

    private void executeImageGenerationTask(TaskEntity task) {
        task = findTask(task.getTaskId());
        updateTaskToRunning(task);

        ModelServiceEntity service = findModelService(task.getServiceCode());

        Map<String, Object> options = parseRequestParams(task.getRequestParams());
        GeneratedImagePayload payload = generateImagePayload(task, service, options);
        FileAssetEntity fileAsset = persistGeneratedImage(task, service, options, payload);
        String resultPayload = buildResultPayload(task, service, options, fileAsset, payload.revisedPrompt());
        upsertTaskResult(task, fileAsset, resultPayload, payload.rawResponse(), "image");
        markTaskSucceeded(task, resultPayload, "image");
    }

    private void executeOpenAiCompatibleTask(TaskEntity task, ModelServiceEntity service) {
        task = findTask(task.getTaskId());
        updateTaskToRunning(task);

        Map<String, Object> options = parseRequestParams(task.getRequestParams());
        OpenAiCompatiblePayload payload = invokeOpenAiCompatible(task, service, options);
        String resultPayload = buildTextResultPayload(task, service, options, payload.content());
        String resultType = resolveConfiguredResultType(task.getFunctionCode(), service);
        upsertTaskResult(task, null, resultPayload, payload.rawResponse(), resultType);
        markTaskSucceeded(task, resultPayload, resultType);
    }

    private void executeSpeechSynthesisTask(TaskEntity task, ModelServiceEntity service) {
        task = findTask(task.getTaskId());
        updateTaskToRunning(task);

        Map<String, Object> options = parseRequestParams(task.getRequestParams());
        GeneratedAudioPayload payload = generateSpeechPayload(task, service, options);
        FileAssetEntity fileAsset = persistGeneratedAudio(task, service, options, payload);
        String resultPayload = buildAudioResultPayload(task, service, options, fileAsset);
        upsertTaskResult(task, fileAsset, resultPayload, payload.rawResponse(), "audio");
        markTaskSucceeded(task, resultPayload, "audio");
    }

    private void executePptGenerationTask(TaskEntity task, ModelServiceEntity service) {
        task = findTask(task.getTaskId());
        updateTaskToRunning(task);

        Map<String, Object> options = parseRequestParams(task.getRequestParams());
        OpenAiCompatiblePayload outlinePayload = invokeOpenAiCompatible(task, service, options);
        GeneratedFilePayload pptPayload = generatePptxPayload(task, service, options, outlinePayload.content());
        FileAssetEntity fileAsset = persistGeneratedFile(
                task,
                service,
                options,
                pptPayload,
                "ppt-generation",
                "file",
                "pptx"
        );
        String resultPayload = buildAssetResultPayload(
                task,
                service,
                options,
                fileAsset,
                "PPT Generation Result",
                "PPTX file generated from a structured outline with themed cover, agenda, section pages and closing slide.",
                "file"
        );
        upsertTaskResult(task, fileAsset, resultPayload, outlinePayload.rawResponse(), "file");
        markTaskSucceeded(task, resultPayload, "file");
    }

    private void executeVideoGenerationTask(TaskEntity task, ModelServiceEntity service) {
        task = findTask(task.getTaskId());
        updateTaskToRunning(task);

        Map<String, Object> options = parseRequestParams(task.getRequestParams());
        GeneratedVideoPayload videoPayload = generateVideoPayload(task, service, options);
        FileAssetEntity fileAsset = persistGeneratedFile(
                task,
                service,
                options,
                new GeneratedFilePayload(videoPayload.bytes(), videoPayload.mimeType(), videoPayload.rawResponse()),
                "video-generation",
                "video",
                "mp4"
        );
        String resultPayload = buildAssetResultPayload(
                task,
                service,
                options,
                fileAsset,
                "Video Generation Result",
                "Video generated from your current prompt. You can play it in the conversation or download the MP4 file.",
                "video"
        );
        upsertTaskResult(task, fileAsset, resultPayload, videoPayload.rawResponse(), "video");
        markTaskSucceeded(task, resultPayload, "video");
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
        if (isOpenRouterImageGenerationService(service)) {
            return generateViaOpenRouterImageChat(task, service, options);
        }

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
                throw new IllegalStateException("Image generation service returned no recognizable image payload");
            }
            DownloadedImage downloadedImage = downloadBinary(URI.create(imageUrl), Duration.ofSeconds(resolveTimeoutSeconds(service)));
            imageBytes = downloadedImage.bytes();
            mimeType = downloadedImage.mimeType();
        }

        return new GeneratedImagePayload(imageBytes, normalizeMimeType(mimeType), revisedPrompt, response.body());
    }

    private GeneratedImagePayload generateViaOpenRouterImageChat(TaskEntity task, ModelServiceEntity service, Map<String, Object> options) {
        URI endpoint = resolveOpenRouterChatEndpoint(service);
        Map<String, Object> requestBody = buildOpenRouterImageRequestBody(task, service, options);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(resolveTimeoutSeconds(service)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(requestBody)));

        applyTemplateHeaders(requestBuilder, service, task, options);
        if ("bearer".equalsIgnoreCase(service.getAuthType()) || !StringUtils.hasText(service.getAuthType())) {
            requestBuilder.header("Authorization", "Bearer " + resolveBearerToken(service));
        }

        HttpResponse<String> response = sendTextRequest(requestBuilder.build());
        JsonNode root = parseJsonNode(response.body());
        byte[] imageBytes = extractImageBytes(root);
        String mimeType = extractImageMimeType(root);
        if (imageBytes == null) {
            String imageUrl = extractImageUrl(root);
            if (!StringUtils.hasText(imageUrl)) {
                throw new IllegalStateException("OpenRouter image service returned no recognizable image payload");
            }
            if (imageUrl.startsWith("data:")) {
                imageBytes = decodeBase64Image(imageUrl);
                mimeType = extractDataUrlMimeType(imageUrl, DEFAULT_IMAGE_MIME_TYPE);
            } else {
                DownloadedImage downloadedImage = downloadBinary(URI.create(imageUrl), Duration.ofSeconds(resolveTimeoutSeconds(service)));
                imageBytes = downloadedImage.bytes();
                mimeType = downloadedImage.mimeType();
            }
        }

        return new GeneratedImagePayload(imageBytes, normalizeMimeType(mimeType), null, toJson(Map.of(
                "provider", "openrouter",
                "endpoint", endpoint.toString(),
                "model", defaultString(service.getModelCode(), service.getModelName()),
                "mimeType", normalizeMimeType(mimeType)
        )));
    }

    private Map<String, Object> buildOpenRouterImageRequestBody(TaskEntity task, ModelServiceEntity service, Map<String, Object> options) {
        if (StringUtils.hasText(service.getPayloadTemplate()) && !"{}".equals(service.getPayloadTemplate().trim())) {
            return parseRequestParams(renderTemplate(service.getPayloadTemplate(), task, service, options));
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", StringUtils.hasText(service.getModelCode()) ? service.getModelCode() : service.getModelName());
        requestBody.put("messages", List.of(Map.of("role", "user", "content", buildImagePrompt(task, options))));
        requestBody.put("modalities", List.of("image", "text"));
        return requestBody;
    }

    private String buildImagePrompt(TaskEntity task, Map<String, Object> options) {
        List<String> parts = new ArrayList<>();
        parts.add("Generate an image from this prompt: " + task.getInputText());
        String ratio = readStringOption(options, "ratio");
        if (StringUtils.hasText(ratio)) {
            parts.add("Aspect ratio: " + ratio);
        }
        String style = readStringOption(options, "style");
        if (StringUtils.hasText(style)) {
            parts.add("Style: " + style);
        }
        String template = readStringOption(options, "template");
        if (StringUtils.hasText(template)) {
            parts.add("Template/use case: " + template);
        }
        return String.join("\n", parts);
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

    private GeneratedAudioPayload generateSpeechPayload(TaskEntity task, ModelServiceEntity service, Map<String, Object> options) {
        if ("openrouter_tts".equalsIgnoreCase(defaultString(service.getProviderType(), ""))) {
            return generateOpenRouterSpeechPayload(task, service, options);
        }

        URI endpoint = resolveSpeechEndpoint(service);
        Map<String, Object> requestBody = buildSpeechRequestBody(task, service, options);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(resolveTimeoutSeconds(service)))
                .header("Content-Type", "application/json")
                .header("Accept", "audio/*,application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(requestBody)));

        applyTemplateHeaders(requestBuilder, service, task, options);
        if ("bearer".equalsIgnoreCase(service.getAuthType()) || !StringUtils.hasText(service.getAuthType())) {
            requestBuilder.header("Authorization", "Bearer " + resolveBearerToken(service));
        }

        HttpResponse<byte[]> response = sendBinaryRequest(requestBuilder.build(), "speech synthesis");
        String mimeType = normalizeMimeType(response.headers().firstValue("Content-Type").orElse(resolveAudioMimeType(options)));
        byte[] body = response.body();
        if (!mimeType.contains("json")) {
            return new GeneratedAudioPayload(body, resolveAudioMimeType(mimeType, options), toJson(Map.of(
                    "provider", defaultString(service.getProviderType(), "openai_compatible"),
                    "endpoint", endpoint.toString(),
                    "model", defaultString(service.getModelCode(), service.getModelName()),
                    "mimeType", mimeType
            )));
        }

        String rawJson = new String(body, StandardCharsets.UTF_8);
        JsonNode root = parseJsonNode(rawJson);
        byte[] audioBytes = extractAudioBytes(root);
        String audioMimeType = extractAudioMimeType(root, options);
        if (audioBytes == null) {
            String audioUrl = extractAudioUrl(root);
            if (!StringUtils.hasText(audioUrl)) {
                throw new IllegalStateException("Speech service returned no recognizable audio payload");
            }
            DownloadedImage downloadedAudio = downloadBinary(URI.create(audioUrl), Duration.ofSeconds(resolveTimeoutSeconds(service)), "audio/*");
            audioBytes = downloadedAudio.bytes();
            audioMimeType = downloadedAudio.mimeType();
        }
        return new GeneratedAudioPayload(audioBytes, resolveAudioMimeType(audioMimeType, options), rawJson);
    }

    private GeneratedAudioPayload generateOpenRouterSpeechPayload(TaskEntity task, ModelServiceEntity service, Map<String, Object> options) {
        URI endpoint = resolveOpenRouterChatEndpoint(service);
        Map<String, Object> requestBody = buildOpenRouterSpeechRequestBody(task, service, options);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(resolveTimeoutSeconds(service)))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream,application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(requestBody)));

        applyTemplateHeaders(requestBuilder, service, task, options);
        if ("bearer".equalsIgnoreCase(service.getAuthType()) || !StringUtils.hasText(service.getAuthType())) {
            requestBuilder.header("Authorization", "Bearer " + resolveBearerToken(service));
        }

        HttpResponse<String> response = sendTextRequest(requestBuilder.build());
        byte[] pcmBytes = extractOpenRouterPcmAudio(response.body());
        if (pcmBytes.length == 0) {
            throw new IllegalStateException("OpenRouter audio stream returned no audio data");
        }
        byte[] wavBytes = wrapPcm16AsWav(pcmBytes, 24000, 1);
        return new GeneratedAudioPayload(wavBytes, "audio/wav", toJson(Map.of(
                "provider", "openrouter",
                "endpoint", endpoint.toString(),
                "model", defaultString(service.getModelCode(), service.getModelName()),
                "mimeType", "audio/wav",
                "pcmBytes", pcmBytes.length
        )));
    }

    private Map<String, Object> buildOpenRouterSpeechRequestBody(TaskEntity task, ModelServiceEntity service, Map<String, Object> options) {
        if (StringUtils.hasText(service.getPayloadTemplate()) && !"{}".equals(service.getPayloadTemplate().trim())) {
            return parseRequestParams(renderTemplate(service.getPayloadTemplate(), task, service, options));
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", StringUtils.hasText(service.getModelCode()) ? service.getModelCode() : service.getModelName());
        requestBody.put("messages", List.of(Map.of("role", "user", "content", task.getInputText())));
        requestBody.put("modalities", List.of("text", "audio"));
        requestBody.put("audio", Map.of(
                "voice", defaultString(readStringOption(options, "voice"), "alloy"),
                "format", "pcm16"
        ));
        requestBody.put("stream", true);
        return requestBody;
    }

    private URI resolveOpenRouterChatEndpoint(ModelServiceEntity service) {
        String endpoint = service.getEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalStateException("OpenRouter endpoint is empty");
        }
        String normalized = endpoint.trim();
        if (normalized.endsWith("/chat/completions")) {
            return URI.create(normalized);
        }
        if (normalized.endsWith("/audio/speech")) {
            return URI.create(normalized.substring(0, normalized.length() - "/audio/speech".length()) + "/chat/completions");
        }
        return URI.create(trimTrailingSlash(normalized) + "/chat/completions");
    }

    private byte[] extractOpenRouterPcmAudio(String eventStream) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!StringUtils.hasText(eventStream)) {
            return output.toByteArray();
        }
        eventStream.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("data:"))
                .map(line -> line.substring("data:".length()).trim())
                .filter(payload -> StringUtils.hasText(payload) && !"[DONE]".equals(payload))
                .forEach(payload -> appendOpenRouterAudioChunk(output, payload));
        return output.toByteArray();
    }

    private void appendOpenRouterAudioChunk(ByteArrayOutputStream output, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode choices = root.path("choices");
            if (!choices.isArray()) {
                return;
            }
            for (JsonNode choice : choices) {
                JsonNode audio = choice.path("delta").path("audio");
                if (audio.hasNonNull("data")) {
                    byte[] chunk = decodeBase64Audio(audio.get("data").asText());
                    if (chunk != null && chunk.length > 0) {
                        output.writeBytes(chunk);
                    }
                }
            }
        } catch (JsonProcessingException ex) {
            log.debug("Skip malformed OpenRouter audio stream chunk: {}", payload, ex);
        }
    }

    private byte[] wrapPcm16AsWav(byte[] pcmBytes, int sampleRate, int channels) {
        int bitsPerSample = 16;
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int dataSize = pcmBytes.length;
        int chunkSize = 36 + dataSize;
        ByteArrayOutputStream wav = new ByteArrayOutputStream(44 + dataSize);
        writeAscii(wav, "RIFF");
        writeLittleEndianInt(wav, chunkSize);
        writeAscii(wav, "WAVE");
        writeAscii(wav, "fmt ");
        writeLittleEndianInt(wav, 16);
        writeLittleEndianShort(wav, 1);
        writeLittleEndianShort(wav, channels);
        writeLittleEndianInt(wav, sampleRate);
        writeLittleEndianInt(wav, byteRate);
        writeLittleEndianShort(wav, blockAlign);
        writeLittleEndianShort(wav, bitsPerSample);
        writeAscii(wav, "data");
        writeLittleEndianInt(wav, dataSize);
        wav.writeBytes(pcmBytes);
        return wav.toByteArray();
    }

    private void writeAscii(ByteArrayOutputStream output, String value) {
        output.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
    }

    private void writeLittleEndianInt(ByteArrayOutputStream output, int value) {
        output.write(value & 0xFF);
        output.write((value >> 8) & 0xFF);
        output.write((value >> 16) & 0xFF);
        output.write((value >> 24) & 0xFF);
    }

    private void writeLittleEndianShort(ByteArrayOutputStream output, int value) {
        output.write(value & 0xFF);
        output.write((value >> 8) & 0xFF);
    }

    private Map<String, Object> buildSpeechRequestBody(TaskEntity task, ModelServiceEntity service, Map<String, Object> options) {
        if (StringUtils.hasText(service.getPayloadTemplate()) && !"{}".equals(service.getPayloadTemplate().trim())) {
            return parseRequestParams(renderTemplate(service.getPayloadTemplate(), task, service, options));
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", StringUtils.hasText(service.getModelCode()) ? service.getModelCode() : service.getModelName());
        requestBody.put("input", task.getInputText());
        requestBody.put("voice", defaultString(readStringOption(options, "voice"), "alloy"));
        requestBody.put("response_format", defaultString(readStringOption(options, "format"), "mp3"));
        double speed = resolveDoubleOption(options, "speed", 1.0D);
        if (speed > 0) {
            requestBody.put("speed", speed);
        }
        return requestBody;
    }

    private URI resolveSpeechEndpoint(ModelServiceEntity service) {
        String endpoint = service.getEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalStateException("Speech service endpoint is empty");
        }
        String normalized = endpoint.trim();
        if (normalized.endsWith("/audio/speech")) {
            return URI.create(normalized);
        }
        if (normalized.endsWith("/chat/completions")) {
            return URI.create(normalized.substring(0, normalized.length() - "/chat/completions".length()) + "/audio/speech");
        }
        return URI.create(trimTrailingSlash(normalized) + "/audio/speech");
    }

    private GeneratedFilePayload generatePptxPayload(
            TaskEntity task,
            ModelServiceEntity service,
            Map<String, Object> options,
            String outlineContent
    ) {
        List<SlideDraft> slides = buildSlideDrafts(task, options, outlineContent);
        PptTheme theme = resolvePptTheme(readStringOption(options, "template"));
        try (XMLSlideShow ppt = new XMLSlideShow(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ppt.setPageSize(new Dimension(1280, 720));
            addCoverSlide(ppt, task.getInputText(), service, options, theme);
            addAgendaSlide(ppt, slides, theme);
            for (int index = 0; index < slides.size(); index++) {
                addContentSlide(ppt, slides.get(index), index + 1, slides.size(), theme);
            }
            addClosingSlide(ppt, task.getInputText(), theme);
            ppt.write(output);
            return new GeneratedFilePayload(
                    output.toByteArray(),
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    outlineContent
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate PPTX file", ex);
        }
    }

    private List<SlideDraft> buildSlideDrafts(TaskEntity task, Map<String, Object> options, String outlineContent) {
        List<SlideDraft> parsedSlides = parseSlidesFromJson(outlineContent);
        if (!parsedSlides.isEmpty()) {
            return limitSlides(parsedSlides, options);
        }

        List<String> lines = normalizeOutlineLines(outlineContent);
        if (lines.isEmpty()) {
            lines = List.of(task.getInputText());
        }
        int maxSlides = resolveIntegerOption(options, "pages", 8);
        List<SlideDraft> slides = new ArrayList<>();
        for (int index = 0; index < Math.min(maxSlides, Math.max(1, lines.size())); index++) {
            int from = Math.min(index * 4, lines.size());
            int to = Math.min(from + 4, lines.size());
            List<String> bullets = from < to ? lines.subList(from, to) : List.of("Expand around the topic with practical examples.");
            String title = index == 0 ? "Core Overview" : "Section " + (index + 1);
            slides.add(new SlideDraft(title, new ArrayList<>(bullets)));
        }
        return slides;
    }

    private List<SlideDraft> parseSlidesFromJson(String outlineContent) {
        String json = stripMarkdownFence(outlineContent);
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode slidesNode = root.isArray() ? root : root.path("slides");
            if (!slidesNode.isArray()) {
                slidesNode = root.path("presentation").path("slides");
            }
            if (!slidesNode.isArray()) {
                slidesNode = root.path("deck").path("slides");
            }
            if (!slidesNode.isArray()) {
                return Collections.emptyList();
            }
            List<SlideDraft> slides = new ArrayList<>();
            for (JsonNode slideNode : slidesNode) {
                String title = firstText(slideNode, List.of("title", "heading", "name"));
                List<String> bullets = new ArrayList<>();
                JsonNode bulletsNode = slideNode.path("bullets");
                if (!bulletsNode.isArray()) {
                    bulletsNode = slideNode.path("points");
                }
                if (!bulletsNode.isArray()) {
                    bulletsNode = slideNode.path("content");
                }
                if (bulletsNode.isArray()) {
                    bulletsNode.forEach(item -> {
                        if (item.isTextual()) {
                            bullets.add(item.asText());
                        } else if (item.hasNonNull("text")) {
                            bullets.add(item.get("text").asText());
                        }
                    });
                } else if (bulletsNode.isTextual()) {
                    bullets.addAll(normalizeOutlineLines(bulletsNode.asText()));
                }
                if (StringUtils.hasText(title) || !bullets.isEmpty()) {
                    slides.add(new SlideDraft(defaultString(title, "Section " + (slides.size() + 1)), bullets));
                }
            }
            return slides;
        } catch (JsonProcessingException ex) {
            return Collections.emptyList();
        }
    }

    private List<SlideDraft> limitSlides(List<SlideDraft> slides, Map<String, Object> options) {
        int maxSlides = resolveIntegerOption(options, "pages", slides.size());
        return slides.stream().limit(Math.max(1, maxSlides)).toList();
    }

    private List<String> normalizeOutlineLines(String content) {
        if (!StringUtils.hasText(content)) {
            return Collections.emptyList();
        }
        return content.lines()
                .map(line -> line.replaceFirst("^\\s*[-*#\\d.\u3001\uFF0E\uFF1A:)+\\s*", "").trim())
                .filter(StringUtils::hasText)
                .filter(line -> !line.equalsIgnoreCase("json"))
                .limit(48)
                .toList();
    }

    private String stripMarkdownFence(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        int objectStart = trimmed.indexOf('{');
        int arrayStart = trimmed.indexOf('[');
        int start = objectStart >= 0 && arrayStart >= 0 ? Math.min(objectStart, arrayStart) : Math.max(objectStart, arrayStart);
        if (start > 0) {
            trimmed = trimmed.substring(start);
        }
        return trimmed;
    }

    private String firstText(JsonNode node, List<String> fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.hasNonNull(fieldName)) {
                return node.get(fieldName).asText();
            }
        }
        return null;
    }

    private void addCoverSlide(XMLSlideShow ppt, String topic, ModelServiceEntity service, Map<String, Object> options, PptTheme theme) {
        XSLFSlide slide = ppt.createSlide();
        paintBackground(slide, theme);
        addAccentShape(slide, 920, -80, 420, 420, theme.accent(), 0.18);
        addAccentShape(slide, -120, 520, 360, 260, theme.secondary(), 0.2);
        addTextBox(slide, defaultString(topic, "PPT Generation Result"), 86, 170, 900, 150, 46D, true, theme.onDark());
        addTextBox(slide, "Generated by " + defaultString(service.getServiceName(), service.getServiceCode()), 92, 340, 940, 44, 20D, false, theme.mutedOnDark());
        addTextBox(slide, "Template: " + defaultString(readStringOption(options, "template"), "business") + "   Slides: " + resolveIntegerOption(options, "pages", 8), 92, 405, 820, 34, 16D, false, theme.accent());
    }

    private void addAgendaSlide(XMLSlideShow ppt, List<SlideDraft> slides, PptTheme theme) {
        XSLFSlide slide = ppt.createSlide();
        addBackground(slide, theme.surface());
        addTopRule(slide, theme);
        addTextBox(slide, "Agenda", 72, 58, 760, 64, 34D, true, theme.primary());
        XSLFTextBox agenda = slide.createTextBox();
        agenda.setAnchor(new Rectangle(92, 150, 760, 440));
        for (int index = 0; index < Math.min(8, slides.size()); index++) {
            XSLFTextParagraph paragraph = agenda.addNewTextParagraph();
            paragraph.setLeftMargin(0D);
            XSLFTextRun marker = paragraph.addNewTextRun();
            marker.setText(String.format("%02d  ", index + 1));
            marker.setFontFamily("Microsoft YaHei");
            marker.setFontSize(22D);
            marker.setBold(true);
            marker.setFontColor(theme.accent());
            XSLFTextRun title = paragraph.addNewTextRun();
            title.setText(slides.get(index).title());
            title.setFontFamily("Microsoft YaHei");
            title.setFontSize(22D);
            title.setFontColor(theme.primary());
        }
        addAccentShape(slide, 930, 150, 230, 360, theme.secondary(), 0.28);
    }

    private void addContentSlide(XMLSlideShow ppt, SlideDraft draft, int pageNo, int totalPages, PptTheme theme) {
        XSLFSlide slide = ppt.createSlide();
        addBackground(slide, theme.surface());
        addTopRule(slide, theme);
        addTextBox(slide, draft.title(), 68, 48, 820, 66, 30D, true, theme.primary());
        addTextBox(slide, pageNo + " / " + totalPages, 1070, 54, 110, 34, 16D, false, theme.secondary());

        XSLFTextBox body = slide.createTextBox();
        body.setAnchor(new Rectangle(90, 150, 780, 455));
        List<String> bullets = draft.bullets().isEmpty() ? List.of("Continue with this section.") : draft.bullets();
        for (String bullet : bullets.stream().limit(6).toList()) {
            XSLFTextParagraph paragraph = body.addNewTextParagraph();
            paragraph.setBullet(true);
            paragraph.setLeftMargin(28D);
            paragraph.setIndent(-18D);
            XSLFTextRun run = paragraph.addNewTextRun();
            run.setText(bullet);
            run.setFontFamily("Microsoft YaHei");
            run.setFontSize(20D);
            run.setFontColor(theme.text());
        }
        addInsightPanel(slide, bullets, theme);
    }

    private void addClosingSlide(XMLSlideShow ppt, String topic, PptTheme theme) {
        XSLFSlide slide = ppt.createSlide();
        paintBackground(slide, theme);
        addAccentShape(slide, 850, 110, 300, 300, theme.accent(), 0.2);
        addTextBox(slide, "Next Steps", 90, 175, 820, 76, 42D, true, theme.onDark());
        addTextBox(slide, defaultString(topic, "Review the generated deck") + " - align owners, timeline and validation metrics.", 94, 285, 920, 86, 22D, false, theme.mutedOnDark());
        addTextBox(slide, "Thank you", 94, 430, 520, 40, 20D, false, theme.accent());
    }

    private void addInsightPanel(XSLFSlide slide, List<String> bullets, PptTheme theme) {
        XSLFAutoShape panel = slide.createAutoShape();
        panel.setShapeType(ShapeType.ROUND_RECT);
        panel.setAnchor(new Rectangle(925, 158, 260, 360));
        panel.setFillColor(theme.panel());
        panel.setLineColor(theme.panel());
        addTextBox(slide, "Speaker Notes", 950, 185, 220, 36, 18D, true, theme.primary());
        String note = bullets.isEmpty() ? "Use this page to expand the core narrative." : bullets.get(0);
        addTextBox(slide, note, 950, 238, 210, 160, 17D, false, theme.text());
        addTextBox(slide, "Focus / Evidence / Decision", 950, 444, 220, 32, 14D, false, theme.secondary());
    }

    private void addTopRule(XSLFSlide slide, PptTheme theme) {
        XSLFAutoShape rule = slide.createAutoShape();
        rule.setShapeType(ShapeType.RECT);
        rule.setAnchor(new Rectangle(0, 0, 1280, 12));
        rule.setFillColor(theme.accent());
        rule.setLineColor(theme.accent());
    }

    private void paintBackground(XSLFSlide slide, PptTheme theme) {
        addBackground(slide, theme.primary());
        XSLFAutoShape band = slide.createAutoShape();
        band.setShapeType(ShapeType.RECT);
        band.setAnchor(new Rectangle(0, 560, 1280, 160));
        band.setFillColor(theme.secondary());
        band.setLineColor(theme.secondary());
    }

    private void addAccentShape(XSLFSlide slide, int x, int y, int width, int height, Color color, double alpha) {
        XSLFAutoShape shape = slide.createAutoShape();
        shape.setShapeType(ShapeType.ELLIPSE);
        shape.setAnchor(new Rectangle(x, y, width, height));
        shape.setFillColor(mix(color, Color.WHITE, alpha));
        shape.setLineColor(mix(color, Color.WHITE, alpha));
    }

    private Color mix(Color base, Color overlay, double overlayWeight) {
        double baseWeight = 1D - overlayWeight;
        return new Color(
                Math.min(255, (int) Math.round(base.getRed() * baseWeight + overlay.getRed() * overlayWeight)),
                Math.min(255, (int) Math.round(base.getGreen() * baseWeight + overlay.getGreen() * overlayWeight)),
                Math.min(255, (int) Math.round(base.getBlue() * baseWeight + overlay.getBlue() * overlayWeight))
        );
    }

    private PptTheme resolvePptTheme(String template) {
        return switch (defaultString(template, "business").toLowerCase()) {
            case "tech" -> new PptTheme(new Color(9, 28, 50), new Color(40, 88, 255), new Color(44, 210, 211), new Color(241, 246, 250), new Color(20, 33, 48), new Color(226, 236, 246), Color.WHITE, new Color(207, 224, 239));
            case "minimal" -> new PptTheme(new Color(38, 38, 35), new Color(143, 120, 82), new Color(215, 194, 151), new Color(250, 248, 242), new Color(52, 50, 45), new Color(239, 232, 217), Color.WHITE, new Color(232, 226, 213));
            default -> new PptTheme(new Color(24, 62, 69), new Color(209, 93, 57), new Color(237, 166, 72), new Color(250, 247, 239), new Color(43, 59, 58), new Color(241, 230, 212), Color.WHITE, new Color(215, 231, 226));
        };
    }

    private void addBackground(XSLFSlide slide, Color color) {
        XSLFAutoShape background = slide.createAutoShape();
        background.setShapeType(ShapeType.RECT);
        background.setAnchor(new Rectangle(0, 0, 1280, 720));
        background.setFillColor(color);
        background.setLineColor(color);
    }

    private void addTextBox(
            XSLFSlide slide,
            String text,
            int x,
            int y,
            int width,
            int height,
            Double fontSize,
            boolean bold,
            Color color
    ) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle(x, y, width, height));
        XSLFTextParagraph paragraph = box.addNewTextParagraph();
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(text);
        run.setFontFamily("Microsoft YaHei");
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setFontColor(color);
    }

    private GeneratedVideoPayload generateVideoPayload(TaskEntity task, ModelServiceEntity service, Map<String, Object> options) {
        URI endpoint = resolveVideoEndpoint(service);
        Map<String, Object> requestBody = buildVideoRequestBody(task, service, options);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(resolveTimeoutSeconds(service)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(requestBody)));

        applyTemplateHeaders(requestBuilder, service, task, options);
        if ("bearer".equalsIgnoreCase(service.getAuthType()) || !StringUtils.hasText(service.getAuthType())) {
            requestBuilder.header("Authorization", "Bearer " + resolveBearerToken(service));
        }

        HttpResponse<String> submitResponse = sendTextRequest(requestBuilder.build());
        JsonNode submitRoot = parseJsonNode(submitResponse.body());
        String directVideoUrl = extractVideoUrl(submitRoot);
        if (StringUtils.hasText(directVideoUrl)) {
            DownloadedImage downloadedVideo = downloadBinary(URI.create(directVideoUrl), Duration.ofSeconds(resolveTimeoutSeconds(service)), "video/*");
            return new GeneratedVideoPayload(downloadedVideo.bytes(), resolveVideoMimeType(downloadedVideo.mimeType()), submitResponse.body());
        }

        String jobId = extractRemoteJobId(submitRoot);
        if (!StringUtils.hasText(jobId)) {
            throw new IllegalStateException("Video service returned no job id or video url");
        }

        JsonNode statusRoot = pollVideoStatus(service, endpoint, jobId, options);
        String videoUrl = extractVideoUrl(statusRoot);
        if (!StringUtils.hasText(videoUrl)) {
            URI contentEndpoint = resolveVideoContentEndpoint(endpoint, jobId);
            DownloadedImage content = downloadBinary(
                    contentEndpoint,
                    Duration.ofSeconds(resolveTimeoutSeconds(service)),
                    "video/*",
                    resolveBearerToken(service)
            );
            return new GeneratedVideoPayload(content.bytes(), resolveVideoMimeType(content.mimeType()), toJson(Map.of(
                    "submit", parseRequestParams(submitResponse.body()),
                    "status", statusRoot
            )));
        }

        DownloadedImage downloadedVideo = downloadBinary(URI.create(videoUrl), Duration.ofSeconds(resolveTimeoutSeconds(service)), "video/*");
        return new GeneratedVideoPayload(downloadedVideo.bytes(), resolveVideoMimeType(downloadedVideo.mimeType()), toJson(Map.of(
                "submit", parseRequestParams(submitResponse.body()),
                "status", statusRoot
        )));
    }

    private Map<String, Object> buildVideoRequestBody(TaskEntity task, ModelServiceEntity service, Map<String, Object> options) {
        if (StringUtils.hasText(service.getPayloadTemplate()) && !"{}".equals(service.getPayloadTemplate().trim())) {
            return parseRequestParams(renderTemplate(service.getPayloadTemplate(), task, service, options));
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", StringUtils.hasText(service.getModelCode()) ? service.getModelCode() : service.getModelName());
        requestBody.put("prompt", task.getInputText());
        String duration = readStringOption(options, "duration");
        if (StringUtils.hasText(duration)) {
            requestBody.put("duration", resolveVideoDurationSeconds(duration));
        }
        String ratio = readStringOption(options, "ratio");
        if (StringUtils.hasText(ratio)) {
            requestBody.put("aspect_ratio", resolveVideoAspectRatio(ratio));
        }
        String style = readStringOption(options, "style");
        if (StringUtils.hasText(style)) {
            requestBody.put("style", style);
        }
        return requestBody;
    }

    private int resolveVideoDurationSeconds(String duration) {
        String normalized = duration.toLowerCase().replace("seconds", "").replace("second", "").replace("s", "").trim();
        try {
            int seconds = Math.max(1, Integer.parseInt(normalized));
            if (seconds <= 4) {
                return 4;
            }
            if (seconds <= 6) {
                return 6;
            }
            return 8;
        } catch (NumberFormatException ex) {
            return 4;
        }
    }

    private String resolveVideoAspectRatio(String ratio) {
        if ("9:16".equals(ratio)) {
            return "9:16";
        }
        return "16:9";
    }

    private URI resolveVideoEndpoint(ModelServiceEntity service) {
        String endpoint = service.getEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            throw new IllegalStateException("Video service endpoint is empty");
        }
        String normalized = endpoint.trim();
        if (normalized.endsWith("/videos")) {
            return URI.create(normalized);
        }
        if (normalized.endsWith("/chat/completions")) {
            return URI.create(normalized.substring(0, normalized.length() - "/chat/completions".length()) + "/videos");
        }
        return URI.create(trimTrailingSlash(normalized) + "/videos");
    }

    private JsonNode pollVideoStatus(ModelServiceEntity service, URI videoEndpoint, String jobId, Map<String, Object> options) {
        int maxAttempts = Math.max(1, resolveIntegerOption(options, "pollAttempts", 60));
        long intervalMs = Math.max(1000L, resolveIntegerOption(options, "pollIntervalMs", 3000));
        URI statusEndpoint = resolveVideoStatusEndpoint(videoEndpoint, jobId);
        JsonNode lastStatus = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            if (attempt > 0) {
                sleepQuietly(intervalMs);
            }
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(statusEndpoint)
                    .timeout(Duration.ofSeconds(resolveTimeoutSeconds(service)))
                    .header("Accept", "application/json")
                    .GET();
            if ("bearer".equalsIgnoreCase(service.getAuthType()) || !StringUtils.hasText(service.getAuthType())) {
                requestBuilder.header("Authorization", "Bearer " + resolveBearerToken(service));
            }
            HttpResponse<String> response = sendTextRequest(requestBuilder.build());
            lastStatus = parseJsonNode(response.body());
            String status = extractRemoteStatus(lastStatus);
            if (isRemoteSuccessStatus(status)) {
                return lastStatus;
            }
            if (isRemoteFailureStatus(status)) {
                throw new IllegalStateException("Video generation failed: " + lastStatus);
            }
        }
        throw new IllegalStateException("Video generation timed out: " + (lastStatus == null ? jobId : lastStatus));
    }

    private URI resolveVideoStatusEndpoint(URI videoEndpoint, String jobId) {
        return URI.create(trimTrailingSlash(videoEndpoint.toString()) + "/" + URLEncoder.encode(jobId, StandardCharsets.UTF_8));
    }

    private URI resolveVideoContentEndpoint(URI videoEndpoint, String jobId) {
        return URI.create(trimTrailingSlash(videoEndpoint.toString()) + "/" + URLEncoder.encode(jobId, StandardCharsets.UTF_8) + "/content");
    }

    private String extractRemoteJobId(JsonNode root) {
        for (String field : List.of("id", "jobId", "job_id", "taskId", "task_id", "requestId", "request_id")) {
            if (root.hasNonNull(field)) {
                return root.get(field).asText();
            }
        }
        JsonNode data = root.path("data");
        if (data.isObject()) {
            return extractRemoteJobId(data);
        }
        return null;
    }

    private String extractRemoteStatus(JsonNode root) {
        for (String field : List.of("status", "state", "task_status")) {
            if (root.hasNonNull(field)) {
                return root.get(field).asText();
            }
        }
        JsonNode data = root.path("data");
        if (data.isObject()) {
            return extractRemoteStatus(data);
        }
        return null;
    }

    private boolean isRemoteSuccessStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String normalized = status.trim().toLowerCase();
        return List.of("success", "succeeded", "completed", "complete", "done", "finished").contains(normalized);
    }

    private boolean isRemoteFailureStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        String normalized = status.trim().toLowerCase();
        return List.of("failed", "failure", "error", "canceled", "cancelled").contains(normalized);
    }

    private String extractVideoUrl(JsonNode root) {
        for (String field : List.of("url", "video_url", "videoUrl", "download_url", "downloadUrl", "content_url", "contentUrl")) {
            if (root.hasNonNull(field)) {
                return root.get(field).asText();
            }
        }
        JsonNode data = root.path("data");
        if (data.isObject()) {
            String url = extractVideoUrl(data);
            if (StringUtils.hasText(url)) {
                return url;
            }
        }
        if (data.isArray() && data.size() > 0) {
            String url = extractVideoUrl(data.get(0));
            if (StringUtils.hasText(url)) {
                return url;
            }
        }
        JsonNode output = root.path("output");
        if (output.isObject()) {
            return extractVideoUrl(output);
        }
        if (output.isArray() && output.size() > 0) {
            return extractVideoUrl(output.get(0));
        }
        return null;
    }

    private String resolveVideoMimeType(String mimeType) {
        String normalized = normalizeMimeType(mimeType);
        return normalized.startsWith("video/") ? normalized : "video/mp4";
    }

    private void sleepQuietly(long intervalMs) {
        try {
            Thread.sleep(intervalMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Video polling interrupted", ex);
        }
    }

    private OpenAiCompatiblePayload invokeOpenAiCompatible(TaskEntity task, ModelServiceEntity service, Map<String, Object> options) {
        URI endpoint = URI.create(service.getEndpoint());
        Map<String, Object> requestBody = buildOpenAiCompatibleRequestBody(task, service, options);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(resolveTimeoutSeconds(service)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(requestBody)));

        applyTemplateHeaders(requestBuilder, service, task, options);
        if ("bearer".equalsIgnoreCase(service.getAuthType()) || !StringUtils.hasText(service.getAuthType())) {
            requestBuilder.header("Authorization", "Bearer " + resolveBearerToken(service));
        }

        HttpResponse<String> response = sendTextRequest(requestBuilder.build());
        JsonNode root = parseJsonNode(response.body());
        String content = extractOpenAiTextContent(root);
        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("OpenAI-compatible service returned no text content");
        }
        return new OpenAiCompatiblePayload(content, response.body());
    }

    private Map<String, Object> buildOpenAiCompatibleRequestBody(
            TaskEntity task,
            ModelServiceEntity service,
            Map<String, Object> options
    ) {
        if (StringUtils.hasText(service.getPayloadTemplate()) && !"{}".equals(service.getPayloadTemplate().trim())) {
            return parseRequestParams(renderTemplate(service.getPayloadTemplate(), task, service, options));
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", StringUtils.hasText(service.getModelCode()) ? service.getModelCode() : service.getModelName());
        requestBody.put("messages", buildOpenAiCompatibleMessages(task, options));
        requestBody.put("temperature", resolveDoubleOption(options, "temperature", 0.7D));
        return requestBody;
    }

    private List<Map<String, Object>> buildOpenAiCompatibleMessages(TaskEntity task, Map<String, Object> options) {
        Map<String, Object> systemMessage = Map.of("role", "system", "content", buildSystemPrompt(task.getFunctionCode(), options));
        List<String> imageDataUrls = resolveAttachedImageDataUrls(task, options);
        if (imageDataUrls.isEmpty()) {
            return List.of(systemMessage, Map.of("role", "user", "content", task.getInputText()));
        }

        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("type", "text", "text", task.getInputText()));
        imageDataUrls.forEach(dataUrl -> content.add(Map.of(
                "type", "image_url",
                "image_url", Map.of("url", dataUrl)
        )));
        return List.of(systemMessage, Map.of("role", "user", "content", content));
    }

    private void applyTemplateHeaders(
            HttpRequest.Builder requestBuilder,
            ModelServiceEntity service,
            TaskEntity task,
            Map<String, Object> options
    ) {
        if (!StringUtils.hasText(service.getHeaderTemplate()) || "{}".equals(service.getHeaderTemplate().trim())) {
            return;
        }
        Map<String, Object> headers = parseRequestParams(renderTemplate(service.getHeaderTemplate(), task, service, options));
        headers.forEach((key, value) -> {
            if (StringUtils.hasText(key) && value != null) {
                String headerValue = String.valueOf(value);
                if (StringUtils.hasText(headerValue) && !"Authorization".equalsIgnoreCase(key)) {
                    requestBuilder.header(key, headerValue);
                }
            }
        });
    }

    private String renderTemplate(String template, TaskEntity task, ModelServiceEntity service, Map<String, Object> options) {
        String rendered = template;
        rendered = rendered.replace("${modelCode}", defaultString(service.getModelCode(), service.getModelName()));
        rendered = rendered.replace("${modelName}", defaultString(service.getModelName(), service.getModelCode()));
        rendered = rendered.replace("${serviceCode}", defaultString(service.getServiceCode(), ""));
        rendered = rendered.replace("${inputText}", escapeJsonString(task.getInputText()));
        rendered = rendered.replace("${optionsJson}", toJson(options));
        for (Map.Entry<String, Object> entry : options.entrySet()) {
            rendered = rendered.replace("${" + entry.getKey() + "}", escapeJsonString(String.valueOf(entry.getValue())));
        }
        return rendered;
    }

    private String buildSystemPrompt(String capabilityCode, Map<String, Object> options) {
        String optionSummary = options.isEmpty() ? "" : "\nCurrent options: " + toJson(options);
        return switch (capabilityCode) {
            case CAPABILITY_TEXT_TO_PPT -> """
                    You generate PPT outlines. Return JSON only, no markdown.
                    Schema: {"title":"deck title","slides":[{"title":"slide title","bullets":["point 1","point 2"]}]}.
                    Keep each bullet concise and practical.
                    """ + optionSummary;
            case CAPABILITY_IMAGE_RECOGNITION -> "You analyze images or visual task descriptions accurately." + optionSummary;
            case CAPABILITY_TEXT_TO_SPEECH -> "You prepare text-to-speech task instructions and return concise synthesis guidance." + optionSummary;
            case CAPABILITY_TEXT_TO_VIDEO -> "You prepare video generation prompts and return concise production guidance." + optionSummary;
            default -> "You are a helpful multimodal workbench assistant." + optionSummary;
        };
    }

    private String extractOpenAiTextContent(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).path("message");
            JsonNode content = message.path("content");
            if (content.isTextual()) {
                return content.asText();
            }
            if (content.isArray()) {
                List<String> parts = new ArrayList<>();
                content.forEach(item -> {
                    if (item.hasNonNull("text")) {
                        parts.add(item.get("text").asText());
                    } else if (item.hasNonNull("content")) {
                        parts.add(item.get("content").asText());
                    }
                });
                return String.join("\n", parts);
            }
        }
        if (root.hasNonNull("output_text")) {
            return root.get("output_text").asText();
        }
        if (root.hasNonNull("text")) {
            return root.get("text").asText();
        }
        return null;
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
            throw new IllegalStateException("閻㈢喐鍨氶崶鍓у娣囨繂鐡ㄦ径杈Е", ex);
        }
    }

    private FileAssetEntity persistGeneratedAudio(
            TaskEntity task,
            ModelServiceEntity service,
            Map<String, Object> options,
            GeneratedAudioPayload payload
    ) {
        try {
            String fileId = generateId("file");
            String extension = mediaTypeToExtension(payload.mimeType(), "mp3");
            Path relativePath = Paths.get("speech-generation", LocalDate.now().toString(), fileId + "." + extension);
            Path absolutePath = resolveStorageRoot().resolve(relativePath);
            Files.createDirectories(absolutePath.getParent());
            Files.write(absolutePath, payload.bytes());

            FileAssetEntity fileAsset = new FileAssetEntity();
            fileAsset.setFileId(fileId);
            fileAsset.setRelatedType("task");
            fileAsset.setRelatedId(task.getTaskId());
            fileAsset.setFileRole("result");
            fileAsset.setFileType("audio");
            fileAsset.setFileName(task.getTaskId() + "." + extension);
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
            fileAsset.setExtraMeta(toJson(extraMeta));
            fileAssetMapper.insert(fileAsset);
            return fileAsset;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to persist generated audio", ex);
        }
    }

    private FileAssetEntity persistGeneratedFile(
            TaskEntity task,
            ModelServiceEntity service,
            Map<String, Object> options,
            GeneratedFilePayload payload,
            String directory,
            String fileType,
            String fallbackExtension
    ) {
        try {
            String fileId = generateId("file");
            String extension = mediaTypeToExtension(payload.mimeType(), fallbackExtension);
            Path relativePath = Paths.get(directory, LocalDate.now().toString(), fileId + "." + extension);
            Path absolutePath = resolveStorageRoot().resolve(relativePath);
            Files.createDirectories(absolutePath.getParent());
            Files.write(absolutePath, payload.bytes());

            FileAssetEntity fileAsset = new FileAssetEntity();
            fileAsset.setFileId(fileId);
            fileAsset.setRelatedType("task");
            fileAsset.setRelatedId(task.getTaskId());
            fileAsset.setFileRole("result");
            fileAsset.setFileType(fileType);
            fileAsset.setFileName(task.getTaskId() + "." + extension);
            fileAsset.setStorageKey(relativePath.toString().replace('\\', '/'));
            fileAsset.setFileSize((long) payload.bytes().length);
            fileAsset.setMimeType(payload.mimeType());
            fileAsset.setDownloadUrl("/api/workbench/files/" + fileId + "/download");
            fileAsset.setSourceTaskId(task.getTaskId());

            Map<String, Object> extraMeta = new LinkedHashMap<>();
            extraMeta.put("serviceCode", service.getServiceCode());
            extraMeta.put("modelName", service.getModelName());
            extraMeta.put("prompt", task.getInputText());
            extraMeta.put("options", options);
            if (StringUtils.hasText(payload.sourceText())) {
                extraMeta.put("sourceText", payload.sourceText());
            }
            fileAsset.setExtraMeta(toJson(extraMeta));
            fileAssetMapper.insert(fileAsset);
            return fileAsset;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to persist generated file", ex);
        }
    }

    private void upsertTaskResult(
            TaskEntity task,
            FileAssetEntity fileAsset,
            String resultPayload,
            String rawResponse,
            String resultType
    ) {
        TaskResultEntity existing = taskResultMapper.selectOne(new LambdaQueryWrapper<TaskResultEntity>()
                .eq(TaskResultEntity::getTaskId, task.getTaskId())
                .last("limit 1"));
        TaskResultEntity result = existing == null ? new TaskResultEntity() : existing;
        if (existing == null) {
            result.setResultId(generateId("result"));
            result.setTaskId(task.getTaskId());
        }
        result.setRecordId(findRecordIdByTaskId(task.getTaskId()));
        result.setResultType(resultType);
        result.setStatus("SUCCESS");
        result.setTextResult("Task completed successfully.");
        result.setFileIds(fileAsset == null ? "[]" : toJson(List.of(fileAsset.getFileId())));
        result.setStructuredResult(resultPayload);
        result.setRawResponse(StringUtils.hasText(rawResponse) ? rawResponse : "{}");
        result.setErrorDetail("{}");
        if (existing == null) {
            taskResultMapper.insert(result);
        } else {
            taskResultMapper.updateById(result);
        }
    }

    private void markTaskSucceeded(TaskEntity originalTask, String resultPayload, String resultType) {
        TaskEntity task = findTask(originalTask.getTaskId());
        OffsetDateTime finishedAt = OffsetDateTime.now();
        task.setStatus("SUCCESS");
        task.setResultType(resultType);
        if (task.getStartedAt() == null) {
            task.setStartedAt(task.getCreatedAt() == null ? finishedAt : task.getCreatedAt());
        }
        task.setFinishedAt(finishedAt);
        task.setDurationMs(Math.max(0L, Duration.between(task.getStartedAt(), finishedAt).toMillis()));
        task.setErrorMessage(null);
        taskMapper.updateById(task);
        updateCallRecordSucceeded(task, resultPayload);
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
        updateCallRecordFailed(task, errorMessage);

        TaskResultEntity existing = taskResultMapper.selectOne(new LambdaQueryWrapper<TaskResultEntity>()
                .eq(TaskResultEntity::getTaskId, task.getTaskId())
                .last("limit 1"));
        if (existing == null) {
            TaskResultEntity result = new TaskResultEntity();
            result.setResultId(generateId("result"));
            result.setTaskId(task.getTaskId());
            result.setRecordId(findRecordIdByTaskId(task.getTaskId()));
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
            errorMessageEntity.setContentText("Task execution failed: " + errorMessage);
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
        updateCallRecordRunning(task);
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
            String resultType = resolveMockResultType(latestTask.getFunctionCode());
            FileAssetEntity placeholderFile = "image".equals(resultType) ? persistMockPlaceholderFile(latestTask) : null;
            String resultPayload = buildMockResultPayload(latestTask, resultType, placeholderFile);
            upsertTaskResult(latestTask, placeholderFile, resultPayload, "{}", resultType);
            markTaskSucceeded(latestTask, resultPayload, resultType);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            markTaskFailed(task, "task interrupted", toJson(Map.of("type", "interrupted")));
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

    private String buildMockResultPayload(TaskEntity task, String resultType, FileAssetEntity fileAsset) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "Task Result / Mock");
        payload.put("summary", "Mock output generated for capability: " + resolveCapabilityLabel(task.getFunctionCode()));
        payload.put("chips", List.of("status: success", "mode: mock", "type: " + resultType));
        payload.put("actionLabel", "Continue from this result");
        payload.put("kind", resultType);
        if ("image".equals(resultType) && fileAsset != null) {
            payload.put("previewImageUrl", fileAsset.getPreviewUrl());
            payload.put("downloadUrl", fileAsset.getDownloadUrl());
            payload.put("fileId", fileAsset.getFileId());
        } else if ("text".equals(resultType)) {
            payload.put("text", "Mock analysis result: no issues detected in current input.");
        } else if ("audio".equals(resultType) || "video".equals(resultType) || "file".equals(resultType)) {
            payload.put("text", "Mock " + resultType + " output is ready. Asset generation is not enabled yet.");
        }
        return toJson(payload);
    }

    private String buildTextResultPayload(
            TaskEntity task,
            ModelServiceEntity service,
            Map<String, Object> options,
            String content
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", service.getServiceName() + " Result");
        payload.put("summary", content);
        payload.put("text", content);
        payload.put("chips", buildResultChips(service, options));
        payload.put("actionLabel", "Continue from this result");
        payload.put("kind", resolveConfiguredResultType(task.getFunctionCode(), service));
        return toJson(payload);
    }

    private String resolveMockResultType(String capabilityCode) {
        return switch (capabilityCode) {
            case "text_to_speech" -> "audio";
            case "text_to_ppt" -> "file";
            case "text_to_video" -> "video";
            case "image_recognition" -> "text";
            default -> "image";
        };
    }

    private String resolveConfiguredResultType(String capabilityCode, ModelServiceEntity service) {
        String modelType = service == null ? null : service.getModelType();
        if ("ppt".equalsIgnoreCase(modelType)) {
            return "file";
        }
        if ("audio".equalsIgnoreCase(modelType)) {
            return "audio";
        }
        if ("video".equalsIgnoreCase(modelType)) {
            return "video";
        }
        return switch (capabilityCode) {
            case CAPABILITY_TEXT_TO_SPEECH -> "audio";
            case CAPABILITY_TEXT_TO_PPT -> "file";
            case CAPABILITY_TEXT_TO_VIDEO -> "video";
            default -> "text";
        };
    }

    private String resolveCapabilityLabel(String capabilityCode) {
        if (!StringUtils.hasText(capabilityCode)) {
            return "unknown";
        }
        return capabilityCode.replace('_', ' ');
    }

    private String buildResultPayload(
            TaskEntity task,
            ModelServiceEntity service,
            Map<String, Object> options,
            FileAssetEntity fileAsset,
            String revisedPrompt
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "Image Generation Result");
        payload.put("summary", buildImageSummary(task.getInputText(), revisedPrompt));
        payload.put("chips", buildResultChips(service, options));
        payload.put("actionLabel", "閸╄桨绨拠銉ユ禈缂佈呯敾");
        payload.put("previewImageUrl", fileAsset.getPreviewUrl());
        payload.put("downloadUrl", fileAsset.getDownloadUrl());
        payload.put("fileId", fileAsset.getFileId());
        payload.put("kind", "image");
        return toJson(payload);
    }

    private String buildAudioResultPayload(
            TaskEntity task,
            ModelServiceEntity service,
            Map<String, Object> options,
            FileAssetEntity fileAsset
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "Speech Generation Result");
        payload.put("summary", "Audio generated from your current text. You can download the result file.");
        payload.put("chips", buildResultChips(service, options));
        payload.put("actionLabel", "Continue from this result");
        payload.put("kind", "audio");
        payload.put("downloadUrl", fileAsset.getDownloadUrl());
        payload.put("fileId", fileAsset.getFileId());
        payload.put("fileName", fileAsset.getFileName());
        payload.put("mimeType", fileAsset.getMimeType());
        payload.put("text", task.getInputText());
        return toJson(payload);
    }

    private String buildAssetResultPayload(
            TaskEntity task,
            ModelServiceEntity service,
            Map<String, Object> options,
            FileAssetEntity fileAsset,
            String title,
            String summary,
            String kind
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("summary", summary);
        payload.put("chips", buildResultChips(service, options));
        payload.put("actionLabel", "Continue from this result");
        payload.put("kind", kind);
        payload.put("downloadUrl", fileAsset.getDownloadUrl());
        payload.put("fileId", fileAsset.getFileId());
        payload.put("fileName", fileAsset.getFileName());
        payload.put("text", task.getInputText());
        if ("video".equals(kind)) {
            payload.put("previewVideoUrl", fileAsset.getPreviewUrl());
        }
        return toJson(payload);
    }

    private String buildImageSummary(String prompt, String revisedPrompt) {
        if (StringUtils.hasText(revisedPrompt) && !revisedPrompt.equals(prompt)) {
            return "Image generated from your prompt. Revised prompt metadata was captured.";
        }
        return "Image generated from your current input. You can preview or download it.";
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

    private String createCallRecord(TaskEntity task) {
        CallRecordEntity record = new CallRecordEntity();
        record.setRecordId(generateId("record"));
        record.setTaskId(task.getTaskId());
        record.setSessionId(task.getSessionId());
        record.setMessageId(task.getMessageId());
        record.setParentTaskId(task.getParentTaskId());
        record.setSourceAssetIds(defaultJsonArray(task.getSourceAssetIds()));
        record.setFunctionCode(task.getFunctionCode());
        record.setSelectionMode(task.getSelectionMode());
        record.setResolvedIntent(task.getResolvedIntent());
        record.setServiceCode(task.getServiceCode());
        record.setModelName(task.getModelName());
        record.setRequestSummary(abbreviatePreview(task.getInputText()));
        record.setInputText(task.getInputText());
        record.setRequestParams(defaultJsonObject(task.getRequestParams()));
        record.setInputAssets(defaultJsonArray(task.getInputAssetIds()));
        record.setStatus(task.getStatus());
        record.setResultSummary("{}");
        record.setDownloadCount(0);
        record.setRecordStatus("active");
        record.setTags("[]");
        record.setIsDeleted(false);
        callRecordMapper.insert(record);
        return record.getRecordId();
    }

    private void updateCallRecordRunning(TaskEntity task) {
        CallRecordEntity record = findCallRecordByTaskId(task.getTaskId());
        if (record == null) {
            return;
        }
        record.setStatus("RUNNING");
        record.setStartedAt(task.getStartedAt());
        callRecordMapper.updateById(record);
    }

    private void updateCallRecordSucceeded(TaskEntity task, String resultPayload) {
        CallRecordEntity record = findCallRecordByTaskId(task.getTaskId());
        if (record == null) {
            return;
        }
        record.setStatus("SUCCESS");
        record.setResultType(task.getResultType());
        record.setResultSummary(StringUtils.hasText(resultPayload) ? resultPayload : "{}");
        record.setErrorMessage(null);
        record.setStartedAt(task.getStartedAt());
        record.setFinishedAt(task.getFinishedAt());
        record.setDurationMs(task.getDurationMs());
        callRecordMapper.updateById(record);
    }

    private void updateCallRecordFailed(TaskEntity task, String errorMessage) {
        CallRecordEntity record = findCallRecordByTaskId(task.getTaskId());
        if (record == null) {
            return;
        }
        record.setStatus("FAILED");
        record.setErrorMessage(errorMessage);
        record.setStartedAt(task.getStartedAt());
        record.setFinishedAt(task.getFinishedAt());
        record.setDurationMs(task.getDurationMs());
        callRecordMapper.updateById(record);
    }

    private CallRecordEntity findCallRecordByTaskId(String taskId) {
        return callRecordMapper.selectOne(new LambdaQueryWrapper<CallRecordEntity>()
                .eq(CallRecordEntity::getTaskId, taskId)
                .eq(CallRecordEntity::getIsDeleted, false)
                .last("limit 1"));
    }

    private String findRecordIdByTaskId(String taskId) {
        CallRecordEntity record = findCallRecordByTaskId(taskId);
        return record == null ? null : record.getRecordId();
    }

    private String defaultJsonObject(String value) {
        return StringUtils.hasText(value) ? value : "{}";
    }

    private String defaultJsonArray(String value) {
        return StringUtils.hasText(value) ? value : "[]";
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
            ChatSessionEntity existingConversation = findConversation(conversationId);
            if ("ARCHIVED".equals(existingConversation.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "conversation is archived, restore it before submitting");
            }
            return existingConversation;
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
        List<ModelServiceEntity> boundServices = listEnabledServicesForCapability(capability.getFunctionCode(), null);
        for (ModelServiceEntity service : boundServices) {
            FunctionModelBindingEntity binding = findEnabledBinding(capability.getFunctionCode(), service.getServiceCode());
            if (binding != null && Boolean.TRUE.equals(binding.getIsDefault())) {
                return service;
            }
        }
        if (!boundServices.isEmpty()) {
            return boundServices.get(0);
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
                .orderByDesc(ModelServiceEntity::getIsDefault)
                .orderByAsc(ModelServiceEntity::getSortOrder)
                .orderByAsc(ModelServiceEntity::getCreatedAt)
                .last("limit 1"));
        if (fallback == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "no enabled model service found for capability");
        }
        return fallback;
    }

    private List<ModelServiceEntity> listEnabledServicesForCapability(String functionCode, String preferredServiceCode) {
        List<FunctionModelBindingEntity> bindings = functionModelBindingMapper.selectList(new LambdaQueryWrapper<FunctionModelBindingEntity>()
                .eq(FunctionModelBindingEntity::getFunctionCode, functionCode)
                .eq(FunctionModelBindingEntity::getEnabled, true)
                .orderByDesc(FunctionModelBindingEntity::getIsDefault)
                .orderByAsc(FunctionModelBindingEntity::getSortOrder)
                .orderByAsc(FunctionModelBindingEntity::getCreatedAt));
        List<String> serviceCodes = bindings.stream()
                .map(FunctionModelBindingEntity::getServiceCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (serviceCodes.isEmpty()) {
            return modelServiceMapper.selectList(new LambdaQueryWrapper<ModelServiceEntity>()
                    .eq(ModelServiceEntity::getFunctionCode, functionCode)
                    .eq(ModelServiceEntity::getEnabled, true)
                    .orderByDesc(ModelServiceEntity::getIsDefault)
                    .orderByAsc(ModelServiceEntity::getSortOrder)
                    .orderByAsc(ModelServiceEntity::getCreatedAt));
        }

        Map<String, ModelServiceEntity> serviceMap = modelServiceMapper.selectList(new LambdaQueryWrapper<ModelServiceEntity>()
                        .in(ModelServiceEntity::getServiceCode, serviceCodes)
                        .eq(ModelServiceEntity::getEnabled, true))
                .stream()
                .collect(Collectors.toMap(ModelServiceEntity::getServiceCode, Function.identity()));

        List<ModelServiceEntity> orderedServices = serviceCodes.stream()
                .map(serviceMap::get)
                .filter(service -> service != null)
                .collect(Collectors.toCollection(ArrayList::new));
        if (StringUtils.hasText(preferredServiceCode)) {
            orderedServices.stream()
                    .filter(service -> preferredServiceCode.equals(service.getServiceCode()))
                    .findFirst()
                    .ifPresent(service -> {
                        orderedServices.remove(service);
                        orderedServices.add(0, service);
                    });
        }
        return orderedServices;
    }

    private FunctionModelBindingEntity findEnabledBinding(String functionCode, String serviceCode) {
        if (!StringUtils.hasText(functionCode) || !StringUtils.hasText(serviceCode)) {
            return null;
        }
        return functionModelBindingMapper.selectOne(new LambdaQueryWrapper<FunctionModelBindingEntity>()
                .eq(FunctionModelBindingEntity::getFunctionCode, functionCode)
                .eq(FunctionModelBindingEntity::getServiceCode, serviceCode)
                .eq(FunctionModelBindingEntity::getEnabled, true)
                .last("limit 1"));
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

    private String normalizeConversationScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return "active";
        }
        String normalized = scope.trim().toLowerCase();
        return switch (normalized) {
            case "archived", "all" -> normalized;
            default -> "active";
        };
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

    private ModelServiceEntity findModelService(String serviceCode) {
        ModelServiceEntity service = modelServiceMapper.selectOne(new LambdaQueryWrapper<ModelServiceEntity>()
                .eq(ModelServiceEntity::getServiceCode, serviceCode)
                .last("limit 1"));
        if (service == null) {
            throw new IllegalStateException("Model service config not found: " + serviceCode);
        }
        return service;
    }

    private boolean isOpenAiCompatible(ModelServiceEntity service) {
        return service != null
                && StringUtils.hasText(service.getEndpoint())
                && "openai_compatible".equalsIgnoreCase(service.getProviderType());
    }

    private boolean isOpenRouterImageGenerationService(ModelServiceEntity service) {
        if (service == null || !StringUtils.hasText(service.getEndpoint())) {
            return false;
        }
        String providerType = defaultString(service.getProviderType(), "").toLowerCase();
        String endpoint = service.getEndpoint().toLowerCase();
        String modelType = defaultString(service.getModelType(), "").toLowerCase();
        return "openrouter_image".equals(providerType)
                || ("image".equals(modelType) && endpoint.contains("/openrouter/"));
    }

    private boolean isSpeechSynthesisService(ModelServiceEntity service) {
        if (service == null || !StringUtils.hasText(service.getEndpoint())) {
            return false;
        }
        String providerType = defaultString(service.getProviderType(), "").toLowerCase();
        String modelType = defaultString(service.getModelType(), "").toLowerCase();
        return "audio".equals(modelType)
                || "tts".equals(providerType)
                || "openai_tts".equals(providerType)
                || "openrouter_tts".equals(providerType);
    }

    private boolean isVideoGenerationService(ModelServiceEntity service) {
        if (service == null || !StringUtils.hasText(service.getEndpoint())) {
            return false;
        }
        String providerType = defaultString(service.getProviderType(), "").toLowerCase();
        String modelType = defaultString(service.getModelType(), "").toLowerCase();
        return "video".equals(modelType)
                || "video_generation".equals(providerType)
                || "openrouter_video".equals(providerType);
    }

    private String resolveBearerToken(ModelServiceEntity service) {
        if (StringUtils.hasText(service.getSecretRef())) {
            String value = System.getenv(service.getSecretRef().trim());
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        if (StringUtils.hasText(service.getApiKeySecret())) {
            return service.getApiKeySecret();
        }
        String normalizedServiceCode = service.getServiceCode() == null
                ? ""
                : service.getServiceCode().replaceAll("[^A-Za-z0-9]", "_").toUpperCase();
        List<String> candidateKeys = List.of(
                "MODEL_SERVICE_API_KEY__" + normalizedServiceCode,
                "MODEL_SERVICE_API_KEY",
                "OPENROUTER_API_KEY",
                "OPENAI_API_KEY"
        );
        for (String key : candidateKeys) {
            String value = System.getenv(key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        throw new IllegalStateException("Bearer token not found. Configure secretRef, API key, or environment variable MODEL_SERVICE_API_KEY__" + normalizedServiceCode);
    }

    private String resolveErrorMessage(Exception ex) {
        Throwable current = ex;
        while (current != null) {
            if (StringUtils.hasText(current.getMessage())) {
                return current.getMessage();
            }
            current = current.getCause();
        }
        return "Task execution failed";
    }

    private String resolveRequestParams(WorkbenchChatRequest request) {
        String rawOptions = StringUtils.hasText(request.composerOptions())
                ? request.composerOptions()
                : request.options();
        Map<String, Object> params = new LinkedHashMap<>(parseRequestParams(rawOptions));
        if (StringUtils.hasText(request.schemaVersion())) {
            params.put("schemaVersion", request.schemaVersion());
        }
        if (StringUtils.hasText(request.templateCode())) {
            params.put("templateCode", request.templateCode());
        }
        if (StringUtils.hasText(request.attachments())) {
            params.put("attachments", parseRequestParams(request.attachments()));
        }
        return params.isEmpty() ? "{}" : toJson(params);
    }

    private String resolveInputAssetIds(WorkbenchChatRequest request) {
        List<String> fileIds = extractAttachmentFileIds(request.attachments());
        return fileIds.isEmpty() ? "[]" : toJson(fileIds);
    }

    private List<String> extractAttachmentFileIds(String rawAttachments) {
        if (!StringUtils.hasText(rawAttachments)) {
            return Collections.emptyList();
        }
        try {
            JsonNode root = objectMapper.readTree(rawAttachments);
            List<String> fileIds = new ArrayList<>();
            if (root.isArray()) {
                root.forEach(item -> {
                    if (item.isTextual()) {
                        fileIds.add(item.asText());
                    } else if (item.hasNonNull("fileId")) {
                        fileIds.add(item.get("fileId").asText());
                    }
                });
            } else if (root.hasNonNull("fileId")) {
                fileIds.add(root.get("fileId").asText());
            }
            return fileIds.stream().filter(StringUtils::hasText).distinct().toList();
        } catch (JsonProcessingException ex) {
            return Collections.emptyList();
        }
    }

    private List<String> resolveAttachedImageDataUrls(TaskEntity task, Map<String, Object> options) {
        List<String> fileIds = new ArrayList<>();
        Object attachments = options.get("attachments");
        if (attachments != null) {
            fileIds.addAll(extractAttachmentFileIds(toJson(attachments)));
        }
        if (StringUtils.hasText(task.getInputAssetIds())) {
            fileIds.addAll(extractAttachmentFileIds(task.getInputAssetIds()));
        }
        if (fileIds.isEmpty()) {
            return Collections.emptyList();
        }
        return fileIds.stream()
                .distinct()
                .map(this::buildImageDataUrl)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String buildImageDataUrl(String fileId) {
        FileAssetEntity entity = fileAssetMapper.selectOne(new LambdaQueryWrapper<FileAssetEntity>()
                .eq(FileAssetEntity::getFileId, fileId)
                .last("limit 1"));
        if (entity == null || !normalizeMimeType(entity.getMimeType()).startsWith("image/")) {
            return null;
        }
        Path absolutePath = resolveStorageRoot().resolve(entity.getStorageKey()).normalize();
        if (!absolutePath.startsWith(resolveStorageRoot()) || !Files.exists(absolutePath)) {
            return null;
        }
        try {
            String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(absolutePath));
            return "data:" + normalizeMimeType(entity.getMimeType()) + ";base64," + base64;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read attached image", ex);
        }
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

    private double resolveDoubleOption(Map<String, Object> options, String key, double defaultValue) {
        Object value = options.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private int resolveIntegerOption(Map<String, Object> options, String key, int defaultValue) {
        Object value = options.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String escapeJsonString(String value) {
        if (value == null) {
            return "";
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            return json.substring(1, json.length() - 1);
        } catch (JsonProcessingException ex) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }

    private JsonNode parseJsonNode(String rawJson) {
        try {
            return objectMapper.readTree(rawJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("download failed", ex);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON serialization failed", ex);
        }
    }

    private String extractImageUrl(JsonNode root) {
        List<JsonNode> candidates = List.of(
                root.path("choices").path(0).path("message").path("images"),
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
                JsonNode imageUrl = first.path("image_url");
                if (imageUrl.isTextual()) {
                    return imageUrl.asText();
                }
                if (imageUrl.hasNonNull("url")) {
                    return imageUrl.get("url").asText();
                }
            }
        }
        if (root.hasNonNull("url")) {
            return root.get("url").asText();
        }
        return findImageUrl(root);
    }

    private byte[] extractImageBytes(JsonNode root) {
        List<JsonNode> candidates = List.of(
                root.path("choices").path(0).path("message").path("images"),
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
                JsonNode imageUrl = first.path("image_url");
                if (imageUrl.hasNonNull("url")) {
                    return decodeBase64Image(imageUrl.get("url").asText());
                }
            }
        }
        return findImageBytes(root);
    }

    private String extractImageMimeType(JsonNode root) {
        if (root.hasNonNull("mime_type")) {
            return root.get("mime_type").asText();
        }
        List<JsonNode> candidates = List.of(
                root.path("choices").path(0).path("message").path("images"),
                root.path("data"),
                root.path("images"),
                root.path("output")
        );
        for (JsonNode candidate : candidates) {
            if (candidate.isArray() && candidate.size() > 0) {
                JsonNode first = candidate.get(0);
                if (first.hasNonNull("mime_type")) {
                    return first.get("mime_type").asText();
                }
                JsonNode imageUrl = first.path("image_url");
                if (imageUrl.hasNonNull("url")) {
                    return extractDataUrlMimeType(imageUrl.get("url").asText(), DEFAULT_IMAGE_MIME_TYPE);
                }
            }
        }
        return DEFAULT_IMAGE_MIME_TYPE;
    }

    private String findImageUrl(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            JsonNode imageUrl = node.path("image_url");
            if (imageUrl.isTextual()) {
                return imageUrl.asText();
            }
            if (imageUrl.hasNonNull("url")) {
                return imageUrl.get("url").asText();
            }
            for (String field : List.of("url", "download_url", "imageUrl")) {
                if (node.hasNonNull(field) && node.get(field).isTextual()) {
                    String value = node.get(field).asText();
                    if (value.startsWith("data:image/") || value.startsWith("http://") || value.startsWith("https://")) {
                        return value;
                    }
                }
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                String found = findImageUrl(fields.next().getValue());
                if (StringUtils.hasText(found)) {
                    return found;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                String found = findImageUrl(item);
                if (StringUtils.hasText(found)) {
                    return found;
                }
            }
        }
        return null;
    }

    private byte[] findImageBytes(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            JsonNode imageUrl = node.path("image_url");
            if (imageUrl.hasNonNull("url")) {
                byte[] decoded = decodeBase64Image(imageUrl.get("url").asText());
                if (decoded != null) {
                    return decoded;
                }
            }
            for (String field : List.of("b64_json", "b64", "base64", "data")) {
                if (node.hasNonNull(field) && node.get(field).isTextual()) {
                    byte[] decoded = decodeBase64Image(node.get(field).asText());
                    if (decoded != null) {
                        return decoded;
                    }
                }
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                byte[] decoded = findImageBytes(fields.next().getValue());
                if (decoded != null) {
                    return decoded;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                byte[] decoded = findImageBytes(item);
                if (decoded != null) {
                    return decoded;
                }
            }
        }
        return null;
    }

    private byte[] decodeBase64Image(String value) {
        if (!StringUtils.hasText(value) || value.startsWith("http://") || value.startsWith("https://")) {
            return null;
        }
        String candidate = value.trim();
        int commaIndex = candidate.indexOf(',');
        if (candidate.startsWith("data:") && commaIndex >= 0) {
            candidate = candidate.substring(commaIndex + 1);
        }
        try {
            return Base64.getDecoder().decode(candidate);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String extractDataUrlMimeType(String value, String fallback) {
        if (!StringUtils.hasText(value) || !value.startsWith("data:")) {
            return fallback;
        }
        int semicolonIndex = value.indexOf(';');
        if (semicolonIndex <= "data:".length()) {
            return fallback;
        }
        return value.substring("data:".length(), semicolonIndex);
    }

    private byte[] extractAudioBytes(JsonNode root) {
        List<JsonNode> candidates = List.of(root, root.path("data"), root.path("audio"), root.path("output"));
        for (JsonNode candidate : candidates) {
            if (candidate.isTextual()) {
                byte[] decoded = decodeBase64Audio(candidate.asText());
                if (decoded != null) {
                    return decoded;
                }
            }
            if (candidate.isObject()) {
                for (String field : List.of("b64_json", "b64", "base64", "audio", "data")) {
                    if (candidate.hasNonNull(field)) {
                        byte[] decoded = decodeBase64Audio(candidate.get(field).asText());
                        if (decoded != null) {
                            return decoded;
                        }
                    }
                }
            }
            if (candidate.isArray() && candidate.size() > 0) {
                byte[] decoded = extractAudioBytes(candidate.get(0));
                if (decoded != null) {
                    return decoded;
                }
            }
        }
        return null;
    }

    private String extractAudioUrl(JsonNode root) {
        List<JsonNode> candidates = List.of(root, root.path("data"), root.path("audio"), root.path("output"));
        for (JsonNode candidate : candidates) {
            if (candidate.isObject()) {
                for (String field : List.of("url", "audio_url", "download_url")) {
                    if (candidate.hasNonNull(field)) {
                        return candidate.get(field).asText();
                    }
                }
            }
            if (candidate.isArray() && candidate.size() > 0) {
                String url = extractAudioUrl(candidate.get(0));
                if (StringUtils.hasText(url)) {
                    return url;
                }
            }
        }
        return null;
    }

    private String extractAudioMimeType(JsonNode root, Map<String, Object> options) {
        List<JsonNode> candidates = List.of(root, root.path("data"), root.path("audio"), root.path("output"));
        for (JsonNode candidate : candidates) {
            if (candidate.isObject()) {
                for (String field : List.of("mime_type", "mimeType", "content_type", "contentType")) {
                    if (candidate.hasNonNull(field)) {
                        return candidate.get(field).asText();
                    }
                }
            }
            if (candidate.isArray() && candidate.size() > 0) {
                String mimeType = extractAudioMimeType(candidate.get(0), options);
                if (StringUtils.hasText(mimeType)) {
                    return mimeType;
                }
            }
        }
        return resolveAudioMimeType(options);
    }

    private byte[] decodeBase64Audio(String value) {
        if (!StringUtils.hasText(value) || value.startsWith("http://") || value.startsWith("https://")) {
            return null;
        }
        String candidate = value.trim();
        int commaIndex = candidate.indexOf(',');
        if (candidate.startsWith("data:") && commaIndex >= 0) {
            candidate = candidate.substring(commaIndex + 1);
        }
        try {
            return Base64.getDecoder().decode(candidate);
        } catch (IllegalArgumentException ex) {
            return null;
        }
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
        return downloadBinary(uri, timeout, "image/*");
    }

    private DownloadedImage downloadBinary(URI uri, Duration timeout, String accept) {
        return downloadBinary(uri, timeout, accept, null);
    }

    private DownloadedImage downloadBinary(URI uri, Duration timeout, String accept, String bearerToken) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Accept", accept)
                .GET();
        if (StringUtils.hasText(bearerToken)) {
            requestBuilder.header("Authorization", "Bearer " + bearerToken);
        }
        try {
            HttpResponse<byte[]> response = HttpClient.newHttpClient().send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String body = new String(response.body(), StandardCharsets.UTF_8);
                throw new IllegalStateException("download failed, HTTP " + response.statusCode() + ", response: " + body);
            }
            String defaultMimeType = accept.startsWith("audio") ? "audio/mpeg" : DEFAULT_IMAGE_MIME_TYPE;
            String mimeType = response.headers().firstValue("Content-Type").orElse(defaultMimeType);
            return new DownloadedImage(response.body(), normalizeMimeType(mimeType));
        } catch (IOException ex) {
            throw new IllegalStateException("download failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("download interrupted", ex);
        }
    }

    private HttpResponse<byte[]> sendBinaryRequest(HttpRequest request, String label) {
        try {
            HttpResponse<byte[]> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String body = new String(response.body(), StandardCharsets.UTF_8);
                throw new IllegalStateException(label + " request failed, HTTP " + response.statusCode() + ", response: " + body);
            }
            return response;
        } catch (IOException ex) {
            throw new IllegalStateException(label + " request failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(label + " request interrupted", ex);
        }
    }

    private HttpResponse<String> sendTextRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("remote request failed, HTTP " + response.statusCode() + ", response: " + response.body());
            }
            return response;
        } catch (IOException ex) {
            throw new IllegalStateException("remote request failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("remote request interrupted", ex);
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

    private boolean matchesConversationKeyword(WorkbenchConversationSummaryVO summary, String normalizedKeyword) {
        return containsIgnoreCase(summary.title(), normalizedKeyword)
                || containsIgnoreCase(summary.latestMessagePreview(), normalizedKeyword);
    }

    private boolean containsIgnoreCase(String value, String normalizedKeyword) {
        return StringUtils.hasText(value) && value.toLowerCase().contains(normalizedKeyword);
    }

    private ModelServiceEntity resolvePreferredService(
            FunctionConfigEntity capability,
            List<ModelServiceEntity> enabledServices,
            String serviceCode
    ) {
        if (enabledServices.isEmpty()) {
            return null;
        }
        if (StringUtils.hasText(serviceCode)) {
            for (ModelServiceEntity service : enabledServices) {
                if (serviceCode.equals(service.getServiceCode())) {
                    return service;
                }
            }
        }
        if (StringUtils.hasText(capability.getDefaultServiceCode())) {
            for (ModelServiceEntity service : enabledServices) {
                if (capability.getDefaultServiceCode().equals(service.getServiceCode())) {
                    return service;
                }
            }
        }
        return enabledServices.get(0);
    }

    private Map<String, List<String>> parseSupportedOptions(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return Collections.emptyMap();
        }
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isObject()) {
                return Collections.emptyMap();
            }
            Map<String, List<String>> options = new LinkedHashMap<>();
            root.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                List<String> values = new ArrayList<>();
                if (value.isArray()) {
                    value.forEach(item -> values.add(item.asText()));
                } else if (!value.isNull()) {
                    values.add(value.asText());
                }
                if (!values.isEmpty()) {
                    options.put(entry.getKey(), values);
                }
            });
            return options;
        } catch (JsonProcessingException ex) {
            return Collections.emptyMap();
        }
    }

    private List<WorkbenchComposerOptionVO> resolveComposerFieldOptions(
            String capabilityCode,
            String fieldKey,
            Map<String, List<String>> supportedOptions,
            List<ParamTemplateEntity> templates
    ) {
        if ("template".equals(fieldKey) && !templates.isEmpty()) {
            return templates.stream()
                    .map(template -> WorkbenchComposerOptionVO.builder()
                            .label(template.getTemplateName())
                            .value(template.getTemplateCode())
                            .build())
                    .toList();
        }

        List<String> supportedValues = switch (fieldKey) {
            case "template" -> {
                if (CAPABILITY_TEXT_TO_PPT.equals(capabilityCode) && supportedOptions.containsKey("theme")) {
                    yield supportedOptions.get("theme");
                }
                yield supportedOptions.get(fieldKey);
            }
            default -> supportedOptions.get(fieldKey);
        };
        if (supportedValues != null && !supportedValues.isEmpty()) {
            return buildComposerOptions(supportedValues);
        }

        List<String> fallbackValues = CAPABILITY_FALLBACK_OPTIONS_MAP
                .getOrDefault(capabilityCode, Collections.emptyMap())
                .getOrDefault(fieldKey, Collections.emptyList());
        return buildComposerOptions(fallbackValues);
    }

    private List<WorkbenchComposerOptionVO> buildComposerOptions(List<String> values) {
        return values.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .map(value -> WorkbenchComposerOptionVO.builder()
                        .label(value)
                        .value(value)
                        .build())
                .toList();
    }

    private boolean supportsUpload(String capabilityCode) {
        return CAPABILITY_IMAGE_GENERATION.equals(capabilityCode)
                || CAPABILITY_IMAGE_RECOGNITION.equals(capabilityCode);
    }

    private String mimeTypeToExtension(String mimeType) {
        return switch (normalizeMimeType(mimeType)) {
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            case "image/svg+xml" -> "svg";
            default -> "png";
        };
    }

    private String mediaTypeToExtension(String mimeType, String fallback) {
        return switch (normalizeMimeType(mimeType)) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/svg+xml" -> "svg";
            case "audio/mpeg", "audio/mp3" -> "mp3";
            case "audio/wav", "audio/x-wav" -> "wav";
            case "audio/ogg", "audio/opus" -> "opus";
            case "audio/l16", "audio/pcm" -> "pcm";
            case "audio/aac" -> "aac";
            case "audio/flac" -> "flac";
            case "video/mp4" -> "mp4";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx";
            default -> fallback;
        };
    }

    private String normalizeMimeType(String mimeType) {
        return StringUtils.hasText(mimeType) ? mimeType.split(";")[0].trim() : DEFAULT_IMAGE_MIME_TYPE;
    }

    private String resolveAudioMimeType(Map<String, Object> options) {
        return switch (defaultString(readStringOption(options, "format"), "mp3").toLowerCase()) {
            case "wav" -> "audio/wav";
            case "opus" -> "audio/opus";
            case "pcm" -> "audio/pcm";
            case "aac" -> "audio/aac";
            case "flac" -> "audio/flac";
            default -> "audio/mpeg";
        };
    }

    private String resolveAudioMimeType(String mimeType, Map<String, Object> options) {
        String normalized = normalizeMimeType(mimeType);
        if (normalized.startsWith("audio/")) {
            return normalized;
        }
        return resolveAudioMimeType(options);
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

    private WorkbenchConversationSummaryVO toConversationSummaryVO(
            ChatSessionEntity entity,
            ChatMessageEntity latestMessage,
            int messageCount
    ) {
        return WorkbenchConversationSummaryVO.builder()
                .id(entity.getId())
                .conversationId(entity.getSessionId())
                .title(entity.getTitle())
                .lastCapabilityCode(entity.getLastFunctionCode())
                .lastServiceCode(entity.getLastServiceCode())
                .status(entity.getStatus())
                .latestMessagePreview(buildConversationPreview(latestMessage))
                .latestMessageType(latestMessage == null ? null : latestMessage.getMessageType())
                .latestMessageAt(latestMessage == null ? null : latestMessage.getCreatedAt())
                .messageCount(messageCount)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private WorkbenchUploadedFileVO toUploadedFileVO(FileAssetEntity entity) {
        return WorkbenchUploadedFileVO.builder()
                .fileId(entity.getFileId())
                .fileName(entity.getFileName())
                .fileType(entity.getFileType())
                .mimeType(entity.getMimeType())
                .fileSize(entity.getFileSize())
                .previewUrl(entity.getPreviewUrl())
                .downloadUrl(entity.getDownloadUrl())
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

    private String buildConversationPreview(ChatMessageEntity message) {
        if (message == null || !StringUtils.hasText(message.getContentType())) {
            return null;
        }

        if ("result".equals(message.getContentType())) {
            if (StringUtils.hasText(message.getContentText())) {
                try {
                    JsonNode resultNode = objectMapper.readTree(message.getContentText());
                    if (resultNode.hasNonNull("title")) {
                        return "\u7ed3\u679c\uff1a" + abbreviatePreview(resultNode.get("title").asText());
                    }
                } catch (JsonProcessingException ignored) {
                    // Fall back to a generic result description when the payload is not plain text.
                }
            }
            return "Task result is ready";
        }

        if ("status".equals(message.getContentType())) {
            return abbreviatePreview(message.getContentText());
        }

        return abbreviatePreview(message.getContentText());
    }

    private String abbreviatePreview(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 72) {
            return normalized;
        }
        return normalized.substring(0, 69) + "...";
    }

    private record GeneratedImagePayload(
            byte[] bytes,
            String mimeType,
            String revisedPrompt,
            String rawResponse
    ) {
    }

    private record GeneratedAudioPayload(
            byte[] bytes,
            String mimeType,
            String rawResponse
    ) {
    }

    private record GeneratedFilePayload(
            byte[] bytes,
            String mimeType,
            String sourceText
    ) {
    }

    private record GeneratedVideoPayload(
            byte[] bytes,
            String mimeType,
            String rawResponse
    ) {
    }

    private record OpenAiCompatiblePayload(
            String content,
            String rawResponse
    ) {
    }

    private record SlideDraft(
            String title,
            List<String> bullets
    ) {
    }

    private record PptTheme(
            Color primary,
            Color secondary,
            Color accent,
            Color surface,
            Color text,
            Color panel,
            Color onDark,
            Color mutedOnDark
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


