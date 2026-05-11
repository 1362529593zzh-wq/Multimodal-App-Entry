# 文生 PPT 改进方案（迭代到 4）

## 1. 背景与目标

当前项目中的文生 PPT 功能已经具备基础能力：

- 用户在工作台选择 `文生 PPT`；
- 后端调用大模型生成 PPT 大纲；
- 前端展示可编辑大纲；
- 用户确认后，后端使用 Apache POI 渲染 PPTX；
- 最终生成可下载的 PPT 文件。

当前主要代码位置：

- 后端主逻辑：`backend/src/main/java/com/shupai/multimodal/appentry/service/impl/WorkbenchServiceImpl.java`
- PPT 请求 DTO：`backend/src/main/java/com/shupai/multimodal/appentry/model/dto/PptGenerateRequest.java`
- 工作台接口：`backend/src/main/java/com/shupai/multimodal/appentry/web/WorkbenchController.java`
- 前端大纲编辑器：`frontend/src/components/workbench/ppt-outline-editor.tsx`
- 前端工作台类型：`frontend/src/pages/workbench/types.ts`
- 前端消息流渲染：`frontend/src/components/workbench/message-stream.tsx`

本方案目标不是推翻原有工作台，而是在长期可维护的前提下，将 PPT 生成功能从：

```text
AI 生成大纲 + 后端固定模板渲染
```

升级为：

```text
AI 生成结构化 PPT 设计稿 + 前端可视化编辑 + 后端稳定导出 PPTX
```

本阶段实施到“迭代 4”，即完成：

- 旧功能稳定；
- 新增 `ai_full` 智能生成模式；
- AI 输出完整 `DeckPlan`；
- 前端提供智能 PPT 编辑器；
- 支持 DeckPlan 导出 PPTX；
- 支持单页 AI 重写、整体优化、换模板。

图标、图表、图片生成、视觉质检、文档转 PPT、品牌包等能力作为后续阶段预留，不纳入本阶段强制范围。

---

## 2. 总体原则

### 2.0 明确实现边界

本方案只覆盖“文生 PPT 迭代 0-4”的能力建设，目标是形成长期可维护的智能 PPT 工作台 MVP。

本阶段要实现：

- 保留旧版 `legacy` PPT 生成链路；
- 新增 `ai_full` PPT 智能生成链路；
- AI 生成结构化 `AiPptDeckPlan`；
- 前端展示并编辑 `AiPptDeckPlan`；
- 前端提供基础逐页预览；
- 后端根据 `AiPptDeckPlan` 导出可编辑 PPTX；
- 支持单页 AI 重写；
- 支持整份 PPT AI 优化；
- 支持内置模板切换；
- 保证旧工作台、任务、文件下载能力不被破坏。

本阶段不实现：

- 不做完整在线 PPT 画布编辑器；
- 不做像 Canva 一样的自由拖拽元素编辑；
- 不做协同编辑；
- 不做分享链接；
- 不做版本历史管理；
- 不做完整模板市场；
- 不做数据库级模板管理；
- 不做品牌包管理后台；
- 不做文档转 PPT；
- 不做 AI 图片生成并插入 PPT；
- 不做 ECharts 图表生成；
- 不做 AI 视觉质检；
- 不做自动修复闭环；
- 不替换现有工作台整体架构；
- 不删除旧版 `ppt_draft` 能力。

本阶段的产品边界：

```text
输入主题
  ↓
AI 生成 DeckPlan
  ↓
用户编辑页面内容和版式
  ↓
可单页重写 / 整体优化 / 换模板
  ↓
导出 PPTX
```

不是：

```text
完整在线 PPT 编辑器
完整文档转 PPT 产品
完整企业模板管理系统
完整多模态视觉生成系统
```

### 2.1 保留原工作台

现有工作台能力入口不变：

```text
image_generation
image_recognition
text_to_speech
text_to_video
text_to_ppt
```

`text_to_ppt` 继续作为文生 PPT 的能力入口。

### 2.2 新旧模式共存

新增 PPT 生成模式：

```text
legacy：原有模式，生成 ppt_draft，再渲染 PPTX
ai_full：新模式，AI 生成完整 DeckPlan，再编辑和导出
```

