import { useEffect, useMemo, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Card, Tag, message } from 'antd';
import { fetchServiceDictionary } from '../../api/config';
import {
  createWorkbenchConversation,
  fetchWorkbenchConversationMessages,
  fetchWorkbenchFunctions,
  fetchWorkbenchTask,
  submitWorkbenchChat,
} from '../../api/workbench';
import { Composer } from '../../components/workbench/composer';
import { MessageStream } from '../../components/workbench/message-stream';
import { WorkbenchShell } from '../../components/workbench/workbench-shell';
import type { WorkbenchFunction } from '../../types/api';
import { fallbackWorkbenchFunctions, getWorkbenchBlueprint } from './mock';
import type {
  WorkbenchDraftOptions,
  WorkbenchFollowUpState,
  WorkbenchMessageItem,
  WorkbenchResultData,
  WorkbenchShellSidebarState,
  WorkbenchTaskCardData,
} from './types';

const createLocalId = (prefix: string) => `${prefix}_${Math.random().toString(36).slice(2, 10)}`;

const buildTaskStageText = (status: string) => {
  switch (status) {
    case 'PENDING':
    case 'CREATED':
      return '任务已进入队列，正在等待工作台主链路继续推进。';
    case 'RUNNING':
      return '任务正在执行中，后端正在调用模型服务并准备结果文件。';
    case 'SUCCESS':
      return '任务已成功完成，结果图片与文件资产已回灌到当前工作台。';
    case 'FAILED':
      return '任务执行失败，请检查模型服务配置或稍后重试。';
    default:
      return '任务状态未知，请检查服务端返回。';
  }
};

const parseWorkbenchResult = (contentText: string | null, relatedTaskId: string | null): WorkbenchResultData => {
  const fallback: WorkbenchResultData = {
    title: '任务结果 / 后端回流',
    summary: contentText ?? '任务已完成。',
    chips: relatedTaskId ? [`task: ${relatedTaskId}`] : ['status: success'],
    actionLabel: '基于该结果继续',
    kind: 'generic',
  };

  if (!contentText) {
    return fallback;
  }

  try {
    const parsed = JSON.parse(contentText) as Partial<WorkbenchResultData>;
    const chips = Array.from(
      new Set([...(parsed.chips ?? []), ...(relatedTaskId ? [`task: ${relatedTaskId}`] : ['status: success'])]),
    );
    return {
      title: parsed.title ?? fallback.title,
      summary: parsed.summary ?? fallback.summary,
      chips,
      actionLabel: parsed.actionLabel ?? fallback.actionLabel,
      kind: parsed.kind ?? (parsed.previewImageUrl ? 'image' : 'generic'),
      previewImageUrl: parsed.previewImageUrl,
      downloadUrl: parsed.downloadUrl,
      fileId: parsed.fileId,
    };
  } catch {
    return fallback;
  }
};

