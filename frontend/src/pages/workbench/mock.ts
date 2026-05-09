import type { WorkbenchFunction } from '../../types/api';
import type {
  WorkbenchBlueprint,
  WorkbenchDraftOptions,
  WorkbenchResultData,
  WorkbenchTaskCardData,
  WorkbenchTaskStatus,
} from './types';

export const fallbackWorkbenchFunctions: WorkbenchFunction[] = [
  {
    id: 1,
    functionCode: 'image_generation',
    functionName: '图像生成',
    icon: 'image-generation',
    sortOrder: 1,
    allowManualModelSelect: true,
    showInMainBar: true,
    showInMoreMenu: false,
    defaultServiceCode: 'img-gen-default',
    defaultServiceName: 'Image Generation Default',
    description: '文生图、风格化与结果追问的主入口。',
  },
  {
    id: 2,
    functionCode: 'image_recognition',
    functionName: '图像识别',
    icon: 'image-recognition',
    sortOrder: 2,
    allowManualModelSelect: true,
    showInMainBar: true,
    showInMoreMenu: false,
    defaultServiceCode: 'img-rec-default',
    defaultServiceName: 'Vision Default',
    description: '图片理解、OCR 和细节识别。',
  },
  {
    id: 3,
    functionCode: 'text_to_speech',
    functionName: '文生语音',
    icon: 'text-to-speech',
    sortOrder: 3,
    allowManualModelSelect: true,
    showInMainBar: true,
    showInMoreMenu: false,
    defaultServiceCode: 'tts-default',
    defaultServiceName: 'TTS Default',
    description: '文本转语音与声线选择。',
  },
  {
    id: 4,
    functionCode: 'text_to_ppt',
    functionName: '文生 PPT',
    icon: 'text-to-ppt',
    sortOrder: 4,
    allowManualModelSelect: true,
    showInMainBar: true,
    showInMoreMenu: false,
    defaultServiceCode: 'ppt-default',
    defaultServiceName: 'PPT Default',
    description: '主题、页数与模板组合的快速出稿入口。',
  },
  {
    id: 5,
    functionCode: 'text_to_video',
    functionName: '视频生成',
    icon: 'text-to-video',
    sortOrder: 5,
    allowManualModelSelect: true,
    showInMainBar: true,
    showInMoreMenu: true,
    defaultServiceCode: 'video-default',
    defaultServiceName: 'Video Default',
    description: '长任务能力，适合检验任务状态卡演化。',
  },
];

const fieldOptions = (values: string[]) => values.map((value) => ({ label: value, value }));

const blueprints: Record<string, WorkbenchBlueprint> = {
  image_generation: {
    placeholder: '描述你想要的图片',
    helper: '第 4 步先验证功能条和参数快捷区的交互，第 5 步再接任务编排与真实模型。',
    fields: [
      { key: 'ratio', label: '画幅比例', placeholder: '选择比例', options: fieldOptions(['1:1', '4:3', '16:9']) },
      { key: 'style', label: '风格', placeholder: '选择风格', options: fieldOptions(['realistic', 'illustration', 'anime']) },
      { key: 'template', label: '模板', placeholder: '选择模板', options: fieldOptions(['poster', 'product', 'landscape']) },
    ],
  },
  image_recognition: {
    placeholder: '输入你要分析的图片说明，后续会接入上传与引用资产。',
    helper: '这一版先用消息流 + 任务状态卡承接识别任务。',
    fields: [
      {
        key: 'recognitionMode',
        label: '识别模式',
        placeholder: '选择识别模式',
        options: fieldOptions(['general', 'ocr', 'detail']),
      },
    ],
  },
  text_to_speech: {
    placeholder: '输入要合成语音的文本',
    helper: '骨架阶段先验证参数切换与消息卡结构。',
    fields: [
      { key: 'voice', label: '音色', placeholder: '选择音色', options: fieldOptions(['alloy', 'nova', 'shimmer']) },
      { key: 'format', label: '格式', placeholder: '选择格式', options: fieldOptions(['mp3', 'pcm']) },
    ],
  },
  text_to_ppt: {
    placeholder: '输入 PPT 主题和提纲',
    helper: '先把页数、模板风格等快捷项贴着输入区展开。',
    fields: [
      { key: 'template', label: '模板风格', placeholder: '选择风格', options: fieldOptions(['business', 'tech', 'minimal']) },
      { key: 'pages', label: '页数', placeholder: '选择页数', options: fieldOptions(['8', '10', '12']) },
    ],
  },
  text_to_video: {
    placeholder: '描述你想要的视频',
    helper: '视频能力天然适合后续接异步任务链路，所以这一步先用 mock 演化状态卡。',
    fields: [
      { key: 'duration', label: '时长', placeholder: '选择时长', options: fieldOptions(['5s', '10s', '30s']) },
      { key: 'ratio', label: '画幅比例', placeholder: '选择比例', options: fieldOptions(['16:9', '9:16']) },
      { key: 'style', label: '风格', placeholder: '选择风格', options: fieldOptions(['cinematic', 'anime', 'documentary']) },
    ],
  },
};

export const getWorkbenchBlueprint = (
  capabilityCode: string,
  modelOptions: { label: string; value: string }[],
): WorkbenchBlueprint => {
  const base = blueprints[capabilityCode] ?? blueprints.image_generation;
  const fields = [...base.fields];

  if (modelOptions.length > 0) {
    fields.unshift({
      key: 'model',
      label: '模型服务',
      placeholder: '选择模型服务',
      options: modelOptions,
    });
  }

  return {
    helper: base.helper,
    placeholder: base.placeholder,
    fields,
  };
};

export const buildMockTask = (taskId: string, capabilityCode: string, capabilityName: string): WorkbenchTaskCardData => ({
  taskId,
  capabilityCode,
  capabilityName,
  status: 'queued',
  stageText: '任务已进入队列，准备生成执行快照。',
});

export const evolveTask = (task: WorkbenchTaskCardData, status: WorkbenchTaskStatus): WorkbenchTaskCardData => {
  const stageTextMap: Record<WorkbenchTaskStatus, string> = {
    queued: '任务已进入队列，准备生成执行快照。',
    running: '正在根据当前能力、模型与快捷参数生成任务输出。',
    succeeded: '任务已完成，结果已回流到消息流。',
    failed: '任务执行失败，请检查参数或稍后重试。',
  };

  return {
    ...task,
    status,
    stageText: stageTextMap[status],
  };
};

export const buildMockResult = (
  capabilityName: string,
  prompt: string,
  options: WorkbenchDraftOptions,
): WorkbenchResultData => {
  const chips = Object.entries(options)
    .filter(([, value]) => Boolean(value))
    .map(([key, value]) => `${key}: ${value}`);

  return {
    title: `${capabilityName} / 结果占位卡`,
    summary: `已按当前草稿生成 mock 结果。原始输入为“${prompt.slice(0, 48)}${prompt.length > 48 ? '...' : ''}”。下一步第 5 步会把这里替换成真实任务返回。`,
    chips: chips.length > 0 ? chips : ['status: mock'],
    actionLabel: '基于该结果继续',
  };
};