前端提交参数建议增加：

```json
{
  "pptMode": "ai_full"
}
```

后端根据 `pptMode` 判断执行链路。

### 2.3 不破坏旧结果

保留旧结果类型：

```ts
kind: 'ppt_draft'
```

新增新结果类型：

```ts
kind: 'ppt_deck'
```

前端根据结果类型分别渲染：

```tsx
if (message.result.kind === 'ppt_deck' && message.result.deckPlan) {
  return <AiPptDeckEditor deckPlan={message.result.deckPlan} />;
}

if (message.result.kind === 'ppt_draft' && message.result.pptDraft) {
  return <PptOutlineEditor draft={message.result.pptDraft} />;
}
```

### 2.4 AI 负责设计，程序负责渲染

不建议让 AI 直接生成 PPTX 二进制文件。

推荐方式：

```text
用户需求
  ↓
AI 生成 AiPptDeckPlan
  ↓
前端编辑和预览
  ↓
后端根据 AiPptDeckPlan 渲染 PPTX
```

这样更稳定，也便于长期维护和扩展。

---

## 3. 文件结构治理规则

为避免本次改造后文件结构变乱，必须遵守以下规则。

### 3.1 不继续膨胀 WorkbenchServiceImpl

`WorkbenchServiceImpl.java` 只能保留工作台调度职责。

允许保留：

- 任务查询；
- 任务状态流转；
- 原有 legacy PPT 调用；
- 调用新 PPT service 的入口。

不允许继续新增：

- DeckPlan 生成细节；
- Prompt 拼接细节；
- 单页重写细节；
- 模板切换细节；
- PPTX layout 渲染细节；
- 新版 PPT DTO 解析细节。

新增 PPT 业务必须进入：

```text
backend/src/main/java/com/shupai/multimodal/appentry/service/ppt/
```

### 3.2 后端按职责分包

后端新增文件只允许放在以下目录。

PPT service：

```text
backend/src/main/java/com/shupai/multimodal/appentry/service/ppt/
```

PPT model：

```text
backend/src/main/java/com/shupai/multimodal/appentry/model/ppt/
```

PPT web 请求 DTO：

```text
backend/src/main/java/com/shupai/multimodal/appentry/model/dto/
```

PPT controller 扩展：

```text
backend/src/main/java/com/shupai/multimodal/appentry/web/
```

如果新增 controller，建议命名：

```text
PptWorkbenchController.java
```

如果继续复用 `WorkbenchController.java`，只允许增加薄接口，不允许写业务逻辑。

### 3.3 前端按组件职责分层

前端新增 PPT 组件只允许放在：

```text
frontend/src/components/workbench/
```

命名统一使用：

```text
ai-ppt-*.tsx
```

例如：

```text
ai-ppt-deck-editor.tsx
ai-ppt-slide-preview.tsx
ai-ppt-slide-panel.tsx
ai-ppt-layout-renderer.tsx
ai-ppt-template-picker.tsx
ai-ppt-toolbar.tsx
```

不允许把新版 PPT 编辑器逻辑继续堆进：

```text
frontend/src/components/workbench/message-stream.tsx
frontend/src/pages/workbench/page.tsx
```

这两个文件只负责：

- 消息流选择渲染哪个结果组件；
- 工作台整体状态和请求调度。

### 3.4 类型定义集中管理

前端 PPT 类型统一放在：

```text
frontend/src/pages/workbench/types.ts
```

短期可以放在该文件中。

如果类型明显增多，再拆到：

```text
frontend/src/pages/workbench/ppt-types.ts
```

但不要在每个组件里重复定义 `AiPptDeckPlan`、`AiPptSlidePlan` 等结构。

### 3.5 API 调用集中管理

所有 PPT 新接口调用统一放在：

```text
frontend/src/api/workbench.ts
```

建议新增：

```ts
generateAiPptDeck(...)
renderAiPpt(...)
rewriteAiPptSlide(...)
optimizeAiPptDeck(...)
changeAiPptTemplate(...)
```

组件内不直接写 `axios` 请求。

### 3.6 样式集中管理

当前项目样式主要在：

```text
frontend/src/index.css
```

