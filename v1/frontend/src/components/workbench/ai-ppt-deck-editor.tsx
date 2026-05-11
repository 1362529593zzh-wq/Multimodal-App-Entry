import { CopyOutlined, DeleteOutlined, DownloadOutlined, PlusOutlined, RobotOutlined } from '@ant-design/icons';
import { Button, Card, Input, Modal, Select, Space, Tag, message } from 'antd';
import type { CSSProperties } from 'react';
import { useMemo, useState } from 'react';
import { changeAiPptTemplate, optimizeAiPptDeck, renderAiPpt, rewriteAiPptSlide } from '../../api/workbench';
import type { AiPptContentBlock, AiPptDeckPlan, AiPptSlidePlan } from '../../pages/workbench/types';

interface AiPptDeckEditorProps {
  deckPlan: AiPptDeckPlan;
}

const layoutOptions = [
  { label: '封面大图', value: 'cover-hero' },
  { label: '目录列表', value: 'agenda-list' },
  { label: '章节页', value: 'section-divider' },
  { label: '核心观点', value: 'key-message' },
  { label: '三卡片', value: 'three-cards' },
  { label: '左右对比', value: 'split-compare' },
  { label: '时间路线', value: 'timeline-roadmap' },
  { label: '指标卡片', value: 'metric-cards' },
  { label: '总结行动', value: 'summary-actions' },
];

const templateOptions = [
  { label: '咨询白底', value: 'consulting-clean' },
  { label: '商务蓝', value: 'business-blue' },
  { label: '深色科技', value: 'tech-dark' },
  { label: '金融绿', value: 'finance-green' },
  { label: '政企红金', value: 'government-red' },
  { label: '创业渐变', value: 'startup-gradient' },
];

const createSlide = (page: number): AiPptSlidePlan => ({
  id: `slide_${Math.random().toString(36).slice(2, 10)}`,
  page,
  role: 'content',
  type: 'content',
  title: '新页面',
  takeaway: '补充这一页的核心观点。',
  content: [{ type: 'point', title: '关键要点', text: '说明事实、洞察或行动建议。' }],
  layout: { type: 'key-message', density: 'medium' },
  visual: { icons: [], background: { type: 'solid', color: '#FAFBFC' } },
  speakerNotes: '围绕本页核心观点展开说明。',
});

const normalizePages = (slides: AiPptSlidePlan[]) => slides.map((slide, index) => ({ ...slide, page: index + 1 }));

