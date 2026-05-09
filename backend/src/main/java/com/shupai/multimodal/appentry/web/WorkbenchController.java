package com.shupai.multimodal.appentry.web;

import com.shupai.multimodal.appentry.model.dto.ConversationCreateRequest;
import com.shupai.multimodal.appentry.model.dto.ConversationUpdateRequest;
import com.shupai.multimodal.appentry.model.dto.WorkbenchChatRequest;
import com.shupai.multimodal.appentry.model.vo.ChatMessageVO;
import com.shupai.multimodal.appentry.model.vo.ConversationVO;
import com.shupai.multimodal.appentry.model.vo.TaskVO;
import com.shupai.multimodal.appentry.model.vo.WorkbenchChatSubmitVO;
import com.shupai.multimodal.appentry.model.vo.WorkbenchComposerSchemaVO;
import com.shupai.multimodal.appentry.model.vo.WorkbenchConversationSummaryVO;
import com.shupai.multimodal.appentry.model.vo.WorkbenchFileResource;
import com.shupai.multimodal.appentry.model.vo.WorkbenchFunctionVO;
import com.shupai.multimodal.appentry.model.vo.WorkbenchUploadedFileVO;
import com.shupai.multimodal.appentry.service.WorkbenchService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workbench")
public class WorkbenchController {

    private final WorkbenchService workbenchService;

    @GetMapping("/functions")
    public List<WorkbenchFunctionVO> listFunctions() {
        return workbenchService.listFunctions();
    }

    @GetMapping("/conversations")
    public List<WorkbenchConversationSummaryVO> listConversations(
            @RequestParam(defaultValue = "active") String scope,
            @RequestParam(required = false) String keyword
    ) {
        return workbenchService.listConversations(scope, keyword);
    }

    @GetMapping("/functions/{functionCode}/composer-schema")
    public WorkbenchComposerSchemaVO getComposerSchema(
            @PathVariable String functionCode,
            @RequestParam(required = false) String serviceCode
    ) {
        return workbenchService.getComposerSchema(functionCode, serviceCode);
    }

    @PostMapping("/conversations")
    public ConversationVO createConversation(@Valid @RequestBody ConversationCreateRequest request) {
        return workbenchService.createConversation(request);
    }

    @PatchMapping("/conversations/{conversationId}")
    public ConversationVO updateConversation(
            @PathVariable String conversationId,
            @Valid @RequestBody ConversationUpdateRequest request
    ) {
        return workbenchService.updateConversation(conversationId, request);
    }

    @PatchMapping("/conversations/{conversationId}/archive")
    public void archiveConversation(@PathVariable String conversationId) {
        workbenchService.archiveConversation(conversationId);
    }

    @PatchMapping("/conversations/{conversationId}/restore")
    public void restoreConversation(@PathVariable String conversationId) {
        workbenchService.restoreConversation(conversationId);
    }

    @GetMapping("/conversations/{conversationId}")
    public ConversationVO getConversation(@PathVariable String conversationId) {
        return workbenchService.getConversation(conversationId);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<ChatMessageVO> listMessages(@PathVariable String conversationId) {
        return workbenchService.listMessages(conversationId);
    }

    @PostMapping("/chat")
    public WorkbenchChatSubmitVO submitChat(@Valid @RequestBody WorkbenchChatRequest request) {
        return workbenchService.submitChat(request);
    }

    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public WorkbenchUploadedFileVO uploadFile(@RequestParam("file") MultipartFile file) {
        return workbenchService.uploadFile(file);
    }

    @GetMapping("/tasks/{taskId}")
    public TaskVO getTask(@PathVariable String taskId) {
        return workbenchService.getTask(taskId);
    }

    @GetMapping("/files/{fileId}/preview")
    public ResponseEntity<Resource> previewFile(@PathVariable String fileId) {
        WorkbenchFileResource file = workbenchService.previewFile(fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(file.fileName()).build().toString())
                .body(file.resource());
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        WorkbenchFileResource file = workbenchService.downloadFile(fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .body(file.resource());
    }
}