本阶段可继续使用该文件，但必须增加清晰分区：

```css
/* AI PPT editor */
```

所有新增 class 统一前缀：

```text
ai-ppt-
```

例如：

```text
ai-ppt-editor
ai-ppt-editor__sidebar
ai-ppt-editor__canvas
ai-ppt-editor__panel
ai-ppt-slide
ai-ppt-slide--three-cards
```

不允许复用大量 `ppt-outline-editor__*` 样式来承载新编辑器，避免旧新模式耦合。

### 3.7 渲染器必须可替换

后端 PPTX 渲染必须通过接口：

```java
PptRenderService
```

当前实现：

```java
PoiAiPptRenderService
```

业务层只能依赖：

```java
PptRenderService
```

不能在 `AiPptGenerationService` 或 controller 中直接依赖 Apache POI 类。

这样后续可以平滑替换为：

```text
PptxGenJsRenderService
HtmlToPptRenderService
```

### 3.8 Prompt 不散落在业务代码中

本阶段允许先把 prompt 放在 `PptPlanningService`、`PptSlideDesignService`、`PptOptimizeService` 中。

但不允许散落在：

```text
WorkbenchServiceImpl.java
WorkbenchController.java
PoiAiPptRenderService.java
```

如果 prompt 增多，后续统一抽到：

```text
backend/src/main/java/com/shupai/multimodal/appentry/service/ppt/PptPromptFactory.java
```

### 3.9 禁止跨层调用

必须遵守：

```text
Controller -> Service -> Renderer / AI Client / File Service
```

禁止：

```text
Controller 直接调用 Apache POI
Controller 直接拼 prompt
前端组件直接写接口 URL
Renderer 直接调用 AI
Renderer 修改任务状态
```

### 3.10 文件数量控制

迭代 0-4 后端新增文件建议控制在：

```text
service/ppt: 7 个以内
model/ppt: 8 个以内
dto: 4 个以内
web: 1 个以内
```

前端新增文件建议控制在：

```text
components/workbench/ai-ppt-*.tsx: 6 个以内
```

如果某个文件超过约 500 行，再考虑拆分。

---

## 4. 目标架构

```text
工作台 text_to_ppt
  ↓
读取 pptMode
  ├─ legacy
  │   ├─ AI 生成旧版大纲
  │   ├─ PptOutlineEditor 编辑
  │   └─ Apache POI 渲染 PPTX
  │
  └─ ai_full
      ├─ PptPlanningService 生成 DeckPlan
      ├─ AiPptDeckEditor 编辑与预览
      ├─ PptRenderService 渲染 PPTX
      ├─ PptSlideDesignService 单页重写
      ├─ PptTemplateService 应用模板
      └─ PptOptimizeService 整体优化
```

---

## 5. 后端模块规划

当前 PPT 逻辑集中在：

```text
backend/src/main/java/com/shupai/multimodal/appentry/service/impl/WorkbenchServiceImpl.java
```

长期维护不建议继续把新逻辑堆在这个类中。

建议新增 PPT 独立模块：

```text
backend/src/main/java/com/shupai/multimodal/appentry/service/ppt/
  AiPptGenerationService.java
  PptPlanningService.java
  PptSlideDesignService.java
  PptOptimizeService.java
  PptTemplateService.java
  PptRenderService.java
  PoiAiPptRenderService.java
```

建议新增模型目录：

```text
backend/src/main/java/com/shupai/multimodal/appentry/model/ppt/
  AiPptDeckPlan.java
  AiPptSlidePlan.java
  AiPptTheme.java
  AiPptLayoutSpec.java
  AiPptVisualSpec.java
  AiPptContentBlock.java
  AiPptChartSpec.java
  AiPptPreviewSlide.java
```

短期也可以用 `Map<String, Object>` 兼容，但长期建议使用明确 DTO，避免前后端 Schema 漂移。

---

## 6. 前端模块规划

保留旧组件：

```text
frontend/src/components/workbench/ppt-outline-editor.tsx
```

新增新模式组件：

