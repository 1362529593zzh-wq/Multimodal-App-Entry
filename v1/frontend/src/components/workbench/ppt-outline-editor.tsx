import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { Button, Card, Input, Select, Space, Tag } from 'antd';
import { useMemo, useState } from 'react';
import type { PptDeckDraft, PptSlideDraft } from '../../pages/workbench/types';

interface PptOutlineEditorProps {
  draft: PptDeckDraft;
  generating?: boolean;
  onGeneratePpt: (draft: PptDeckDraft) => void;
}

const slideTypeOptions = [
  { label: '内容页', value: 'content' },
  { label: '章节页', value: 'section' },
  { label: '对比页', value: 'comparison' },
  { label: '时间线', value: 'timeline' },
  { label: '流程页', value: 'process' },
  { label: '指标页', value: 'metrics' },
  { label: '总结页', value: 'summary' },
];

const layoutOptions = [
  { label: '核心观点卡片', value: 'key-message' },
  { label: '左右对比', value: 'split-compare' },
  { label: '里程碑路线图', value: 'milestone-roadmap' },
  { label: '步骤流程', value: 'step-flow' },
  { label: '指标卡片', value: 'metric-cards' },
  { label: '章节大标题', value: 'section-hero' },
  { label: '总结行动项', value: 'summary-actions' },
];

const styleOptions = [
  { label: '商务汇报', value: 'business-report' },
  { label: '咨询公司风', value: 'consulting' },
  { label: '创业路演', value: 'startup-pitch' },
  { label: '产品发布', value: 'product-launch' },
  { label: '培训课件', value: 'training' },
  { label: '政企汇报', value: 'government-report' },
];

const themeOptions = [
  { label: '商务专业', value: 'business' },
  { label: '科技蓝', value: 'tech' },
  { label: '极简高级', value: 'minimal' },
  { label: '咨询白底', value: 'consulting-clean' },
  { label: '深色科技', value: 'tech-dark' },
  { label: '金融绿色', value: 'finance-green' },
  { label: '创业渐变', value: 'startup-gradient' },
  { label: '教育柔和', value: 'education-soft' },
  { label: '政企红金', value: 'government-red' },
  { label: '产品霓虹', value: 'product-neon' },
];

const templateOptions = [
  { label: '管理层汇报', value: 'management-report' },
  { label: '咨询报告', value: 'consulting-report' },
  { label: '商业计划书', value: 'business-plan' },
  { label: '产品介绍', value: 'product-intro' },
  { label: '技术架构', value: 'tech-architecture' },
  { label: '项目方案', value: 'project-proposal' },
  { label: '培训课件', value: 'training-course' },
  { label: '工作总结', value: 'work-summary' },
  { label: '市场分析', value: 'market-analysis' },
  { label: '路演融资', value: 'fundraising-pitch' },
];

const createSlide = (): PptSlideDraft => ({
  id: `slide_${Math.random().toString(36).slice(2, 10)}`,
  type: 'content',
  layout: 'key-message',
  visualHint: '',
  title: '新页面',
  subtitle: '',
  takeaway: '',
  bullets: [''],
  speakerNotes: '',
});

