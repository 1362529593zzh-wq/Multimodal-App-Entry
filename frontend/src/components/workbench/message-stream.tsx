import { useEffect, useRef } from 'react';
import { Avatar, Card } from 'antd';
import { RobotOutlined, UserOutlined } from '@ant-design/icons';
import type { WorkbenchFunction } from '../../types/api';
import type { WorkbenchMessageItem } from '../../pages/workbench/types';
import { EmptyConversation } from './empty-conversation';
import { ResultCard } from './result-card';
import { TaskStatusCard } from './task-status-card';

interface MessageStreamProps {
  messages: WorkbenchMessageItem[];
  capabilities: WorkbenchFunction[];
  onSelectCapability: (capabilityCode: string) => void;
  onFollowUp: (message: WorkbenchMessageItem) => void;
}

export const MessageStream = ({
  messages,
  capabilities,
  onSelectCapability,
  onFollowUp,
}: MessageStreamProps) => {
  const bottomRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [messages]);

  if (messages.length === 0) {
    return (
      <Card className="surface-card workbench-stream">
        <EmptyConversation capabilities={capabilities} onSelect={onSelectCapability} />
      </Card>
    );
  }

  return (
    <Card className="surface-card workbench-stream">
      <div className="workbench-stream__list">
        {messages.map((message) => {
          if (message.type === 'task' && message.task) {
            return <TaskStatusCard key={message.id} task={message.task} />;
          }

          if (message.type === 'result' && message.result) {
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
              <Card className="message-card message-card--text">
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
    </Card>
  );
};
