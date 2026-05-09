import type { WorkbenchFunction } from '../../types/api';

export type WorkbenchTaskStatus = 'queued' | 'running' | 'succeeded' | 'failed';

export interface WorkbenchDraftOptions {
  [key: string]: string | undefined;
  model?: string;
  referenceImage?: string;
  referenceImageFileId?: string;
  ratio?: string;
  style?: string;
  template?: string;
  duration?: string;
  recognitionMode?: string;
  voice?: string;
  format?: string;
  pages?: string;
}

export interface WorkbenchFieldOption {
  label: string;
  value: string;
}

export interface WorkbenchFieldDefinition {
  key: string;
  label: string;
  placeholder: string;
  options: WorkbenchFieldOption[];
}

export interface WorkbenchBlueprint {
  placeholder: string;
  helper: string;
  fields: WorkbenchFieldDefinition[];
}

export interface WorkbenchTaskCardData {
  taskId: string;
  capabilityCode: string;
  capabilityName: string;
  status: WorkbenchTaskStatus;
  stageText: string;
}

export interface WorkbenchResultData {
  title: string;
  summary: string;
  chips: string[];
  actionLabel: string;
  kind?: 'image' | 'audio' | 'video' | 'file' | 'generic';
  previewImageUrl?: string;
  previewVideoUrl?: string;
  downloadUrl?: string;
  fileId?: string;
  fileName?: string;
  mimeType?: string;
}

export interface WorkbenchMessageItem {
  id: string;
  role: 'user' | 'assistant' | 'system';
  type: 'text' | 'task' | 'result';
  text: string;
  createdAt: string;
  task?: WorkbenchTaskCardData;
  result?: WorkbenchResultData;
}

export interface WorkbenchFollowUpState {
  taskId: string;
  sourceLabel: string;
}

export interface WorkbenchShellSidebarState {
  currentCapability?: WorkbenchFunction;
  draftText: string;
  queuedTasks: number;
  messageCount: number;
}
