import { useEffect, useMemo, useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Card, Input, Modal, message } from 'antd';
import {
  archiveWorkbenchConversation,
  createWorkbenchConversation,
  fetchWorkbenchComposerSchema,
  fetchWorkbenchConversations,
  fetchWorkbenchConversationMessages,
  fetchWorkbenchFunctions,
  fetchWorkbenchTask,
  restoreWorkbenchConversation,
  submitWorkbenchChat,
  updateWorkbenchConversation,
  uploadWorkbenchFile,
} from '../../api/workbench';
import { Composer } from '../../components/workbench/composer';
import { ConversationHistory, type ConversationScope } from '../../components/workbench/conversation-history';
import { MessageStream } from '../../components/workbench/message-stream';
import { WorkbenchShell } from '../../components/workbench/workbench-shell';
import type { WorkbenchComposerSchema, WorkbenchConversationSummary, WorkbenchFunction } from '../../types/api';
import { fallbackWorkbenchFunctions, getWorkbenchBlueprint } from './mock';
import type {
  WorkbenchDraftOptions,
  WorkbenchFollowUpState,
  WorkbenchMessageItem,
  WorkbenchResultData,
  WorkbenchTaskCardData,
} from './types';

const createLocalId = (prefix: string) => `${prefix}_${Math.random().toString(36).slice(2, 10)}`;
const workbenchCapabilityOrder = ['image_generation', 'image_recognition', 'text_to_speech', 'text_to_video', 'text_to_ppt'];
const workbenchCapabilityNameMap: Record<string, string> = {
  image_generation: '图像生成',
  image_recognition: '图像识别',
  text_to_speech: '文生语音',
  text_to_video: '视频生成',
  text_to_ppt: '文生 PPT',
};

const hasGarbledText = (text?: string | null) =>
  Boolean(text && (/\?{2,}|�|鏂|鎼|鏈|褰|绛|鐢|å|ç|æ|ï¼|ä¸|é/.test(text)));

const safeWorkbenchText = (text: string | null | undefined, fallback: string) =>
  text && !hasGarbledText(text) ? text : fallback;

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
  const readableContent = safeWorkbenchText(contentText, '任务已完成，结果已回流到当前会话。');
  const fallback: WorkbenchResultData = {
    title: '任务结果 / 后端回流',
    summary: readableContent,
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
    const fileName = parsed.fileName ?? '';
    const mimeType = parsed.mimeType ?? '';
    const looksLikeVideo =
      parsed.kind === 'video' ||
      Boolean(parsed.previewVideoUrl) ||
      mimeType.startsWith('video/') ||
      /\.(mp4|webm|mov|m4v)$/i.test(fileName) ||
      /\.(mp4|webm|mov|m4v)(\?|$)/i.test(parsed.downloadUrl ?? '');
    return {
      title: safeWorkbenchText(parsed.title, fallback.title),
      summary: safeWorkbenchText(parsed.summary, fallback.summary),
      chips,
      actionLabel: parsed.actionLabel ?? fallback.actionLabel,
      kind: looksLikeVideo ? 'video' : parsed.kind ?? (parsed.previewImageUrl ? 'image' : 'generic'),
      previewImageUrl: parsed.previewImageUrl,
      previewVideoUrl: parsed.previewVideoUrl ?? (looksLikeVideo ? parsed.downloadUrl : undefined),
      downloadUrl: parsed.downloadUrl,
      fileId: parsed.fileId,
      fileName: parsed.fileName,
      mimeType: parsed.mimeType,
    };
  } catch {
    return fallback;
  }
};