```text
frontend/src/components/workbench/ai-ppt-deck-editor.tsx
frontend/src/components/workbench/ai-ppt-slide-preview.tsx
frontend/src/components/workbench/ai-ppt-slide-panel.tsx
frontend/src/components/workbench/ai-ppt-layout-renderer.tsx
frontend/src/components/workbench/ai-ppt-template-picker.tsx
frontend/src/components/workbench/ai-ppt-toolbar.tsx
```

建议编辑器布局：

```text
┌────────────────────────────────────────────┐
│ 顶部工具栏：标题 / 模板 / AI 优化 / 导出 PPT │
├──────────────┬────────────────┬────────────┤
│ 左侧页面列表  │ 中间页面预览    │ 右侧属性编辑 │
│              │                │            │
│ 封面          │ 当前 Slide      │ 标题        │
│ 目录          │                │ 核心观点    │
│ 内容页        │                │ 内容块      │
│ 总结页        │                │ 版式        │
└──────────────┴────────────────┴────────────┘
```

---

## 7. 核心数据结构

### 7.1 AiPptDeckPlan

```ts
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
  };
  theme: AiPptTheme;
  storyline: string[];
  slides: AiPptSlidePlan[];
}
```

### 7.2 AiPptTheme

```ts
export interface AiPptTheme {
  primaryColor: string;
  secondaryColor?: string;
  accentColor: string;
  backgroundColor: string;
  textColor?: string;
  fontFamily?: string;
  visualStyle?: string;
}
```

### 7.3 AiPptSlidePlan

```ts
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
```

### 7.4 AiPptContentBlock

```ts
export interface AiPptContentBlock {
  type: 'point' | 'metric' | 'quote' | 'table' | 'timeline' | 'process';
  title?: string;
  text?: string;
  value?: string;
  unit?: string;
  items?: string[];
}
```

### 7.5 AiPptLayoutSpec

```ts
export interface AiPptLayoutSpec {
  type:
    | 'cover-hero'
    | 'agenda-list'
    | 'section-divider'
    | 'key-message'
    | 'three-cards'
    | 'split-compare'
    | 'timeline-roadmap'
    | 'metric-cards'
    | 'summary-actions';
  composition?: string;
  density?: 'low' | 'medium' | 'high';
  emphasis?: string;
}
```

### 7.6 AiPptVisualSpec

```ts
export interface AiPptVisualSpec {
  icons?: string[];
  imagePrompt?: string;
  imageFileId?: string;
  chartSpec?: AiPptChartSpec;
  background?: {
    type: 'solid' | 'gradient' | 'pattern' | 'image';
    color?: string;
  };
}
```

---

## 8. 接口设计

### 8.1 保留旧接口

```http
POST /api/workbench/tasks/{taskId}/ppt/generate-file
```

用途：

- 旧版 `ppt_draft` 生成 PPTX；
- 新版也可以继续复用，但建议新增更明确的渲染接口。

### 8.2 新增 AI 生成 DeckPlan

```http
POST /api/workbench/ppt/ai-generate
```

请求示例：

```json
{
  "prompt": "生成一份 AI 客服系统建设方案",
  "assetIds": [],
  "options": {
    "pages": 10,
    "audience": "公司管理层",
    "scenario": "project_proposal",
    "style": "consulting",
    "theme": "consulting-clean",
    "pptMode": "ai_full",
    "language": "zh-CN"
  }
}
```

返回示例：

```json
{
  "kind": "ppt_deck",
  "deckPlan": {},
  "previewSlides": []
}
```

### 8.3 新增 PPTX 渲染接口

```http
POST /api/workbench/ppt/render
```

请求示例：

```json
{
  "deckPlan": {}
}
```

返回示例：

```json
{
  "fileId": "file_xxx",
  "fileName": "AI客服系统建设方案.pptx",
  "downloadUrl": "/api/workbench/files/file_xxx/download"
}
```

### 8.4 新增单页重写接口

```http
POST /api/workbench/ppt/slides/{slideId}/rewrite
```

请求示例：

```json
{
  "deckPlan": {},
  "instruction": "把这一页改成左右对比，减少文字，突出当前问题和目标状态"
}
```

返回示例：

```json
{
  "slide": {}
}
```

### 8.5 新增整体优化接口

```http
POST /api/workbench/ppt/optimize
```

请求示例：