export const AiPptDeckEditor = ({ deckPlan }: AiPptDeckEditorProps) => {
  const [editableDeck, setEditableDeck] = useState<AiPptDeckPlan>(deckPlan);
  const [activeIndex, setActiveIndex] = useState(0);
  const [busy, setBusy] = useState<string>();
  const activeSlide = editableDeck.slides[activeIndex] ?? editableDeck.slides[0];
  const theme = editableDeck.theme;

  const title = editableDeck.deck?.title ?? 'AI PPT';
  const slideCount = editableDeck.slides.length;
  const activeBlocks = useMemo(() => activeSlide?.content ?? [], [activeSlide]);

  const updateDeck = (patch: Partial<AiPptDeckPlan['deck']>) => {
    setEditableDeck((previous) => ({ ...previous, deck: { ...previous.deck, ...patch } }));
  };

  const updateSlide = (patch: Partial<AiPptSlidePlan>) => {
    setEditableDeck((previous) => ({
      ...previous,
      slides: previous.slides.map((slide, index) => (index === activeIndex ? { ...slide, ...patch } : slide)),
    }));
  };

  const updateBlock = (blockIndex: number, patch: Partial<AiPptContentBlock>) => {
    const nextBlocks = activeBlocks.map((block, index) => (index === blockIndex ? { ...block, ...patch } : block));
    updateSlide({ content: nextBlocks });
  };

  const handleRender = async () => {
    setBusy('render');
    try {
      const result = await renderAiPpt(editableDeck);
      Modal.success({
        title: 'PPTX 已生成',
        content: result.fileName ? `文件：${result.fileName}` : '已生成可编辑 PPTX 文件。',
        okText: '知道了',
      });
      if (result.downloadUrl) {
        window.open(result.downloadUrl, '_blank');
      }
    } catch {
      // http.ts 统一提示。
    } finally {
      setBusy(undefined);
    }
  };

  const handleRewrite = async (instruction: string) => {
    if (!activeSlide) return;
    setBusy('rewrite');
    try {
      const result = await rewriteAiPptSlide(editableDeck, activeSlide.id, instruction);
      setEditableDeck(result.deckPlan);
      void message.success('当前页已优化。');
    } catch {
      // http.ts 统一提示。
    } finally {
      setBusy(undefined);
    }
  };

  const handleOptimize = async (instruction: string) => {
    setBusy('optimize');
    try {
      const result = await optimizeAiPptDeck(editableDeck, instruction);
      setEditableDeck(result.deckPlan);
      void message.success('整份 PPT 已优化。');
    } catch {
      // http.ts 统一提示。
    } finally {
      setBusy(undefined);
    }
  };

  const handleTemplate = async (templateCode: string) => {
    setBusy('template');
    try {
      const result = await changeAiPptTemplate(editableDeck, templateCode);
      setEditableDeck(result.deckPlan);
    } catch {
      // http.ts 统一提示。
    } finally {
      setBusy(undefined);
    }
  };

  return (
    <Card className="message-card message-card--result ai-ppt-editor" bordered={false}>
      <div className="ai-ppt-editor__toolbar">
        <div>
          <Tag color="processing">AI PPT</Tag>
          <h4>{title}</h4>
          <p>{editableDeck.deck?.designDirection || 'AI 已生成结构化 PPT 设计稿，可编辑后导出 PPTX。'}</p>
        </div>
        <Space wrap>
          <Select
            className="ai-ppt-editor__template"
            value={theme?.visualStyle ?? 'consulting-clean'}
            options={templateOptions}
            onChange={handleTemplate}
          />
          <Button loading={busy === 'optimize'} onClick={() => handleOptimize('整体改成咨询公司风格，减少空话，增强结构化表达')}>
            整体优化
          </Button>
          <Button type="primary" icon={<DownloadOutlined />} loading={busy === 'render'} onClick={handleRender}>
            生成 PPTX
          </Button>
        </Space>
      </div>

      <div className="ai-ppt-editor__body">
        <aside className="ai-ppt-editor__sidebar">
          {editableDeck.slides.map((slide, index) => (
            <button
              key={slide.id}
              className={index === activeIndex ? 'ai-ppt-editor__thumb is-active' : 'ai-ppt-editor__thumb'}
              onClick={() => setActiveIndex(index)}
            >
              <span>{String(index + 1).padStart(2, '0')}</span>
              <strong>{slide.title || '未命名页面'}</strong>
              <em>{slide.layout?.type}</em>
            </button>
          ))}
          <Button
            block
            icon={<PlusOutlined />}
            onClick={() =>
              setEditableDeck((previous) => ({
                ...previous,
                slides: normalizePages([...previous.slides, createSlide(previous.slides.length + 1)]),
              }))
            }
          >
            新增页面
          </Button>
        </aside>

        <main className="ai-ppt-editor__canvas">
          {activeSlide ? <SlidePreview slide={activeSlide} deck={editableDeck} /> : null}
        </main>

        {activeSlide ? (
          <aside className="ai-ppt-editor__panel">
            <label>
              PPT 标题
              <Input value={title} onChange={(event) => updateDeck({ title: event.target.value })} />
            </label>
            <label>
              页面标题
              <Input value={activeSlide.title} onChange={(event) => updateSlide({ title: event.target.value })} />
            </label>
            <label>
              核心观点
              <Input.TextArea rows={2} value={activeSlide.takeaway} onChange={(event) => updateSlide({ takeaway: event.target.value })} />
            </label>
            <label>
              版式
              <Select
                value={activeSlide.layout?.type ?? 'key-message'}
                options={layoutOptions}
                onChange={(type) => updateSlide({ layout: { ...activeSlide.layout, type } })}
              />
            </label>
            <div className="ai-ppt-editor__blocks">
              <span>内容块</span>
              {activeBlocks.map((block, index) => (
                <Space key={`${activeSlide.id}_${index}`} direction="vertical" className="ai-ppt-editor__block">
                  <Input value={block.title} placeholder="要点标题" onChange={(event) => updateBlock(index, { title: event.target.value })} />
                  <Input.TextArea rows={2} value={block.text} placeholder="要点说明" onChange={(event) => updateBlock(index, { text: event.target.value })} />
                </Space>
              ))}
              <Button size="small" icon={<PlusOutlined />} onClick={() => updateSlide({ content: [...activeBlocks, { type: 'point', title: '', text: '' }] })}>
                添加要点
              </Button>
            </div>
            <label>
              演讲备注
              <Input.TextArea rows={2} value={activeSlide.speakerNotes} onChange={(event) => updateSlide({ speakerNotes: event.target.value })} />
            </label>
            <Space wrap>
              <Button icon={<RobotOutlined />} loading={busy === 'rewrite'} onClick={() => handleRewrite('精简本页文字，保留核心观点')}>
                AI 精简
              </Button>
              <Button loading={busy === 'rewrite'} onClick={() => handleRewrite('把这一页改成左右对比，突出问题和目标状态')}>
                改成对比页
              </Button>
              <Button
                icon={<CopyOutlined />}
                onClick={() =>
                  setEditableDeck((previous) => ({
                    ...previous,
                    slides: normalizePages([
                      ...previous.slides.slice(0, activeIndex + 1),
                      { ...activeSlide, id: `slide_${Math.random().toString(36).slice(2, 10)}` },
                      ...previous.slides.slice(activeIndex + 1),
                    ]),
                  }))
                }
              >
                复制
              </Button>
              <Button
                danger
                icon={<DeleteOutlined />}
                disabled={slideCount <= 1}
                onClick={() =>
                  setEditableDeck((previous) => ({
                    ...previous,
                    slides: normalizePages(previous.slides.filter((_, index) => index !== activeIndex)),
                  }))
                }
              >
                删除
              </Button>
            </Space>
          </aside>
        ) : null}
      </div>
    </Card>
  );
};

