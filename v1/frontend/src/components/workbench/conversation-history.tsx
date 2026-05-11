import { DeleteOutlined, EditOutlined, PlusOutlined, RollbackOutlined, SearchOutlined } from '@ant-design/icons';
import { Button, Empty, Input, Segmented, Tag, Tooltip } from 'antd';
import type { WorkbenchConversationSummary, WorkbenchFunction } from '../../types/api';
import { formatDateTime } from '../../utils/format';

export type ConversationScope = 'active' | 'archived';

interface ConversationHistoryProps {
  conversations: WorkbenchConversationSummary[];
  capabilities: WorkbenchFunction[];
  activeConversationId?: string;
  draftMode: boolean;
  loading: boolean;
  scope: ConversationScope;
  keyword: string;
  renamingConversationId?: string;
  archivingConversationId?: string;
  restoringConversationId?: string;
  searchLoading?: boolean;
  onScopeChange: (scope: ConversationScope) => void;
  onKeywordChange: (keyword: string) => void;
  onCreateConversation: () => void;
  onSelectConversation: (conversationId: string) => void;
  onRenameConversation: (conversation: WorkbenchConversationSummary) => void;
  onArchiveConversation: (conversation: WorkbenchConversationSummary) => void;
  onRestoreConversation: (conversation: WorkbenchConversationSummary) => void;
}

const colorByCapability: Record<string, string> = {
  image_generation: '#5b8cff',
  image_recognition: '#4fc49a',
  text_to_speech: '#ffb04f',
  text_to_ppt: '#9b72ff',
  text_to_video: '#5d6bff',
};

const previewTitleMap: Record<string, string> = {
  'Image Generation Result': '图像生成结果',
  'Video Generation Result': '视频生成结果',
  'PPT Generation Result': 'PPT 生成结果',
  'Speech Generation Result': '语音生成结果',
};

const fallbackCapabilityNameMap: Record<string, string> = {
  image_generation: '图像生成',
  image_recognition: '图像识别',
  text_to_speech: '文生语音',
  text_to_ppt: '文生 PPT',
  text_to_video: '视频生成',
};

const hasGarbledText = (text?: string | null) =>
  Boolean(text && (/\?{2,}|�|鏂|鎼|鏈|褰|绛|鐢|å|ç|æ|ï¼|ä¸|é/.test(text)));

const getCapabilityName = (capabilityCode: string | null, capabilityName?: string) => {
  if (!capabilityCode) return '通用会话';
  if (capabilityName && !hasGarbledText(capabilityName)) return capabilityName;
  return fallbackCapabilityNameMap[capabilityCode] ?? capabilityCode;
};

const getConversationTitle = (conversation: WorkbenchConversationSummary, capabilityName: string) => {
  if (conversation.title && !hasGarbledText(conversation.title)) return conversation.title;
  return `${capabilityName} 会话`;
};

const normalizePreview = (preview: string | null | undefined, capabilityName: string) => {
  if (!preview || hasGarbledText(preview)) return `结果：${capabilityName}内容已生成`;
  return Object.entries(previewTitleMap).reduce(
    (text, [source, target]) => text.replace(source, target),
    preview,
  );
};

export const ConversationHistory = ({
  conversations,
  capabilities,
  activeConversationId,
  draftMode,
  loading,
  scope,
  keyword,
  renamingConversationId,
  archivingConversationId,
  restoringConversationId,
  searchLoading,
  onScopeChange,
  onKeywordChange,
  onCreateConversation,
  onSelectConversation,
  onRenameConversation,
  onArchiveConversation,
  onRestoreConversation,
}: ConversationHistoryProps) => {
  const capabilityNameMap = new Map(capabilities.map((item) => [item.functionCode, item.functionName]));
  const isArchivedScope = scope === 'archived';

  return (
    <section className="conversation-history">
      <div className="conversation-history__top">
        <Button
          block
          size="large"
          type="primary"
          icon={<PlusOutlined />}
          disabled={isArchivedScope}
          onClick={onCreateConversation}
        >
          新建对话
        </Button>
        <Input
          allowClear
          size="large"
          prefix={<SearchOutlined />}
          value={keyword}
          placeholder="搜索会话"
          onChange={(event) => onKeywordChange(event.target.value)}
        />
        <Segmented
          block
          size="small"
          value={scope}
          options={[
            { label: '进行中', value: 'active' },
            { label: '已归档', value: 'archived' },
          ]}
          onChange={(value) => onScopeChange(value as ConversationScope)}
        />
      </div>

      <div className="conversation-history__title">
        <span>{isArchivedScope ? '归档会话' : '最近会话'}</span>
        {searchLoading || loading ? <Tag bordered={false}>加载中</Tag> : <Tag bordered={false}>{conversations.length}</Tag>}
      </div>

      <div className="conversation-history__list">
        {draftMode && !isArchivedScope ? (
          <button type="button" className="conversation-history__entry is-active" onClick={onCreateConversation}>
            <span className="conversation-history__dot" style={{ background: '#5b8cff' }} />
            <strong>新的工作台会话</strong>
            <p>选择能力后即可开始输入任务需求</p>
            <small>草稿</small>
          </button>
        ) : null}

        {conversations.length === 0 ? (
          <Empty
            className="conversation-history__empty"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={isArchivedScope ? '暂无归档会话' : '暂无会话记录'}
          />
        ) : (
          conversations.map((conversation) => {
            const active = conversation.conversationId === activeConversationId && !draftMode;
            const capabilityName = getCapabilityName(
              conversation.lastCapabilityCode,
              conversation.lastCapabilityCode ? capabilityNameMap.get(conversation.lastCapabilityCode) : undefined,
            );
            const dotColor = conversation.lastCapabilityCode
              ? colorByCapability[conversation.lastCapabilityCode] ?? '#5b8cff'
              : '#94a3b8';
            const title = getConversationTitle(conversation, capabilityName);
            const preview = normalizePreview(conversation.latestMessagePreview, capabilityName);

            return (
              <article
                key={conversation.conversationId}
                className={active ? 'conversation-history__entry is-active' : 'conversation-history__entry'}
              >
                <button type="button" onClick={() => onSelectConversation(conversation.conversationId)}>
                  <span className="conversation-history__dot" style={{ background: dotColor }} />
                  <strong>{title}</strong>
                  <p>{preview}</p>
                  <small>{formatDateTime(conversation.latestMessageAt ?? conversation.updatedAt)}</small>
                </button>
                <div className="conversation-history__item-actions">
                  <Tooltip title="重命名">
                    <Button
                      size="small"
                      shape="circle"
                      icon={<EditOutlined />}
                      loading={renamingConversationId === conversation.conversationId}
                      onClick={() => onRenameConversation(conversation)}
                    />
                  </Tooltip>
                  {isArchivedScope ? (
                    <Tooltip title="恢复">
                      <Button
                        size="small"
                        shape="circle"
                        icon={<RollbackOutlined />}
                        loading={restoringConversationId === conversation.conversationId}
                        onClick={() => onRestoreConversation(conversation)}
                      />
                    </Tooltip>
                  ) : (
                    <Tooltip title="归档">
                      <Button
                        size="small"
                        danger
                        shape="circle"
                        icon={<DeleteOutlined />}
                        loading={archivingConversationId === conversation.conversationId}
                        onClick={() => onArchiveConversation(conversation)}
                      />
                    </Tooltip>
                  )}
                </div>
              </article>
            );
          })
        )}
      </div>
    </section>
  );
};
