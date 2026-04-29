package com.shupai.multimodal.appentry.web;

import com.shupai.multimodal.appentry.model.dto.ConversationCreateRequest;
import com.shupai.multimodal.appentry.model.dto.WorkbenchChatRequest;
import com.shupai.multimodal.appentry.model.vo.ChatMessageVO;
import com.shupai.multimodal.appentry.model.vo.ConversationVO;
import com.shupai.multimodal.appentry.model.vo.TaskVO;
import com.shupai.multimodal.appentry.model.vo.WorkbenchChatSubmitVO;
import com.shupai.multimodal.appentry.model.vo.WorkbenchFileResource;
import com.shupai.multimodal.appentry.model.vo.WorkbenchFunctionVO;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workbench")
public class WorkbenchController {

    private final WorkbenchService workbenchService;

    @GetMapping("/functions")
    public List<WorkbenchFunctionVO> listFunctions() {
        return workbenchService.listFunctions();
    }

    @PostMapping("/conversations")
    public ConversationVO createConversation(@Valid @RequestBody ConversationCreateRequest request) {
        return workbenchService.createConversation(request);
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