```json
{
  "deckPlan": {},
  "instruction": "整体改成咨询公司风格，减少文字，增强数据感"
}
```

返回示例：

```json
{
  "deckPlan": {}
}
```

### 8.6 新增换模板接口

```http
POST /api/workbench/ppt/change-template
```

请求示例：

```json
{
  "deckPlan": {},
  "templateCode": "business-blue"
}
```

返回示例：

```json
{
  "deckPlan": {}
}
```

---

## 9. AI Prompt 方案

### 9.1 生成 DeckPlan Prompt

```text
你是一个由资深咨询顾问、PPT 策划专家和视觉设计师组成的 AI PPT 生成系统。

你的任务不是简单生成大纲，而是生成可以被程序渲染为 PPTX 的结构化 PPT 设计稿。

请严格输出 JSON，不要输出 Markdown。

设计要求：
1. 每页只表达一个核心观点。
2. 每页必须包含 title、takeaway、content、layout、visual、speakerNotes。
3. 根据内容自动选择页面类型。
4. 根据页面类型自动选择布局。
5. 避免空话、套话、占位符。
6. 内容必须完整、具体、可汇报。
7. 保持中文表达自然专业。

可用 layout：
- cover-hero
- agenda-list
- section-divider
- key-message
- three-cards
- split-compare
- timeline-roadmap
- metric-cards
- summary-actions

输出 JSON Schema：
{
  "deck": {
    "title": "",
    "audience": "",
    "scenario": "",
    "goal": "",
    "language": "zh-CN",
    "tone": "",
    "pageCount": 0,
    "designDirection": ""
  },
  "theme": {
    "primaryColor": "",
    "secondaryColor": "",
    "accentColor": "",
    "backgroundColor": "",
    "textColor": "",
    "fontFamily": "Microsoft YaHei",
    "visualStyle": ""
  },
  "storyline": [],
  "slides": [
    {
      "id": "",
      "page": 1,
      "role": "",
      "type": "",
      "title": "",
      "subtitle": "",
      "takeaway": "",
      "content": [
        {
          "type": "point",
          "title": "",
          "text": ""
        }
      ],
      "layout": {
        "type": "",
        "composition": "",
        "density": "medium",
        "emphasis": ""
      },
      "visual": {
        "icons": [],
        "imagePrompt": "",
        "chartSpec": null,
        "background": {
          "type": "solid",
          "color": ""
        }
      },
      "speakerNotes": ""
    }
  ]
}
```

### 9.2 单页重写 Prompt

```text
你是资深 PPT 页面设计专家。

请根据用户指令只重写当前 slide。
不要修改其他页面。
必须保持 JSON Schema 不变。
输出 JSON，不要 Markdown。

用户指令：
{{instruction}}

整份 PPT 上下文：
{{deckSummary}}

当前 slide：
{{slideJson}}
```

### 9.3 整体优化 Prompt

```text
你是资深咨询顾问和演示文稿设计总监。

请根据用户指令优化整份 PPT DeckPlan。
你可以优化：
- 标题表达
- 故事线
- 页面顺序
- 页面版式
- 核心观点
- 要点内容
- 视觉风格

要求：
1. 保留用户已经明确表达的业务含义。
2. 不要删除关键页面。
3. 不要输出空话。
4. 保持 JSON Schema 不变。
5. 只输出 JSON。

用户指令：
{{instruction}}

当前 DeckPlan：
{{deckPlanJson}}
```

---

## 10. 迭代计划

## 迭代 0：基础修复与现状稳定

### 目标

保证旧版文生 PPT 稳定可用，修复现有明显问题，为新模式改造打基础。

### 操作内容

#### 后端

修改：

```text
backend/src/main/java/com/shupai/multimodal/appentry/service/impl/WorkbenchServiceImpl.java
```

操作：

1. 修复后端 fallback 中文乱码；
2. 为 `generatePptFile` 增加参数校验；
3. 为 PPT 渲染增加更详细日志；
4. 当 slide 为空时生成兜底页；
5. 当 `pages` 小于 4 时自动修正；
6. 保留旧版 `ppt_draft` 逻辑。

#### 前端

修改：

```text
frontend/src/components/workbench/ppt-outline-editor.tsx
```

