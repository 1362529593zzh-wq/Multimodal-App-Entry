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
  pptMode?: string;
}

export interface PptSlideDraft {
  id: string;
  type: string;
  layout?: string;
  visualHint?: string;
  title: string;
  subtitle?: string;
  takeaway?: string;
  bullets: string[];
  speakerNotes?: string;
}

export interface PptDeckDraft {
  draftId: string;
  title: string;
  prompt: string;
  config: {
    pages?: number;
    language?: string;
    style?: string;
    theme?: string;
    template?: string;
    ratio?: string;
  };
  slides: PptSlideDraft[];
}

export interface AiPptTheme {
  primaryColor: string;
  secondaryColor?: string;
  accentColor: string;
  backgroundColor: string;
  textColor?: string;
  fontFamily?: string;
  visualStyle?: string;
}

export interface AiPptContentBlock {
  type: 'point' | 'metric' | 'quote' | 'table' | 'timeline' | 'process' | string;
  title?: string;
  text?: string;
  value?: string;
  unit?: string;
  items?: string[];
}

export interface AiPptLayoutSpec {
  type: string;
  composition?: string;
  density?: 'low' | 'medium' | 'high' | string;
  emphasis?: string;
}

export interface AiPptVisualSpec {
  icons?: string[];
  imagePrompt?: string;
  imageFileId?: string;
  chartSpec?: unknown;
  background?: {
    type: 'solid' | 'gradient' | 'pattern' | 'image' | string;
    color?: string;
  };
}

export interface AiPptSlidePlan {
  id: string;
  page: number;
  role: string;
  type: string;
  title: string;
  subtitle?: string;
  takeaway?: string;
  content: AiPptContentBlock[];
  layout: AiPptLayoutSpec;
  visual: AiPptVisualSpec;
  speakerNotes?: string;
}

export interface AiPptDeckPlan {
  deck: {
    title: string;
    audience?: string;
    scenario?: string;
    goal?: string;
    language?: string;
    tone?: string;
    pageCount?: number;
    designDirection?: string;
    serviceCode?: string;
    modelName?: string;
  };
  theme: AiPptTheme;
  storyline: string[];
  slides: AiPptSlidePlan[];
}

export interface AiPptPreviewSlide {
  slideId: string;
  page: number;
  imageUrl: string;
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
  kind?: 'image' | 'audio' | 'video' | 'file' | 'generic' | 'ppt_draft' | 'ppt_deck';
  pptDraft?: PptDeckDraft;
  deckPlan?: AiPptDeckPlan;
  previewSlides?: AiPptPreviewSlide[];
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
