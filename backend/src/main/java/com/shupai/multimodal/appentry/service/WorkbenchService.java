package com.shupai.multimodal.appentry.service;

import com.shupai.multimodal.appentry.model.dto.ConversationCreateRequest;
import com.shupai.multimodal.appentry.model.dto.WorkbenchChatRequest;
import com.shupai.multimodal.appentry.model.vo.ChatMessageVO;
import com.shupai.multimodal.appentry.model.vo.ConversationVO;
import com.shupai.multimodal.appentry.model.vo.TaskVO;
import com.shupai.multimodal.appentry.model.vo.WorkbenchChatSubmitVO;
import com.shupai.multimodal.appentry.model.vo.WorkbenchFileResource;
import com.shupai.multimodal.appentry.model.vo.WorkbenchFunctionVO;
import java.util.List;

public interface WorkbenchService {

    List<WorkbenchFunctionVO> listFunctions();

    ConversationVO createConversation(ConversationCreateRequest request);

    ConversationVO getConversation(String conversationId);

    List<ChatMessageVO> listMessages(String conversationId);

    WorkbenchChatSubmitVO submitChat(WorkbenchChatRequest request);

    TaskVO getTask(String taskId);

    WorkbenchFileResource previewFile(String fileId);

    WorkbenchFileResource downloadFile(String fileId);
}
