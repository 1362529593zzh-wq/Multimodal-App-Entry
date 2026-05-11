import { useEffect, useRef } from 'react';
import { Avatar, Card } from 'antd';
import { RobotOutlined, UserOutlined } from '@ant-design/icons';
import type { WorkbenchFunction } from '../../types/api';
import type { WorkbenchMessageItem } from '../../pages/workbench/types';
import { EmptyConversation } from './empty-conversation';
import { AiPptDeckEditor } from './ai-ppt-deck-editor';
import { PptOutlineEditor } from './ppt-outline-editor';
import { ResultCard } from './result-card';
import { TaskStatusCard } from './task-status-card';
import type { PptDeckDraft } from '../../pages/workbench/types';

interface MessageStreamProps {
  messages: WorkbenchMessageItem[];
  capabilities: WorkbenchFunction[];
  onSelectCapability: (capabilityCode: string) => void;
  onFollowUp: (message: WorkbenchMessageItem) => void;
  onGeneratePpt: (message: WorkbenchMessageItem, draft: PptDeckDraft) => void;
  generatingPptTaskId?: string;
}

export const MessageStream = ({
  messages,
  capabilities,
  onSelectCapability,
  onFollowUp,
  onGeneratePpt,
  generatingPptTaskId,
}: MessageStreamProps) => {
  const bottomRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [messages]);

  if (messages.length === 0) {
    return (
      <section className="workbench-stream">
        <EmptyConversation capabilities={capabilities} onSelect={onSelectCapability} />
      </section>
    );
  }

  return (
    <section className="workbench-stream">
      <div className="workbench-stream__list">
        {messages.map((message) => {
          if (message.type === 'task' && message.task) {
            return <TaskStatusCard key={message.id} task={message.task} />;
          }

          if (message.type === 'result' && message.result) {
            if (message.result.kind === 'ppt_deck' && message.result.deckPlan) {
              return <AiPptDeckEditor key={message.id} deckPlan={message.result.deckPlan} />;
            }
            if (message.result.kind === 'ppt_draft' && message.result.pptDraft) {
              const taskId = message.result.chips.find((chip) => chip.startsWith('task: '))?.replace('task: ', '');
              return (
                <PptOutlineEditor
                  key={message.id}
                  draft={message.result.pptDraft}
                  generating={Boolean(taskId && taskId === generatingPptTaskId)}
                  onGeneratePpt={(draft) => onGeneratePpt(message, draft)}
                />
              );
            }
            return <ResultCard key={message.id} result={message.result} onFollowUp={() => onFollowUp(message)} />;
          }

          return (
            <div
              key={message.id}
              className={`message-row ${message.role === 'user' ? 'message-row--user' : 'message-row--assistant'}`}
            >
              <Avatar
                className="message-row__avatar"
                icon={message.role === 'user' ? <UserOutlined /> : <RobotOutlined />}
              />
              <Card className="message-card message-card--text" bordered={false}>
                <div className="message-card__header">
                  <strong>
                    {message.role === 'user' ? '你' : message.role === 'assistant' ? '助手' : '系统'}
                  </strong>
                  <span className="table-subtext">{new Date(message.createdAt).toLocaleTimeString()}</span>
                </div>
                <p>{message.text}</p>
              </Card>
            </div>
          );
        })}
        <div ref={bottomRef} />
      </div>
    </section>
  );
};