操作：

1. 修复界面中文乱码；
2. 修复按钮、标签、placeholder；
3. 增加空状态提示；
4. 保证旧版大纲编辑器仍可正常生成 PPT。

### 验收标准

- 前端没有乱码；
- 后端兜底文案没有乱码；
- 旧模式可以正常生成 PPTX；
- PPT 生成失败有明确日志；
- 不影响其他工作台能力。

---

## 迭代 1：引入 ai_full 模式与 DeckPlan

### 目标

新增 AI 全流程模式，让 AI 生成完整 `AiPptDeckPlan`，同时保留旧模式。

### 操作内容

#### 后端

新增：

```text
backend/src/main/java/com/shupai/multimodal/appentry/service/ppt/AiPptGenerationService.java
backend/src/main/java/com/shupai/multimodal/appentry/service/ppt/PptPlanningService.java
```

新增模型：

```text
backend/src/main/java/com/shupai/multimodal/appentry/model/ppt/AiPptDeckPlan.java
backend/src/main/java/com/shupai/multimodal/appentry/model/ppt/AiPptSlidePlan.java
backend/src/main/java/com/shupai/multimodal/appentry/model/ppt/AiPptTheme.java
backend/src/main/java/com/shupai/multimodal/appentry/model/ppt/AiPptLayoutSpec.java
backend/src/main/java/com/shupai/multimodal/appentry/model/ppt/AiPptVisualSpec.java
backend/src/main/java/com/shupai/multimodal/appentry/model/ppt/AiPptContentBlock.java
```

修改：

```text
backend/src/main/java/com/shupai/multimodal/appentry/service/impl/WorkbenchServiceImpl.java
```

增加判断：

```java
if (CAPABILITY_TEXT_TO_PPT.equals(task.getFunctionCode())) {
    Map<String, Object> options = parseRequestParams(task.getRequestParams());
    if ("ai_full".equalsIgnoreCase(readStringOption(options, "pptMode"))) {
        executeAiPptGenerationTask(task, service);
        return;
    }
    executePptGenerationTask(task, service);
    return;
}
```

#### 前端

修改：

```text
frontend/src/pages/workbench/types.ts
```

新增：

```ts
export interface AiPptDeckPlan {}
export interface AiPptSlidePlan {}
export interface AiPptPreviewSlide {}
```

扩展：

```ts
kind?: 'image' | 'audio' | 'video' | 'file' | 'generic' | 'ppt_draft' | 'ppt_deck';
deckPlan?: AiPptDeckPlan;
previewSlides?: AiPptPreviewSlide[];
```

新增简版组件：

```text
frontend/src/components/workbench/ai-ppt-deck-editor.tsx
```

修改：

```text
frontend/src/components/workbench/message-stream.tsx
```

增加 `ppt_deck` 渲染分支。

### 验收标准

- 提交 `pptMode = ai_full` 后，后端返回 `kind = ppt_deck`；
- 前端能展示 DeckPlan；
- 旧版 `ppt_draft` 仍可用；
- AI 输出包含 deck、theme、storyline、slides；
- 每个 slide 至少包含 title、takeaway、content、layout、visual。

---

## 迭代 2：AI PPT 编辑器与前端逐页预览

### 目标

把 `ppt_deck` 从 JSON 展示升级成可编辑的智能 PPT 编辑器。

### 操作内容

#### 前端

新增：

```text
frontend/src/components/workbench/ai-ppt-deck-editor.tsx
frontend/src/components/workbench/ai-ppt-slide-preview.tsx
frontend/src/components/workbench/ai-ppt-slide-panel.tsx
frontend/src/components/workbench/ai-ppt-layout-renderer.tsx
frontend/src/components/workbench/ai-ppt-toolbar.tsx
```

实现布局：

```text
顶部工具栏
左侧页面列表
中间页面预览
右侧属性面板
```

第一批支持 layout：

```text
cover-hero
agenda-list
section-divider
key-message
three-cards
split-compare
timeline-roadmap
metric-cards
summary-actions
```

支持编辑：

- PPT 标题；
- slide 标题；
- subtitle；
- takeaway；
- content blocks；
- layout type；
- speakerNotes；
- theme 色彩。

