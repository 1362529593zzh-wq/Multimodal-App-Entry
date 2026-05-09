export interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResponse<T> {
  pageNum: number;
  pageSize: number;
  total: number;
  records: T[];
}

export interface FunctionConfig {
  id: number;
  functionCode: string;
  functionName: string;
  icon: string | null;
  sortOrder: number;
  enabled: boolean;
  isDefault: boolean;
  defaultServiceCode: string | null;
  allowManualModelSelect: boolean;
  showInMainBar: boolean;
  showInMoreMenu: boolean;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ModelService {
  id: number;
  serviceCode: string;
  serviceName: string;
  modelCode: string;
  modelName: string;
  modelType: string;
  functionCode: string;
  vendorCode: string;
  vendorName: string;
  vendorType: string;
  providerType: string;
  endpoint: string | null;
  authType: string | null;
  secretRef: string | null;
  apiKeyMasked: string | null;
  secretKeyMasked: string | null;
  requestMethod: string;
  headerTemplate: string;
  payloadTemplate: string;
  extraConfig: string;
  timeoutMs: number;
  enabled: boolean;
  publishStatus: string;
  isDefault: boolean;
  allowFrontSelect: boolean;
  supportedOptions: string;
  sortOrder: number;
  remark: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface FunctionModelBinding {
  id: number;
  functionCode: string;
  serviceCode: string;
  isDefault: boolean;
  sortOrder: number;
  enabled: boolean;
  remark: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ParamTemplate {
  id: number;
  templateCode: string;
  templateName: string;
  functionCode: string;
  serviceCode: string | null;
  templateType: string | null;
  templatePayload: string;
  presetParams: string;
  description: string | null;
  sortOrder: number;
  enabled: boolean;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CallRecord {
  id: number;
  recordId: string;
  taskId: string;
  conversationId: string | null;
  messageId: string | null;
  parentTaskId: string | null;
  sourceAssetIds: string;
  functionCode: string;
  functionName: string | null;
  selectionMode: string;
  resolvedIntent: string | null;
  serviceCode: string;
  serviceName: string | null;
  modelName: string | null;
  requestSummary: string | null;
  inputText: string | null;
  requestParams: string;
  inputAssets: string;
  status: string;
  resultType: string | null;
  resultSummary: string;
  errorMessage: string | null;
  downloadCount: number;
  recordStatus: string;
  remark: string | null;
  tags: string;
  createdAt: string;
  startedAt: string | null;
  finishedAt: string | null;
  durationMs: number | null;
}

export interface FunctionConfigQuery {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  enabled?: boolean;
}

export interface ModelServiceQuery {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  functionCode?: string;
  vendorCode?: string;
  providerType?: string;
  modelType?: string;
  enabled?: boolean;
}

export interface FunctionModelBindingQuery {
  pageNum: number;
  pageSize: number;
  functionCode?: string;
  serviceCode?: string;
  enabled?: boolean;
}

export interface ParamTemplateQuery {
  pageNum: number;
  pageSize: number;
  functionCode?: string;
  serviceCode?: string;
  templateType?: string;
  enabled?: boolean;
}

export interface CallRecordQuery {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  functionCode?: string;
  serviceCode?: string;
  status?: string;
  resultType?: string;
  startedFrom?: string;
  startedTo?: string;
}

export interface FunctionConfigPayload {
  functionCode?: string;
  functionName: string;
  icon?: string;
  sortOrder?: number;
  enabled?: boolean;
  isDefault?: boolean;
  defaultServiceCode?: string;
  allowManualModelSelect?: boolean;
  showInMainBar?: boolean;
  showInMoreMenu?: boolean;
  description?: string;
}

export interface ModelServicePayload {
  serviceCode?: string;
  serviceName: string;
  modelCode: string;
  modelName: string;
  modelType: string;
  functionCode: string;
  vendorCode?: string;
  vendorName?: string;
  vendorType?: string;
  providerType?: string;
  endpoint?: string;
  authType?: string;
  secretRef?: string;
  apiKey?: string;
  secretKey?: string;
  requestMethod?: string;
  headerTemplate?: string;
  payloadTemplate?: string;
  extraConfig?: string;
  timeoutMs?: number;
  enabled?: boolean;
  publishStatus?: string;
  isDefault?: boolean;
  allowFrontSelect?: boolean;
  supportedOptions?: string;
  sortOrder?: number;
  remark?: string;
}

export interface ModelServiceVendor {
  vendorCode: string;
  vendorName: string;
  vendorType: string;
  total: number;
  enabledCount: number;
  sortOrder: number;
}

export interface FunctionModelBindingPayload {
  functionCode?: string;
  serviceCode?: string;
  isDefault?: boolean;
  sortOrder?: number;
  enabled?: boolean;
  remark?: string;
}

export interface ParamTemplatePayload {
  templateCode?: string;
  templateName: string;
  functionCode: string;
  serviceCode?: string;
  templateType?: string;
  templatePayload: string;
  presetParams: string;
  description?: string;
  sortOrder?: number;
  enabled?: boolean;
  isDefault?: boolean;
}

export interface SelectOptionItem {
  label: string;
  value: string;
  meta?: string;
}

export interface WorkbenchFunction {
  id: number;
  functionCode: string;
  functionName: string;
  icon: string | null;
  sortOrder: number;
  allowManualModelSelect: boolean;
  showInMainBar: boolean;
  showInMoreMenu: boolean;
  defaultServiceCode: string | null;
  defaultServiceName: string | null;
  description: string | null;
}

export interface WorkbenchComposerOption {
  label: string;
  value: string;
}

export interface WorkbenchComposerField {
  key: string;
  label: string;
  placeholder: string;
  options: WorkbenchComposerOption[];
}

export interface WorkbenchComposerSchema {
  capabilityCode: string;
  placeholder: string;
  helper: string;
  supportsUpload: boolean;
  fields: WorkbenchComposerField[];
}

export interface WorkbenchConversation {
  id: number;
  conversationId: string;
  title: string;
  lastCapabilityCode: string | null;
  lastServiceCode: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface WorkbenchConversationPayload {
  title: string;
}

export interface WorkbenchConversationSummary {
  id: number;
  conversationId: string;
  title: string;
  lastCapabilityCode: string | null;
  lastServiceCode: string | null;
  status: string;
  latestMessagePreview: string | null;
  latestMessageType: string | null;
  latestMessageAt: string | null;
  messageCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface WorkbenchChatMessage {
  id: number;
  messageId: string;
  conversationId: string;
  messageType: string;
  contentType: string;
  contentText: string | null;
  relatedTaskId: string | null;
  relatedRecordId: string | null;
  sequenceNo: number;
  createdAt: string;
}

export interface WorkbenchTask {
  id: number;
  taskId: string;
  conversationId: string | null;
  messageId: string | null;
  parentTaskId: string | null;
  capabilityCode: string;
  selectionMode: string;
  resolvedIntent: string | null;
  serviceCode: string;
  modelName: string | null;
  inputText: string | null;
  requestParams: string;
  inputAssetIds: string;
  inheritanceMode: string | null;
  sourceAssetIds: string;
  status: string;
  resultType: string | null;
  errorMessage: string | null;
  createdAt: string;
  startedAt: string | null;
  finishedAt: string | null;
  durationMs: number | null;
}

export interface WorkbenchChatPayload {
  conversationId?: string;
  selectionMode?: string;
  capability?: string;
  inputText: string;
  serviceCode?: string;
  model?: string;
  options?: string;
  composerOptions?: string;
  schemaVersion?: string;
  templateCode?: string;
  attachments?: string;
  inheritanceMode?: string;
  parentTaskId?: string;
  sourceAssetIds?: string;
}

  export interface WorkbenchChatSubmitResult {
    conversationId: string;
    userMessageId: string;
  taskMessageId: string;
  taskId: string;
  resolvedCapability: string;
  resolvedServiceCode: string;
  resolvedModel: string | null;
    status: string;
  }

  export interface WorkbenchUploadedFile {
    fileId: string;
    fileName: string;
    fileType: string;
    mimeType: string;
    fileSize: number;
    previewUrl?: string;
    downloadUrl?: string;
  }
