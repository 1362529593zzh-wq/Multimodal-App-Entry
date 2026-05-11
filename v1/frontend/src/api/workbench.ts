import { requestData, requestEnvelope } from './http';
import type {
  WorkbenchChatMessage,
  WorkbenchChatPayload,
  WorkbenchChatSubmitResult,
  WorkbenchComposerSchema,
  WorkbenchConversation,
  WorkbenchConversationPayload,
  WorkbenchConversationSummary,
  WorkbenchFunction,
  WorkbenchTask,
  WorkbenchUploadedFile,
} from '../types/api';
import type { AiPptDeckPlan, AiPptSlidePlan, PptDeckDraft, WorkbenchResultData } from '../pages/workbench/types';

export const fetchWorkbenchFunctions = () =>
  requestData<WorkbenchFunction[]>({
    url: '/api/workbench/functions',
    method: 'GET',
  });

export const fetchWorkbenchConversations = (scope: 'active' | 'archived' = 'active', keyword?: string) =>
  requestData<WorkbenchConversationSummary[]>({
    url: '/api/workbench/conversations',
    method: 'GET',
    params: { scope, keyword },
  });

export const fetchWorkbenchComposerSchema = (functionCode: string, serviceCode?: string) =>
  requestData<WorkbenchComposerSchema>({
    url: `/api/workbench/functions/${functionCode}/composer-schema`,
    method: 'GET',
    params: { serviceCode },
  });

export const createWorkbenchConversation = (title?: string) =>
  requestData<WorkbenchConversation>({
    url: '/api/workbench/conversations',
    method: 'POST',
    data: { title },
  });

export const updateWorkbenchConversation = (conversationId: string, payload: WorkbenchConversationPayload) =>
  requestData<WorkbenchConversation>({
    url: `/api/workbench/conversations/${conversationId}`,
    method: 'PATCH',
    data: payload,
  });

export const archiveWorkbenchConversation = (conversationId: string) =>
  requestEnvelope<null>({
    url: `/api/workbench/conversations/${conversationId}/archive`,
    method: 'PATCH',
  });

export const restoreWorkbenchConversation = (conversationId: string) =>
  requestEnvelope<null>({
    url: `/api/workbench/conversations/${conversationId}/restore`,
    method: 'PATCH',
  });

export const fetchWorkbenchConversationMessages = (conversationId: string) =>
  requestData<WorkbenchChatMessage[]>({
    url: `/api/workbench/conversations/${conversationId}/messages`,
    method: 'GET',
  });

export const submitWorkbenchChat = (payload: WorkbenchChatPayload) =>
  requestEnvelope<WorkbenchChatSubmitResult>({
    url: '/api/workbench/chat',
    method: 'POST',
    data: payload,
  });

export const uploadWorkbenchFile = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return requestData<WorkbenchUploadedFile>({
    url: '/api/workbench/files',
    method: 'POST',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const fetchWorkbenchTask = (taskId: string) =>
  requestData<WorkbenchTask>({
    url: `/api/workbench/tasks/${taskId}`,
    method: 'GET',
  });

export const generatePptFile = (taskId: string, pptDraft: PptDeckDraft) =>
  requestData<WorkbenchResultData>({
    url: `/api/workbench/tasks/${taskId}/ppt/generate-file`,
    method: 'POST',
    data: { pptDraft },
  });

export const renderAiPpt = (deckPlan: AiPptDeckPlan) =>
  requestData<WorkbenchResultData>({
    url: '/api/workbench/ppt/render',
    method: 'POST',
    data: { deckPlan },
  });

export const rewriteAiPptSlide = (deckPlan: AiPptDeckPlan, slideId: string, instruction: string) =>
  requestData<{ deckPlan: AiPptDeckPlan; slide: AiPptSlidePlan }>({
    url: `/api/workbench/ppt/slides/${slideId}/rewrite`,
    method: 'POST',
    data: { deckPlan, instruction },
  });

export const optimizeAiPptDeck = (deckPlan: AiPptDeckPlan, instruction: string) =>
  requestData<{ deckPlan: AiPptDeckPlan }>({
    url: '/api/workbench/ppt/optimize',
    method: 'POST',
    data: { deckPlan, instruction },
  });

export const changeAiPptTemplate = (deckPlan: AiPptDeckPlan, templateCode: string) =>
  requestData<{ deckPlan: AiPptDeckPlan }>({
    url: '/api/workbench/ppt/change-template',
    method: 'POST',
    data: { deckPlan, templateCode },
  });