支持页面操作：

- 新增页面；
- 删除页面；
- 复制页面；
- 上移页面；
- 下移页面。

#### 后端

本迭代后端只需继续返回 DeckPlan。

### 验收标准

- 用户能在工作台看到逐页 PPT 预览；
- 用户能编辑每页内容；
- 用户能切换基础 layout；
- 用户能新增、删除、复制、调整页面顺序；
- 编辑后的 DeckPlan 保持完整；
- 旧版编辑器不受影响。

---

## 迭代 3：DeckPlan 导出 PPTX

### 目标

让 `AiPptDeckPlan` 可以导出真正的 PPTX 文件。

### 操作内容

#### 后端

新增接口：

```http
POST /api/workbench/ppt/render
```

新增服务：

```text
backend/src/main/java/com/shupai/multimodal/appentry/service/ppt/PptRenderService.java
backend/src/main/java/com/shupai/multimodal/appentry/service/ppt/PoiAiPptRenderService.java
```

建议接口：

```java
public interface PptRenderService {
    GeneratedFilePayload render(AiPptDeckPlan deckPlan);
}
```

第一版使用 Apache POI 实现。

layout 映射：

```text
cover-hero       -> 封面页
agenda-list      -> 目录页
section-divider  -> 章节页
key-message      -> 核心观点页
three-cards      -> 三卡片页
split-compare    -> 左右对比页
timeline-roadmap -> 时间轴页
metric-cards     -> 指标页
summary-actions  -> 总结页
```

建议拆分布局渲染方法：

```java
renderCoverHero(...)
renderAgendaList(...)
renderSectionDivider(...)
renderKeyMessage(...)
renderThreeCards(...)
renderSplitCompare(...)
renderTimelineRoadmap(...)
renderMetricCards(...)
renderSummaryActions(...)
```

#### 前端

在 `AiPptDeckEditor` 顶部工具栏增加：

```text
生成 PPTX
```

点击调用：

```ts
POST /api/workbench/ppt/render
```

成功后展示：

```text
下载 PPTX
```

### 验收标准

- 新模式可以导出 PPTX；
- PPTX 可以被 PowerPoint / WPS 打开；
- PPTX 中中文不乱码；
- PPTX 中文本可编辑；
- 至少 9 种 layout 可以正常渲染；
- 导出不影响旧版 `generate-file`。

---

## 迭代 4：单页 AI 重写、整体优化与换模板

### 目标

让新模式具备长期使用价值，支持用户对局部页面和整体风格进行持续修改。

### 操作内容

#### 4.1 单页 AI 重写

新增接口：

```http
POST /api/workbench/ppt/slides/{slideId}/rewrite
```

后端新增：

```text
backend/src/main/java/com/shupai/multimodal/appentry/service/ppt/PptSlideDesignService.java
```

服务方法：

```java
AiPptSlidePlan rewriteSlide(
    AiPptDeckPlan deckPlan,
    String slideId,
    String instruction,
    ModelServiceEntity service
);
```

前端右侧面板增加快捷操作：

```text
AI 精简本页
AI 扩写本页
改成对比页
改成指标页
改成时间线
改成总结页
自定义指令
```

#### 4.2 整体优化

新增接口：

```http
POST /api/workbench/ppt/optimize
```

后端新增：

```text
backend/src/main/java/com/shupai/multimodal/appentry/service/ppt/PptOptimizeService.java
```

服务方法：

```java
AiPptDeckPlan optimizeDeck(
    AiPptDeckPlan deckPlan,
    String instruction,
    ModelServiceEntity service
);
```

前端顶部工具栏增加：

```text
整体优化
减少文字
增强数据感
改成咨询风格
改成科技风格
改成正式汇报风格
```

#### 4.3 换模板

新增接口：

```http
POST /api/workbench/ppt/change-template
```

后端新增：

```text
backend/src/main/java/com/shupai/multimodal/appentry/service/ppt/PptTemplateService.java
```

第一阶段内置模板即可，不必先建表。

建议内置模板：

```text
business-blue：商务蓝
consulting-clean：咨询白底
tech-dark：深色科技
finance-green：金融绿
startup-gradient：创业渐变
government-red：政企红金
```