export const PptOutlineEditor = ({ draft, generating = false, onGeneratePpt }: PptOutlineEditorProps) => {
  const [editableDraft, setEditableDraft] = useState<PptDeckDraft>(draft);
  const totalSlides = useMemo(() => editableDraft.slides.length + 3, [editableDraft.slides.length]);

  const updateSlide = (index: number, patch: Partial<PptSlideDraft>) => {
    setEditableDraft((previous) => ({
      ...previous,
      slides: previous.slides.map((slide, slideIndex) => (slideIndex === index ? { ...slide, ...patch } : slide)),
    }));
  };

  const updateBullet = (slideIndex: number, bulletIndex: number, value: string) => {
    const slide = editableDraft.slides[slideIndex];
    const bullets = slide.bullets.map((item, index) => (index === bulletIndex ? value : item));
    updateSlide(slideIndex, { bullets });
  };

  return (
    <Card className="message-card message-card--result ppt-outline-editor" bordered={false}>
      <div className="ppt-outline-editor__header">
        <div>
          <Tag color="processing">PPT OUTLINE</Tag>
          <h4>可编辑 PPT 大纲</h4>
          <p>LLM 负责生成大纲，主题与模板由后端渲染为最终 PPTX。</p>
        </div>
        <Tag>{totalSlides} 页</Tag>
      </div>

      <div className="ppt-outline-editor__config">
        <label>
          PPT 标题
          <Input
            value={editableDraft.title}
            onChange={(event) => setEditableDraft((previous) => ({ ...previous, title: event.target.value }))}
          />
        </label>
        <label>
          内容风格
          <Select
            value={editableDraft.config?.style ?? 'business-report'}
            options={styleOptions}
            onChange={(style) =>
              setEditableDraft((previous) => ({ ...previous, config: { ...previous.config, style } }))
            }
          />
        </label>
        <label>
          渲染主题
          <Select
            value={editableDraft.config?.theme ?? 'business'}
            options={themeOptions}
            onChange={(theme) =>
              setEditableDraft((previous) => ({ ...previous, config: { ...previous.config, theme } }))
            }
          />
        </label>
        <label>
          模板结构
          <Select
            value={editableDraft.config?.template ?? 'management-report'}
            options={templateOptions}
            onChange={(template) =>
              setEditableDraft((previous) => ({ ...previous, config: { ...previous.config, template } }))
            }
          />
        </label>
      </div>

      <div className="ppt-outline-editor__slides">
        {editableDraft.slides.map((slide, slideIndex) => (
          <Card
            key={slide.id}
            size="small"
            className="ppt-outline-editor__slide"
            title={`第 ${slideIndex + 1} 页：${slide.title || '未命名页面'}`}
            extra={
              <Button
                type="text"
                danger
                size="small"
                icon={<DeleteOutlined />}
                disabled={editableDraft.slides.length <= 1}
                onClick={() =>
                  setEditableDraft((previous) => ({
                    ...previous,
                    slides: previous.slides.filter((_, index) => index !== slideIndex),
                  }))
                }
              />
            }
          >
            <div className="ppt-outline-editor__slide-grid">
              <label>
                页面类型
                <Select
                  value={slide.type}
                  options={slideTypeOptions}
                  onChange={(type) => updateSlide(slideIndex, { type })}
                />
              </label>
              <label>
                AI 推荐布局
                <Select
                  value={slide.layout ?? 'key-message'}
                  options={layoutOptions}
                  onChange={(layout) => updateSlide(slideIndex, { layout })}
                />
              </label>
              <label>
                页面标题
                <Input value={slide.title} onChange={(event) => updateSlide(slideIndex, { title: event.target.value })} />
              </label>
              <label className="ppt-outline-editor__full">
                AI 视觉建议
                <Input
                  value={slide.visualHint}
                  placeholder="例如：左侧放问题，右侧放解决方案；使用三张指标卡突出收益"
                  onChange={(event) => updateSlide(slideIndex, { visualHint: event.target.value })}
                />
              </label>
              <label className="ppt-outline-editor__full">
                核心观点
                <Input
                  value={slide.takeaway}
                  onChange={(event) => updateSlide(slideIndex, { takeaway: event.target.value })}
                />
              </label>
              <label className="ppt-outline-editor__full">
                要点
                <Space direction="vertical" className="ppt-outline-editor__bullets">
                  {slide.bullets.map((bullet, bulletIndex) => (
                    <Input
                      key={`${slide.id}_${bulletIndex}`}
                      value={bullet}
                      placeholder={`要点 ${bulletIndex + 1}`}
                      onChange={(event) => updateBullet(slideIndex, bulletIndex, event.target.value)}
                    />
                  ))}
                  <Button
                    size="small"
                    icon={<PlusOutlined />}
                    disabled={slide.bullets.length >= 5}
                    onClick={() => updateSlide(slideIndex, { bullets: [...slide.bullets, ''] })}
                  >
                    添加要点
                  </Button>
                </Space>
              </label>
              <label className="ppt-outline-editor__full">
                演讲备注
                <Input.TextArea
                  rows={2}
                  value={slide.speakerNotes}
                  onChange={(event) => updateSlide(slideIndex, { speakerNotes: event.target.value })}
                />
              </label>
            </div>
          </Card>
        ))}
      </div>

      <Space className="ppt-outline-editor__actions" wrap>
        <Button
          icon={<PlusOutlined />}
          onClick={() => setEditableDraft((previous) => ({ ...previous, slides: [...previous.slides, createSlide()] }))}
        >
          新增页面
        </Button>
        <Button type="primary" loading={generating} onClick={() => onGeneratePpt(editableDraft)}>
          生成并保存 PPT
        </Button>
      </Space>
    </Card>
  );
};
