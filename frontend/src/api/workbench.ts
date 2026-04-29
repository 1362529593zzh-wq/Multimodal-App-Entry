import { requestData, requestEnvelope } from './http';
import type {
  WorkbenchChatMessage,
  WorkbenchChatPayload,
  WorkbenchChatSubmitResult,
  WorkbenchConversation,
  WorkbenchFunction,
  WorkbenchTask,
} from '../types/api';

export const fetchWorkbenchFunctions = () =>
  requestData<WorkbenchFunction[]>({
    url: '/api/workbench/functions',
    method: 'GET',
  });

export const createWorkbenchConversation = (title?: string) =>
  requestData<WorkbenchConversation>({
    url: '/api/workbench/conversations',
    method: 'POST',
    data: { title },
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

export const fetchWorkbenchTask = (taskId: string) =>
  requestData<WorkbenchTask>({
    url: `/api/workbench/tasks/${taskId}`,
    method: 'GET',
  });