export const WorkbenchPage = () => {
  const queryClient = useQueryClient();
  const [conversationScope, setConversationScope] = useState<ConversationScope>('active');
  const [conversationId, setConversationId] = useState<string>();
  const [draftConversationMode, setDraftConversationMode] = useState(false);
  const [conversationKeyword, setConversationKeyword] = useState('');
  const [selectedCapability, setSelectedCapability] = useState('image_generation');
  const [draftTexts, setDraftTexts] = useState<Record<string, string>>({});
  const [draftOptions, setDraftOptions] = useState<Record<string, WorkbenchDraftOptions>>({});
  const [followUp, setFollowUp] = useState<WorkbenchFollowUpState | null>(null);
  const [activeTaskMeta, setActiveTaskMeta] = useState<{ taskId: string; capabilityCode: string; capabilityName: string } | null>(null);
  const [editingConversation, setEditingConversation] = useState<WorkbenchConversationSummary | null>(null);
  const [editingTitle, setEditingTitle] = useState('');
  const notifiedTaskRef = useRef<string | null>(null);

  const capabilityQuery = useQuery({
    queryKey: ['workbench', 'functions'],
    queryFn: fetchWorkbenchFunctions,
    staleTime: 60_000,
  });

  const conversationQuery = useQuery({
    queryKey: ['workbench', 'conversations', conversationScope, conversationKeyword],
    queryFn: () => fetchWorkbenchConversations(conversationScope, conversationKeyword || undefined),
    staleTime: 5_000,
  });

  const isArchivedScope = conversationScope === 'archived';
  const resolvedConversationId = conversationId ?? (!draftConversationMode ? conversationQuery.data?.[0]?.conversationId : undefined);

  const messagesQuery = useQuery({
    queryKey: ['workbench', 'messages', resolvedConversationId],
    queryFn: () => fetchWorkbenchConversationMessages(resolvedConversationId!),
    enabled: Boolean(resolvedConversationId),
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

  const renameConversationMutation = useMutation({
    mutationFn: ({ conversationId: nextConversationId, title }: { conversationId: string; title: string }) =>
      updateWorkbenchConversation(nextConversationId, { title }),
  });

  const archiveConversationMutation = useMutation({
    mutationFn: (nextConversationId: string) => archiveWorkbenchConversation(nextConversationId),
  });

  const restoreConversationMutation = useMutation({
    mutationFn: (nextConversationId: string) => restoreWorkbenchConversation(nextConversationId),
  });

  const capabilities = useMemo<WorkbenchFunction[]>(() => {
    const records = capabilityQuery.data?.length ? capabilityQuery.data : fallbackWorkbenchFunctions;
    return [...records]
      .filter((item) => workbenchCapabilityOrder.includes(item.functionCode))
      .map((item) => ({
        ...item,
        functionName: workbenchCapabilityNameMap[item.functionCode] ?? item.functionName,
      }))
      .sort((left, right) => workbenchCapabilityOrder.indexOf(left.functionCode) - workbenchCapabilityOrder.indexOf(right.functionCode));
  }, [capabilityQuery.data]);

  const activateConversation = (nextConversationId?: string) => {
    setConversationId(nextConversationId);
    setDraftConversationMode(!nextConversationId);
    setFollowUp(null);
    setActiveTaskMeta(null);
    setEditingConversation(null);
    notifiedTaskRef.current = null;

    if (!nextConversationId) {
      return;
    }

    const matchedConversation = conversationQuery.data?.find((item) => item.conversationId === nextConversationId);
    if (
      matchedConversation?.lastCapabilityCode &&
      capabilities.some((item) => item.functionCode === matchedConversation.lastCapabilityCode)
    ) {
      setSelectedCapability(matchedConversation.lastCapabilityCode);
    }
  };

  const handleScopeChange = (nextScope: ConversationScope) => {
    setConversationScope(nextScope);
    setConversationId(undefined);
    setDraftConversationMode(false);
    setFollowUp(null);
    setActiveTaskMeta(null);
    setEditingConversation(null);
    notifiedTaskRef.current = null;
  };

  const resolvedCapabilityCode =
    capabilities.length > 0 && capabilities.some((item) => item.functionCode === selectedCapability)
      ? selectedCapability
      : capabilities[0]?.functionCode ?? 'image_generation';

  const currentCapability = useMemo(
    () => capabilities.find((item) => item.functionCode === resolvedCapabilityCode) ?? capabilities[0],
    [capabilities, resolvedCapabilityCode],
  );

  const currentDraftText = draftTexts[resolvedCapabilityCode] ?? '';
  const currentDraftOptions = draftOptions[resolvedCapabilityCode] ?? {};
  const selectedServiceCode = currentDraftOptions.model ?? currentCapability?.defaultServiceCode ?? undefined;

  const composerSchemaQuery = useQuery({
    queryKey: ['workbench', 'composer-schema', currentCapability?.functionCode, selectedServiceCode],
    queryFn: () => fetchWorkbenchComposerSchema(currentCapability!.functionCode, selectedServiceCode),
    enabled: Boolean(currentCapability?.functionCode),
    staleTime: 60_000,
  });

  const blueprint = useMemo(() => {
    const fallbackBlueprint = getWorkbenchBlueprint(currentCapability?.functionCode ?? 'image_generation', []);
    const schema: WorkbenchComposerSchema | undefined = composerSchemaQuery.data;
    if (!schema) {
      return fallbackBlueprint;
    }
    return {
      placeholder: schema.placeholder,
      helper: schema.helper,
      fields: schema.fields.map((field) => ({
        key: field.key,
        label: field.label,
        placeholder: field.placeholder,
        options: field.options,
      })),
    };
  }, [composerSchemaQuery.data, currentCapability?.functionCode]);

  useEffect(() => {
    const status = taskQuery.data?.status;
    const taskId = taskQuery.data?.taskId;
    if (!taskId || notifiedTaskRef.current === taskId || (status !== 'SUCCESS' && status !== 'FAILED')) {
      return;
    }

    notifiedTaskRef.current = taskId;
    void queryClient.invalidateQueries({ queryKey: ['workbench', 'messages', resolvedConversationId] });

    if (status === 'SUCCESS') {
      void message.success('任务已完成，结果已从后端消息流回灌。');
      setTimeout(() => setActiveTaskMeta(null), 600);
      return;
    }

    void message.error('任务执行失败，请检查参数后重试。');
  }, [queryClient, resolvedConversationId, taskQuery.data?.status, taskQuery.data?.taskId]);

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

  const activeTaskId = activeTaskMeta?.taskId;

  const displayMessages = useMemo<WorkbenchMessageItem[]>(() => {
    const records = (messagesQuery.data ?? []).flatMap<WorkbenchMessageItem>((item) => {
      if (activeTaskId && item.contentType === 'status' && item.relatedTaskId === activeTaskId) {
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
          text: safeWorkbenchText(item.contentText, item.messageType === 'user' ? '用户消息暂不可读。' : '消息内容暂不可读。'),
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
  }, [activeTaskCard, activeTaskId, messagesQuery.data]);

  const handleSend = async () => {
    const prompt = currentDraftText.trim();
    if (isArchivedScope) {
      void message.warning('请先恢复会话或切回进行中列表后再发送新消息。');
      return;
    }

    if (!prompt || !currentCapability || submitChatMutation.isPending || createConversationMutation.isPending) {
      return;
    }

    try {
      let nextConversationId = resolvedConversationId;
      if (!nextConversationId) {
        const created = await createConversationMutation.mutateAsync(`${currentCapability.functionName} 会话`);
        nextConversationId = created.conversationId;
        setConversationId(nextConversationId);
        setDraftConversationMode(false);
      }

      const envelope = await submitChatMutation.mutateAsync({
        conversationId: nextConversationId,
        selectionMode: 'manual',
        capability: currentCapability.functionCode,
        inputText: prompt,
        serviceCode: currentDraftOptions.model,
        model: currentDraftOptions.model,
        options: JSON.stringify(currentDraftOptions),
        composerOptions: JSON.stringify(currentDraftOptions),
        schemaVersion: 'composer_schema_v1',
        templateCode: currentDraftOptions.template,
        attachments: currentDraftOptions.referenceImageFileId
          ? JSON.stringify([{ fileId: currentDraftOptions.referenceImageFileId, role: 'referenceImage' }])
          : undefined,
        inheritanceMode: followUp ? 'same_capability' : 'none',
        parentTaskId: followUp?.taskId,
        sourceAssetIds: currentDraftOptions.referenceImageFileId
          ? JSON.stringify([currentDraftOptions.referenceImageFileId])
          : '[]',
      });

      setActiveTaskMeta({
        taskId: envelope.data.taskId,
        capabilityCode: envelope.data.resolvedCapability,
        capabilityName: currentCapability.functionName,
      });
      notifiedTaskRef.current = null;
      setDraftTexts((previous) => ({ ...previous, [resolvedCapabilityCode]: '' }));
      setFollowUp(null);
      await queryClient.invalidateQueries({ queryKey: ['workbench', 'conversations'] });
      await queryClient.invalidateQueries({ queryKey: ['workbench', 'messages', nextConversationId] });
    } catch {
      // http.ts 已统一提示错误，这里不重复弹出。
    }
  };

  const openRenameDialog = (conversation: WorkbenchConversationSummary) => {
    setEditingConversation(conversation);
    setEditingTitle(conversation.title);
  };

  const handleRenameConversation = async () => {
    if (!editingConversation) {
      return;
    }

    const nextTitle = editingTitle.trim();
    if (!nextTitle) {
      void message.warning('会话标题不能为空。');
      return;
    }

    try {
      await renameConversationMutation.mutateAsync({
        conversationId: editingConversation.conversationId,
        title: nextTitle,
      });
      await queryClient.invalidateQueries({ queryKey: ['workbench', 'conversations'] });
      setEditingConversation(null);
      void message.success('会话标题已更新。');
    } catch {
      // http.ts 已统一提示错误，这里不重复弹出。
    }
  };

  const handleArchiveConversation = async (conversation: WorkbenchConversationSummary) => {
    const confirmed = window.confirm(`确认归档会话“${conversation.title}”吗？归档后会从当前列表移除。`);
    if (!confirmed) {
      return;
    }

    try {
      await archiveConversationMutation.mutateAsync(conversation.conversationId);
      if (resolvedConversationId === conversation.conversationId) {
        setConversationId(undefined);
        setDraftConversationMode(false);
        setFollowUp(null);
        setActiveTaskMeta(null);
        notifiedTaskRef.current = null;
      }
      await queryClient.invalidateQueries({ queryKey: ['workbench', 'conversations'] });
      void message.success('会话已归档。');
    } catch {
      // http.ts 已统一提示错误，这里不重复弹出。
    }
  };

  const handleRestoreConversation = async (conversation: WorkbenchConversationSummary) => {
    try {
      await restoreConversationMutation.mutateAsync(conversation.conversationId);
      setConversationScope('active');
      setConversationId(conversation.conversationId);
      setDraftConversationMode(false);
      setFollowUp(null);
      setActiveTaskMeta(null);
      notifiedTaskRef.current = null;
      await queryClient.invalidateQueries({ queryKey: ['workbench', 'conversations'] });
      await queryClient.invalidateQueries({ queryKey: ['workbench', 'messages', conversation.conversationId] });
      void message.success('会话已恢复到进行中列表。');
    } catch {
      // http.ts 已统一提示错误，这里不重复弹出。
    }
  };

  const sidebar = (
    <div className="workbench-sidebar">
      <ConversationHistory
        conversations={conversationQuery.data ?? []}
        capabilities={capabilities}
        activeConversationId={resolvedConversationId}
        draftMode={draftConversationMode}
        loading={conversationQuery.isLoading}
        scope={conversationScope}
        keyword={conversationKeyword}
        renamingConversationId={
          renameConversationMutation.isPending ? renameConversationMutation.variables?.conversationId : undefined
        }
        archivingConversationId={
          archiveConversationMutation.isPending ? archiveConversationMutation.variables : undefined
        }
        restoringConversationId={
          restoreConversationMutation.isPending ? restoreConversationMutation.variables : undefined
        }
        searchLoading={conversationQuery.isFetching && !conversationQuery.isLoading}
        onScopeChange={handleScopeChange}
        onKeywordChange={setConversationKeyword}
        onCreateConversation={() => {
          setConversationScope('active');
          activateConversation(undefined);
        }}
        onSelectConversation={activateConversation}
        onRenameConversation={openRenameDialog}
        onArchiveConversation={handleArchiveConversation}
        onRestoreConversation={handleRestoreConversation}
      />
    </div>
  );

  return (
    <div className="workbench-page">
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
        {isArchivedScope ? (
          <Card className="surface-card composer-panel">
            <Alert
              type="info"
              showIcon
              message="当前正在查看已归档会话"
              description="归档会话默认只读。你可以先恢复该会话，或切回进行中列表后继续发送新消息。"
            />
          </Card>
        ) : (
          <Composer
            capabilities={capabilities}
            selectedCapability={resolvedCapabilityCode}
            blueprint={blueprint}
            draftText={currentDraftText}
            options={currentDraftOptions}
            followUp={followUp}
            sending={submitChatMutation.isPending || createConversationMutation.isPending}
            onSelectCapability={setSelectedCapability}
            onDraftTextChange={(value) => setDraftTexts((previous) => ({ ...previous, [resolvedCapabilityCode]: value }))}
            onOptionChange={(key, value) =>
              setDraftOptions((previous) => ({
                ...previous,
                [resolvedCapabilityCode]: {
                  ...(previous[resolvedCapabilityCode] ?? {}),
                  [key]: value,
                },
              }))
            }
            onUploadReference={async (file) => {
              try {
                const uploaded = await uploadWorkbenchFile(file);
                setDraftOptions((previous) => ({
                  ...previous,
                  [resolvedCapabilityCode]: {
                    ...(previous[resolvedCapabilityCode] ?? {}),
                    referenceImage: uploaded.fileName,
                    referenceImageFileId: uploaded.fileId,
                  },
                }));
                void message.success('参考图已上传。');
              } catch {
                // http.ts 已统一提示错误，这里不重复弹出。
              }
            }}
            onClearDraft={() => {
              setDraftTexts((previous) => ({ ...previous, [resolvedCapabilityCode]: '' }));
              setDraftOptions((previous) => ({ ...previous, [resolvedCapabilityCode]: {} }));
            }}
            onClearFollowUp={() => setFollowUp(null)}
            onSend={() => {
              void handleSend();
            }}
          />
        )}
      </WorkbenchShell>
      <Modal
        title="重命名会话"
        open={Boolean(editingConversation)}
        okText="保存"
        cancelText="取消"
        confirmLoading={renameConversationMutation.isPending}
        onOk={() => {
          void handleRenameConversation();
        }}
        onCancel={() => {
          if (renameConversationMutation.isPending) {
            return;
          }
          setEditingConversation(null);
        }}
      >
        <Input
          maxLength={255}
          value={editingTitle}
          placeholder="请输入会话标题"
          onChange={(event) => setEditingTitle(event.target.value)}
          onPressEnter={() => {
            void handleRenameConversation();
          }}
        />
      </Modal>
    </div>
  );
};
