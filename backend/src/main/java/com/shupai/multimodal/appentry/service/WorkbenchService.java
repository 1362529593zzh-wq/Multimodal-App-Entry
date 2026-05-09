package com.shupai.multimodal.appentry.service;

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
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface WorkbenchService {

    List<WorkbenchFunctionVO> listFunctions();

    List<WorkbenchConversationSummaryVO> listConversations(String scope, String keyword);

    WorkbenchComposerSchemaVO getComposerSchema(String functionCode, String serviceCode);

    ConversationVO createConversation(ConversationCreateRequest request);

    ConversationVO updateConversation(String conversationId, ConversationUpdateRequest request);

    void archiveConversation(String conversationId);

    void restoreConversation(String conversationId);

    ConversationVO getConversation(String conversationId);

    List<ChatMessageVO> listMessages(String conversationId);

    WorkbenchChatSubmitVO submitChat(WorkbenchChatRequest request);

    WorkbenchUploadedFileVO uploadFile(MultipartFile file);

    TaskVO getTask(String taskId);

    WorkbenchFileResource previewFile(String fileId);

    WorkbenchFileResource downloadFile(String fileId);
}