const SlidePreview = ({ slide, deck }: { slide: AiPptSlidePlan; deck: AiPptDeckPlan }) => {
  const theme = deck.theme ?? { primaryColor: '#122237', accentColor: '#DB7640', backgroundColor: '#FAFBFC', textColor: '#1F2937' };
  const layout = slide.layout?.type ?? 'key-message';
  const style = {
    '--ai-ppt-primary': theme.primaryColor,
    '--ai-ppt-accent': theme.accentColor,
    '--ai-ppt-bg': theme.backgroundColor,
    '--ai-ppt-text': theme.textColor ?? '#1F2937',
  } as CSSProperties;

  return (
    <div className={`ai-ppt-slide ai-ppt-slide--${layout}`} style={style}>
      <div className="ai-ppt-slide__rule" />
      <h2>{slide.title}</h2>
      {slide.subtitle ? <p className="ai-ppt-slide__subtitle">{slide.subtitle}</p> : null}
      {slide.takeaway ? <p className="ai-ppt-slide__takeaway">{slide.takeaway}</p> : null}
      <div className="ai-ppt-slide__content">
        {(slide.content ?? []).slice(0, layout === 'split-compare' ? 6 : 4).map((block, index) => (
          <article key={`${slide.id}_preview_${index}`}>
            <strong>{block.title || `要点 ${index + 1}`}</strong>
            <span>{block.text}</span>
          </article>
        ))}
      </div>
      <small>{String(slide.page).padStart(2, '0')} / {deck.slides.length}</small>
    </div>
  );
};