模板配置示例：

```json
{
  "templateCode": "consulting-clean",
  "templateName": "咨询白底",
  "theme": {
    "primaryColor": "#122237",
    "secondaryColor": "#5D7491",
    "accentColor": "#DB7640",
    "backgroundColor": "#FAFBFC",
    "textColor": "#1F2937",
    "fontFamily": "Microsoft YaHei",
    "visualStyle": "consulting-clean"
  }
}
```

应用模板时更新：

- theme；
- slide visual background；
- 默认 layout 风格；
- 颜色；
- 字体。

### 前端操作

新增：

```text
frontend/src/components/workbench/ai-ppt-template-picker.tsx
```

在 `AiPptDeckEditor` 顶部增加：

```text
模板选择
整体优化
生成 PPTX
```

在右侧面板增加：

```text
本页 AI 操作
```

### 验收标准

- 可以只重写当前页；
- 重写当前页不会影响其他页；
- 可以整体优化整份 DeckPlan；
- 可以切换模板主题；
- 换模板后前端预览立即变化；
- 换模板后导出的 PPTX 风格同步变化；
- 用户编辑后的内容不会被无故覆盖；
- 新模式具备长期使用基础。

---

## 11. 长期使用注意事项

### 11.1 DeckPlan Schema 必须稳定

`AiPptDeckPlan` 是新 PPT 能力的核心协议。

后续所有能力都围绕它扩展：

- 前端预览；
- 前端编辑；
- 后端导出；
- 单页重写；
- 整体优化；
- 换模板；
- 图表；
- 图片；
- 质检；
- 文档转 PPT。

因此 Schema 修改要谨慎，建议只做向后兼容扩展。

### 11.2 不要把新逻辑继续堆进 WorkbenchServiceImpl

`WorkbenchServiceImpl.java` 已经承担太多职责。

长期建议：

```text
WorkbenchServiceImpl 只负责调度
PPT 生成细节交给 service/ppt/*
```

### 11.3 旧模式至少保留一个版本周期

新模式上线后，不要立即删除旧模式。

建议：

```text
默认 legacy
灰度 ai_full
稳定后默认 ai_full
最后再考虑下线 legacy
```

### 11.4 渲染器需要可替换

短期可以继续用 Apache POI。

长期如果 POI 布局能力不够，可以新增：

```text
PptxGenJsRenderService
```

因此要保留接口：

```java
PptRenderService
```

业务层不要直接依赖 POI。

### 11.5 AI 输出必须做校验

AI 返回结果可能存在：

- JSON 不合法；
- 字段缺失；
- layout 不支持；
- title 为空；
- content 为空；
- 页数不匹配；
- 出现 Markdown 包裹。

后端需要做：

```text
stripMarkdownFence
JSON parse
Schema normalize
fallback fill
layout normalize
content length limit
```

### 11.6 用户编辑内容优先

在单页重写和整体优化时，要避免覆盖用户编辑。

建议在 prompt 中强调：

```text
保留用户已经明确修改的内容。
仅根据用户指令优化必要字段。
不要删除关键页面。
```

---

## 12. 不纳入迭代 0-4 的能力

以下能力建议作为迭代 5 以后再做：

- 图标库完整映射；
- ECharts 图表生成；
- AI 图片生成并插入 PPT；
- AI 视觉质检；
- 自动修复；
- 文档转 PPT；
- 品牌包；
- 模板管理数据库；
- PPT 版本管理；
- 分享链接；
- 协同编辑。

---

## 13. 最终交付效果

完成迭代 0-4 后，项目中的文生 PPT 将具备：

- 旧版 PPT 生成功能稳定可用；
- 新版 `ai_full` 智能生成模式；
- AI 生成完整 PPT 设计稿；
- 前端逐页预览；
- 前端编辑每页内容；
- 前端切换页面版式；
- 后端导出可编辑 PPTX；
- 单页 AI 重写；
- 整体 AI 优化；
- 模板切换；
- 新旧模式兼容。

此时产品形态将从：

```text
PPT 大纲生成器
```

升级为：

```text
智能 PPT 工作台 MVP
```

这已经具备长期使用和继续扩展的基础。