export const WorkbenchPage = () => {
  const queryClient = useQueryClient();
  const [conversationId, setConversationId] = useState<string>();
  const [selectedCapability, setSelectedCapability] = useState('image_generation');
  const [draftTexts, setDraftTexts] = useState<Record<string, string>>({});
  const [draftOptions, setDraftOptions] = useState<Record<string, WorkbenchDraftOptions>>({});
  const [followUp, setFollowUp] = useState<WorkbenchFollowUpState | null>(null);
  const [activeTaskMeta, setActiveTaskMeta] = useState<{ taskId: string; capabilityCode: string; capabilityName: string } | null>(null);
  const notifiedTaskRef = useRef<string | null>(null);

  const capabilityQuery = useQuery({
    queryKey: ['workbench', 'functions'],
    queryFn: fetchWorkbenchFunctions,
    staleTime: 60_000,
  });

  const serviceQuery = useQuery({
    queryKey: ['workbench', 'services'],
    queryFn: fetchServiceDictionary,
    staleTime: 60_000,
  });

  const messagesQuery = useQuery({
    queryKey: ['workbench', 'messages', conversationId],
    queryFn: () => fetchWorkbenchConversationMessages(conversationId!),
    enabled: Boolean(conversationId),
    staleTime: 0,
  });

  const taskQuery = useQuery({
    queryKey: ['workbench', 'task', activeTaskMeta?.taskId],
    queryFn: () => fetchWorkbenchTask(activeTaskMeta!.taskId),
    enabled: Boolean(activeTaskMeta?.taskId),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === 'SUCCESS' || status === 'FAILED' ? false : 1200;
    },
  });

  const createConversationMutation = useMutation({
    mutationFn: (title?: string) => createWorkbenchConversation(title),
  });

  const submitChatMutation = useMutation({
    mutationFn: submitWorkbenchChat,
  });

  const capabilities = useMemo<WorkbenchFunction[]>(() => {
    const records = capabilityQuery.data?.length ? capabilityQuery.data : fallbackWorkbenchFunctions;
    return [...records].sort((left, right) => left.sortOrder - right.sortOrder);
  }, [capabilityQuery.data]);

  useEffect(() => {
    if (capabilities.length > 0 && !capabilities.some((item) => item.functionCode === selectedCapability)) {
      setSelectedCapability(capabilities[0].functionCode);
    }
  }, [capabilities, selectedCapability]);

  const currentCapability = useMemo(
    () => capabilities.find((item) => item.functionCode === selectedCapability) ?? capabilities[0],
    [capabilities, selectedCapability],
  );

  const modelOptions = useMemo(() => {
    if (!currentCapability) {
      return [];
    }

    return (serviceQuery.data ?? [])
      .filter((item) => item.functionCode === currentCapability.functionCode && item.enabled)
      .map((item) => ({
        label: `${item.serviceName} / ${item.serviceCode}`,
        value: item.serviceCode,
      }));
  }, [currentCapability, serviceQuery.data]);

  const blueprint = useMemo(
    () => getWorkbenchBlueprint(currentCapability?.functionCode ?? 'image_generation', modelOptions),
    [currentCapability?.functionCode, modelOptions],
  );

  const currentDraftText = draftTexts[selectedCapability] ?? '';
  const currentDraftOptions = draftOptions[selectedCapability] ?? {};

  useEffect(() => {
    const status = taskQuery.data?.status;
    const taskId = taskQuery.data?.taskId;
    if (!taskId || notifiedTaskRef.current === taskId || (status !== 'SUCCESS' && status !== 'FAILED')) {
      return;
    }

    notifiedTaskRef.current = taskId;
    void queryClient.invalidateQueries({ queryKey: ['workbench', 'messages', conversationId] });

    if (status === 'SUCCESS') {
      void message.success('任务已完成，结果已从后端消息流回灌。');
      setTimeout(() => setActiveTaskMeta(null), 600);
      return;
    }

    void message.error('任务执行失败，请检查参数后重试。');
  }, [conversationId, queryClient, taskQuery.data?.status, taskQuery.data?.taskId]);

  const activeTaskCard = useMemo<WorkbenchTaskCardData | null>(() => {
    if (!activeTaskMeta) {
      return null;
    }

    const status = taskQuery.data?.status ?? 'PENDING';
    return {
      taskId: activeTaskMeta.taskId,
      capabilityCode: activeTaskMeta.capabilityCode,
      capabilityName: activeTaskMeta.capabilityName,
      status:
        status === 'SUCCESS'
          ? 'succeeded'
          : status === 'FAILED'
            ? 'failed'
            : status === 'RUNNING'
              ? 'running'
              : 'queued',
      stageText: buildTaskStageText(status),
    };
  }, [activeTaskMeta, taskQuery.data?.status]);

  const displayMessages = useMemo<WorkbenchMessageItem[]>(() => {
    const records = (messagesQuery.data ?? []).flatMap<WorkbenchMessageItem>((item) => {
      if (activeTaskMeta?.taskId && item.contentType === 'status' && item.relatedTaskId === activeTaskMeta.taskId) {
        return [];
      }

      if (item.messageType === 'assistant' && item.contentType === 'result') {
        return [
          {
            id: item.messageId,
            role: 'assistant',
            type: 'result',
            text: '',
            createdAt: item.createdAt,
            result: parseWorkbenchResult(item.contentText, item.relatedTaskId),
          },
        ];
      }

      return [
        {
          id: item.messageId,
          role: item.messageType === 'user' ? 'user' : item.messageType === 'assistant' ? 'assistant' : 'system',
          type: 'text',
          text: item.contentText ?? '',
          createdAt: item.createdAt,
        },
      ];
    });

    if (activeTaskCard) {
      records.push({
        id: `task_${activeTaskCard.taskId}`,
        role: 'system',
        type: 'task',
        text: '',
        createdAt: new Date().toISOString(),
        task: activeTaskCard,
      });
    }

    return records;
  }, [activeTaskCard, activeTaskMeta?.taskId, messagesQuery.data]);

  const sidebarState: WorkbenchShellSidebarState = {
    currentCapability,
    draftText: currentDraftText,
    queuedTasks: activeTaskCard && activeTaskCard.status !== 'succeeded' ? 1 : 0,
    messageCount: displayMessages.length,
  };

  const handleSend = async () => {
    const prompt = currentDraftText.trim();
    if (!prompt || !currentCapability || submitChatMutation.isPending || createConversationMutation.isPending) {
      return;
    }

    try {
      let nextConversationId = conversationId;
      if (!nextConversationId) {
        const created = await createConversationMutation.mutateAsync(`${currentCapability.functionName} 会话`);
        nextConversationId = created.conversationId;
        setConversationId(nextConversationId);
      }

      const envelope = await submitChatMutation.mutateAsync({
        conversationId: nextConversationId,
        selectionMode: 'manual',
        capability: currentCapability.functionCode,
        inputText: prompt,
        model: currentDraftOptions.model,
        options: JSON.stringify(currentDraftOptions),
        inheritanceMode: followUp ? 'same_capability' : 'none',
        parentTaskId: followUp?.taskId,
        sourceAssetIds: '[]',
      });

      setActiveTaskMeta({
        taskId: envelope.data.taskId,
        capabilityCode: envelope.data.resolvedCapability,
        capabilityName: currentCapability.functionName,
      });
      notifiedTaskRef.current = null;
      setDraftTexts((previous) => ({ ...previous, [selectedCapability]: '' }));
      setFollowUp(null);
      await queryClient.invalidateQueries({ queryKey: ['workbench', 'messages', nextConversationId] });
    } catch {
      // http.ts 已统一提示错误，这里不重复弹出。
    }
  };

  const sidebar = (
    <div className="workbench-sidebar">
      <Card className="surface-card workbench-sidebar__panel">
        <Tag color="cyan">Step 4</Tag>
        <h3>工作台基础壳子</h3>
        <p>当前已切到真实会话、消息、任务接口，文生图结果会直接以图片卡和文件资产的形式回灌到工作台。</p>
      </Card>
      <Card className="surface-card workbench-sidebar__panel">
        <strong>当前能力</strong>
        <h4>{sidebarState.currentCapability?.functionName ?? '未选择能力'}</h4>
        <p>{sidebarState.currentCapability?.description ?? '先选择一个能力，观察参数区和消息流如何联动。'}</p>
        <div className="workbench-sidebar__meta">
          <span>会话 ID：{conversationId ?? '未创建'}</span>
          <span>消息条数：{sidebarState.messageCount}</span>
          <span>待完成任务：{sidebarState.queuedTasks}</span>
        </div>
      </Card>
      <Card className="surface-card workbench-sidebar__panel">
        <strong>第 5 步衔接</strong>
        <ul className="workbench-sidebar__list">
          <li>文生图已优先接入真实执行链路。</li>
          <li>下一步把图像识别、语音、PPT、视频能力迁移到同一套执行器。</li>
          <li>再补会话历史、文件管理和调用记录中心。</li>
        </ul>
      </Card>
    </div>
  );

  return (
    <div className="page-stack">
      <WorkbenchShell sidebar={sidebar}>
        <MessageStream
          messages={displayMessages}
          capabilities={capabilities}
          onSelectCapability={setSelectedCapability}
          onFollowUp={(messageItem) => {
            if (!messageItem.result) {
              return;
            }

            const taskId = messageItem.result.chips.find((chip) => chip.startsWith('task: '))?.replace('task: ', '');
            setFollowUp({
              taskId: taskId ?? createLocalId('ref'),
              sourceLabel: messageItem.result.title,
            });
          }}
        />
        <Composer
          capabilities={capabilities}
          selectedCapability={selectedCapability}
          blueprint={blueprint}
          draftText={currentDraftText}
          options={currentDraftOptions}
          followUp={followUp}
          sending={submitChatMutation.isPending || createConversationMutation.isPending}
          onSelectCapability={setSelectedCapability}
          onDraftTextChange={(value) => setDraftTexts((previous) => ({ ...previous, [selectedCapability]: value }))}
          onOptionChange={(key, value) =>
            setDraftOptions((previous) => ({
              ...previous,
              [selectedCapability]: {
                ...(previous[selectedCapability] ?? {}),
                [key]: value,
              },
            }))
          }
          onClearDraft={() => {
            setDraftTexts((previous) => ({ ...previous, [selectedCapability]: '' }));
            setDraftOptions((previous) => ({ ...previous, [selectedCapability]: {} }));
          }}
          onClearFollowUp={() => setFollowUp(null)}
          onSend={() => {
            void handleSend();
          }}
        />
      </WorkbenchShell>
    </div>
  );
};
