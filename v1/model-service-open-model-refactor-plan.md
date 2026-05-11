# 模型配置服务与工作台接入国内开源模型改造说明

## 1. 改造目标

本次改造的真实目标不是单纯调整页面样式，而是完成下面这条业务链路：

`国内开源模型配置整理 -> 模型配置服务导入/维护 -> 能力绑定 -> 工作台读取模型与参数 schema -> 工作台真实调用模型`

本说明遵循两个前提：

1. `模型配置服务` 的页面排版严格按 `E:\shupai\Multimodal App Entry\模型配置服务前后端改造清单.md` 执行。
2. `工作台` 的页面排版严格按 `E:\shupai\Multimodal App Entry\工作台前后端修改方案.md` 执行。

也就是说：

- 模型配置服务页继续按“左侧厂商筛选 + 中间服务卡片 + 居中弹窗”的排版改。
- 工作台继续按“左会话栏 + 中间消息区 + 底部固定输入区”的排版改。
- 本次文档重点解决“这些 UI 后面应该接什么数据、后端如何支撑、具体改哪些地方”。

---

## 2. 当前项目现状与主要问题

### 2.1 模型配置服务现状

当前实现位置：

- 前端页面：`E:\shupai\Multimodal App Entry\frontend\src\pages\model-service\page.tsx`
- 前端 API：`E:\shupai\Multimodal App Entry\frontend\src\api\config.ts`
- 前端类型：`E:\shupai\Multimodal App Entry\frontend\src\types\api.ts`
- 后端控制器：`E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\web\ModelServiceController.java`
- 后端服务实现：`E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\service\impl\ModelServiceServiceImpl.java`
- 数据实体：`E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\model\entity\ModelServiceEntity.java`

当前问题：

1. 页面仍然是表格 CRUD，不是服务管理中心。
2. `mm_model_service` 只保存了基础模型信息，没有厂商、协议类型、密钥引用、自定义请求模板等字段。
3. 只有新增、编辑、启停，没有删除、导入、测试连接、厂商字典等能力。
4. 现在按 `functionCode` 管理模型，但你的目标是按“厂商/模型服务”集中管理，再让工作台消费。

### 2.2 工作台现状

当前实现位置：

- 前端页面：`E:\shupai\Multimodal App Entry\frontend\src\pages\workbench\page.tsx`
- 前端组件：
  - `E:\shupai\Multimodal App Entry\frontend\src\components\workbench\workbench-shell.tsx`
  - `E:\shupai\Multimodal App Entry\frontend\src\components\workbench\conversation-history.tsx`
  - `E:\shupai\Multimodal App Entry\frontend\src\components\workbench\message-stream.tsx`
  - `E:\shupai\Multimodal App Entry\frontend\src\components\workbench\composer.tsx`
- 前端 API：`E:\shupai\Multimodal App Entry\frontend\src\api\workbench.ts`
- 后端控制器：`E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\web\WorkbenchController.java`
- 后端服务接口：`E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\service\WorkbenchService.java`
- 后端服务实现：`E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\service\impl\WorkbenchServiceImpl.java`

当前问题：

1. 工作台虽已支持函数列表、会话、消息、composer schema，但真实执行链路仍以当前图像生成为主，其他能力大量依赖 mock 或模拟链路。
2. 工作台当前选择模型的逻辑没有真正以“能力绑定表”为主数据源。
3. 工作台后端调用远端模型时，协议结构仍偏单一，不适合直接接多家国内开源模型服务。
4. 工作台参数区虽然已有 schema 接口，但还没有把“模型服务配置中心”真正作为统一数据来源。

---

## 3. 你这次改造的正确顺序

建议按下面顺序执行，不建议先只做模型服务页 UI：

### 第 1 步：先补模型配置底座

- 扩展数据库表结构
- 扩展后端实体 / DTO / VO
- 明确导入格式
- 明确能力绑定规则
- 明确 provider 适配策略

### 第 2 步：再补工作台调用链

- 工作台按能力读取候选模型
- 工作台按模型读取 composer schema
- 工作台提交时显式传递 `serviceCode`
- 后端按 `providerType` 分发到不同调用适配器

### 第 3 步：再改模型配置服务页面

- 页面排版按原计划改
- 但页面里的数据与动作都接真实后端能力
- 增加导入、删除、测试连接、厂商筛选

### 第 4 步：最后补工作台前端联动

- UI 排版按原计划不变
- 参数面板数据不再依赖本地 mock
- 模型候选来自配置中心

---

## 4. 数据库层需要改造的地方

### 4.1 需要新增迁移脚本

当前已有迁移：

- `E:\shupai\Multimodal App Entry\backend\src\main\resources\db\migration\V1__init_schema.sql`
- `E:\shupai\Multimodal App Entry\backend\src\main\resources\db\migration\V2__core_tables.sql`
- `E:\shupai\Multimodal App Entry\backend\src\main\resources\db\migration\V3__extra_tables.sql`
- `E:\shupai\Multimodal App Entry\backend\src\main\resources\db\migration\V4__indexes.sql`
- `E:\shupai\Multimodal App Entry\backend\src\main\resources\db\migration\V5__seed_data.sql`

建议新增：

- `E:\shupai\Multimodal App Entry\backend\src\main\resources\db\migration\V6__model_service_vendor_and_provider.sql`

该脚本只做结构升级，不直接破坏现有数据。

### 4.2 `app.mm_model_service` 需要扩展的字段

当前表字段还不够支撑“国内开源模型导入 + 工作台真实调用”。

建议新增字段：

- `vendor_code varchar(64) not null default 'default_vendor'`
- `vendor_name varchar(128) not null default '默认厂商'`
- `vendor_type varchar(32) not null default 'official'`
- `provider_type varchar(64) not null default 'openai_compatible'`
- `secret_ref varchar(128)`
- `api_key_masked varchar(128)`
- `secret_key_masked varchar(128)`
- `request_method varchar(16) not null default 'POST'`
- `header_template jsonb not null default '{}'::jsonb`
- `payload_template jsonb not null default '{}'::jsonb`
- `extra_config jsonb not null default '{}'::jsonb`
- `sort_order integer not null default 0`

改造原因：

- `vendor_code / vendor_name`：给模型配置服务页左侧厂商筛选和卡片分组使用。
- `vendor_type`：区分“官方厂商”和“自定义接入”。
- `provider_type`：告诉工作台后端应该用哪种调用适配器。
- `secret_ref / *_masked`：避免把密钥明文直接返回前端。
- `request_method / header_template / payload_template / extra_config`：适配不同国内模型服务协议。
- `sort_order`：给厂商、模型服务排序。

### 4.3 `app.mm_function_model_binding` 的定位要调整

当前表已存在：

- `E:\shupai\Multimodal App Entry\backend\src\main\resources\db\migration\V3__extra_tables.sql`

这张表以后不再只是“辅助表”，而是工作台运行时的主数据源之一。

新规则建议：

1. 一个能力可绑定多个模型服务。
2. `is_default=true` 表示该能力默认模型。
3. 工作台的模型候选列表优先从这张表读。
4. 工作台的默认模型优先从这张表读。
5. `mm_model_service.function_code` 保留兼容，但后续不应作为唯一关系来源。

### 4.4 `app.mm_param_template` 的使用方式要规范

当前表已存在，建议明确区分两类模板：

- `template_type = 'composer_schema'`
- `template_type = 'prompt_template'`

用途：

- `composer_schema`：驱动工作台底部参数展开区。
- `prompt_template`：存放提示词模板或预设参数。

### 4.5 是否需要新增密钥表

如果你希望更安全，建议新增：

- `app.mm_model_service_secret`

建议字段：

- `id`
- `service_code`
- `secret_type`
- `secret_ciphertext`
- `created_at`
- `updated_at`

第一阶段如果想控制成本，也可以先不建，先采用：

- 表中保存 `secret_ref`
- 真实密钥走环境变量或外部配置中心

---

## 5. 后端模型配置服务需要改造的地方

### 5.1 实体 / DTO / VO

需要修改：

- `E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\model\entity\ModelServiceEntity.java`
- `E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\model\dto\ModelServiceCreateRequest.java`
- `E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\model\dto\ModelServiceUpdateRequest.java`
- `E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\model\dto\ModelServiceQuery.java`
- `E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\model\vo\ModelServiceVO.java`

#### 如何改

1. `ModelServiceEntity`
   - 增加数据库新字段对应属性。
   - `headerTemplate / payloadTemplate / extraConfig` 继续走 jsonb type handler。

2. `ModelServiceCreateRequest`
   - 增加：
     - `vendorCode`
     - `vendorName`
     - `vendorType`
     - `providerType`
     - `apiKey`
     - `secretKey`
     - `requestMethod`
     - `headerTemplate`
     - `payloadTemplate`
     - `extraConfig`
     - `sortOrder`

3. `ModelServiceUpdateRequest`
   - 与创建请求保持同口径。
   - 编辑时允许不回传旧密钥，只在重新输入时覆盖。

4. `ModelServiceQuery`
   - 增加：
     - `vendorCode`
     - `providerType`
     - `modelType`

5. `ModelServiceVO`
   - 返回脱敏字段：
     - `apiKeyMasked`
     - `secretKeyMasked`
   - 不返回明文密钥。

### 5.2 Service 实现

需要修改：

- `E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\service\ModelServiceService.java`
- `E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\service\impl\ModelServiceServiceImpl.java`

#### 如何改

1. `page()` 方法
   - 支持按 `vendorCode`、`providerType`、`modelType` 查询。
   - 排序建议改为：
     - `sort_order asc`
     - `is_default desc`
     - `created_at desc`

2. `create()` / `update()`
   - 补写厂商、协议、自定义模板相关字段。
   - 对密钥做脱敏持久化或转换成 `secretRef`。

3. 新增删除方法
   - 删除前检查该服务是否仍被 `mm_function_model_binding` 引用。
   - 如被引用，先阻止删除或要求先解绑。

4. 新增导入方法
   - 接收批量 JSON。
   - 一次导入：
     - 模型服务
     - 能力绑定
     - 参数模板

5. 新增测试连接方法
   - 根据 `providerType` 和 `authType` 做最小请求验证。
   - 返回“连接成功/失败原因”。

### 5.3 Controller 接口

需要修改：

- `E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\web\ModelServiceController.java`

#### 如何改

保留现有接口，同时新增：

- `DELETE /api/config/model-services/{serviceCode}`
- `GET /api/config/model-service-vendors`
- `POST /api/config/model-services/import`
- `POST /api/config/model-services/{serviceCode}/test`
- `GET /api/config/model-services/{serviceCode}/bindings`
- `PUT /api/config/model-services/{serviceCode}/bindings`

同时升级：

- `GET /api/config/model-services`
  - 增加 `vendorCode`
  - 增加 `providerType`
  - 增加 `modelType`

---

## 6. 后端工作台需要改造的地方

### 6.1 请求对象升级

需要修改：

- `E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\model\dto\WorkbenchChatRequest.java`

#### 如何改

保留兼容字段，同时新增：

- `serviceCode`
- `composerOptions`
- `schemaVersion`
- `templateCode`
- `attachments`

兼容策略建议：

1. 第一阶段继续兼容 `options: string`
2. 第二阶段主用 `composerOptions: object`
3. 工作台前端逐步改成传结构化对象

### 6.2 WorkbenchService 接口与控制器

需要修改：

- `E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\service\WorkbenchService.java`
- `E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\web\WorkbenchController.java`

#### 如何改

1. `getComposerSchema`
   - 支持可选 `serviceCode`
   - 建议接口改为：
     - `GET /api/workbench/functions/{functionCode}/composer-schema?serviceCode=xxx`

2. `submitChat`
   - 允许显式指定模型服务。

3. 新增上传接口
   - `POST /api/workbench/assets/upload`
   - 支持图像识别、图像生成参考图等输入素材。

### 6.3 WorkbenchServiceImpl 核心逻辑

需要修改：

- `E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\service\impl\WorkbenchServiceImpl.java`

#### 如何改

##### A. 模型选择逻辑改造

当前逻辑应改为：

1. 优先使用前端提交的 `serviceCode`
2. 否则查 `mm_function_model_binding` 中该能力的默认模型
3. 再查 binding 表中排序靠前的启用模型
4. 最后才兜底 `function_config.default_service_code`

这样模型配置服务和工作台就真正打通了。

##### B. Composer Schema 生成逻辑改造

当前 schema 逻辑已经有雏形，但需要改成：

1. 根据能力先找绑定关系
2. 根据绑定到的服务，读取 `supported_options`
3. 如果存在 `template_type = composer_schema` 的模板，则优先使用模板定义
4. 如果模板未覆盖，再回退到 `supported_options`

##### C. 调用执行链改造

当前 `WorkbenchServiceImpl` 中远端调用协议偏单一，不适合后续对接多个国内开源模型服务。

建议新增适配层：

- `ModelInvokeAdapter`
- `OpenAiCompatibleAdapter`
- `OllamaAdapter`
- `CustomHttpAdapter`

建议新增目录：

- `E:\shupai\Multimodal App Entry\backend\src\main\java\com\shupai\multimodal\appentry\service\invoke\`

职责说明：

1. `ModelInvokeAdapter`
   - 定义统一入口：按能力提交任务，返回标准结果。

2. `OpenAiCompatibleAdapter`
   - 适配大部分国内“OpenAI 兼容协议”的模型网关。

3. `OllamaAdapter`
   - 适配本地或内网部署型服务。

4. `CustomHttpAdapter`
   - 适配用户自定义的 header/payload 模板。

##### D. 结果结构增强

建议统一结果结构，新增或补齐：

- `resultKind`
- `previewAssets`
- `downloadAssets`
- `suggestedFollowUps`
- `structuredPayload`

这样工作台结果卡可以按类型自然渲染。

---

## 7. 前端模型配置服务需要改造的地方

### 7.1 页面入口文件

需要重构：

- `E:\shupai\Multimodal App Entry\frontend\src\pages\model-service\page.tsx`

#### 如何改

严格按你现有改造清单排版：

1. 页面保留 `PageIntro`
2. 左侧改为厂商筛选栏
3. 中间改为服务卡片列表
4. 右侧固定配置区取消
5. 新增 / 编辑统一改成居中弹窗

页面状态建议维护：

- `selectedVendor`
- `keyword`
- `modelType`
- `modalOpen`
- `modalMode`
- `editingRecord`
- `togglingCode`
- `deletingCode`
- `importing`
- `testingCode`

### 7.2 新增组件目录

建议新增：

- `E:\shupai\Multimodal App Entry\frontend\src\components\model-service\vendor-filter.tsx`
- `E:\shupai\Multimodal App Entry\frontend\src\components\model-service\service-card.tsx`
- `E:\shupai\Multimodal App Entry\frontend\src\components\model-service\service-modal.tsx`
- `E:\shupai\Multimodal App Entry\frontend\src\components\model-service\service-stats.tsx`
- `E:\shupai\Multimodal App Entry\frontend\src\components\model-service\import-modal.tsx`

#### 如何改

1. `vendor-filter.tsx`
   - 左侧厂商筛选栏
   - 支持搜索厂商
   - 支持高亮当前厂商

2. `service-card.tsx`
   - 中间服务卡片
   - 展示：
     - 服务名称
     - 模型名称
     - 模型标识
     - endpoint
     - providerType
     - authType
     - 默认标签
     - 前台可选标签
   - 操作：
     - 启用 / 停用
     - 编辑
     - 删除
     - 测试连接

3. `service-modal.tsx`
   - 新增 / 编辑共用
   - 支持：
     - 已有厂商
     - 自定义接入
   - 表单中增加：
     - 厂商
     - providerType
     - authType
     - endpoint
     - 密钥配置
     - supportedOptions
     - extraConfig

4. `service-stats.tsx`
   - 展示已接入数量、启用数量、默认数量

5. `import-modal.tsx`
   - 支持批量导入 JSON
   - 支持粘贴 JSON 或上传文件

### 7.3 前端 API 层

需要修改：

- `E:\shupai\Multimodal App Entry\frontend\src\api\config.ts`

#### 如何改

新增 / 升级方法：

- `fetchModelServices({ vendorCode, providerType, modelType, ... })`
- `deleteModelService(serviceCode)`
- `fetchModelServiceVendors()`
- `importModelServices(payload)`
- `testModelService(serviceCode)`
- `fetchModelServiceBindings(serviceCode)`
- `updateModelServiceBindings(serviceCode, payload)`

### 7.4 前端类型定义

需要修改：

- `E:\shupai\Multimodal App Entry\frontend\src\types\api.ts`

#### 如何改

`ModelService` 增加：

- `vendorCode`
- `vendorName`
- `vendorType`
- `providerType`
- `apiKeyMasked`
- `secretKeyMasked`
- `requestMethod`
- `headerTemplate`
- `payloadTemplate`
- `extraConfig`
- `sortOrder`

`ModelServicePayload` 增加：

- `vendorCode`
- `vendorName`
- `vendorType`
- `providerType`
- `apiKey`
- `secretKey`
- `requestMethod`
- `headerTemplate`
- `payloadTemplate`
- `extraConfig`
- `sortOrder`

### 7.5 样式文件

需要修改：

- `E:\shupai\Multimodal App Entry\frontend\src\index.css`

#### 如何改

严格按你原计划风格做，不改变排版方向。

建议新增样式命名：

- `.model-service-shell`
- `.model-service-layout`
- `.model-service-sidebar`
- `.model-service-list`
- `.model-service-card`
- `.model-service-card__header`
- `.model-service-card__meta`
- `.model-service-card__actions`
- `.model-service-modal`
- `.model-service-modal__grid`

---

## 8. 前端工作台需要改造的地方

### 8.1 保持页面排版不变，只改数据来源

需要修改：

- `E:\shupai\Multimodal App Entry\frontend\src\pages\workbench\page.tsx`
- `E:\shupai\Multimodal App Entry\frontend\src\components\workbench\composer.tsx`
- `E:\shupai\Multimodal App Entry\frontend\src\components\workbench\conversation-history.tsx`
- `E:\shupai\Multimodal App Entry\frontend\src\components\workbench\message-stream.tsx`
- `E:\shupai\Multimodal App Entry\frontend\src\components\workbench\capability-bar.tsx`
- `E:\shupai\Multimodal App Entry\frontend\src\components\workbench\option-bar.tsx`
- `E:\shupai\Multimodal App Entry\frontend\src\pages\workbench\types.ts`
- `E:\shupai\Multimodal App Entry\frontend\src\pages\workbench\mock.ts`

#### 如何改

1. 会话区排版不改，继续按原方案。
2. 消息区排版不改，继续按原方案。
3. 底部输入区排版不改，继续按原方案。
4. 只改这几类数据来源：
   - 能力列表来自真实函数配置
   - 模型候选来自能力绑定
   - composer schema 来自后端模板
   - 上传入口来自真实上传接口

### 8.2 前端 API 层

需要修改：

- `E:\shupai\Multimodal App Entry\frontend\src\api\workbench.ts`

#### 如何改

升级 / 新增：

- `fetchWorkbenchComposerSchema(functionCode, serviceCode?)`
- `uploadWorkbenchAsset(file, capabilityCode)`
- `submitWorkbenchChat(payload)` 中支持：
  - `serviceCode`
  - `composerOptions`
  - `schemaVersion`
  - `templateCode`
  - `attachments`

### 8.3 前端类型

需要修改：

- `E:\shupai\Multimodal App Entry\frontend\src\types\api.ts`

#### 如何改

为工作台增加：

- `WorkbenchComposerSchema.serviceOptions`
- `WorkbenchChatPayload.serviceCode`
- `WorkbenchChatPayload.composerOptions`
- `WorkbenchChatPayload.schemaVersion`
- `WorkbenchChatPayload.templateCode`
- `WorkbenchChatPayload.attachments`

---

## 9. 模型导入格式建议

建议第一阶段采用 JSON 导入，避免 CSV 无法自然表达嵌套字段。

建议导入接口接收格式：

```json
{
  "vendorCode": "qwen",
  "vendorName": "通义千问",
  "services": [
    {
      "serviceCode": "qwen-image-gen-prod",
      "serviceName": "Qwen 文生图生产服务",
      "modelCode": "qwen-image",
      "modelName": "Qwen Image",
      "modelType": "image",
      "providerType": "openai_compatible",
      "vendorType": "official",
      "endpoint": "https://your-endpoint.example.com/v1/images/generations",
      "authType": "bearer",
      "timeoutMs": 60000,
      "enabled": true,
      "publishStatus": "PUBLISHED",
      "isDefault": true,
      "allowFrontSelect": true,
      "supportedOptions": {
        "ratio": ["1:1", "4:3", "16:9"],
        "style": ["realistic", "illustration", "cinematic"]
      },
      "extraConfig": {
        "responseMode": "b64_json",
        "supportsReferenceImage": false
      },
      "remark": "国内文生图主模型",
      "bindings": [
        {
          "functionCode": "image_generation",
          "isDefault": true,
          "sortOrder": 1,
          "enabled": true
        }
      ],
      "templates": [
        {
          "templateCode": "tpl-qwen-image-basic",
          "templateName": "Qwen文生图基础模板",
          "templateType": "composer_schema",
          "enabled": true,
          "isDefault": true,
          "sortOrder": 1,
          "templatePayload": {
            "placeholder": "请描述你想生成的画面",
            "helper": "支持比例、风格选择",
            "supportsUpload": false,
            "fields": [
              {
                "key": "ratio",
                "label": "比例",
                "type": "select",
                "options": ["1:1", "4:3", "16:9"]
              },
              {
                "key": "style",
                "label": "风格",
                "type": "select",
                "options": ["realistic", "illustration", "cinematic"]
              }
            ]
          },
          "presetParams": {
            "ratio": "1:1",
            "style": "realistic"
          }
        }
      ]
    }
  ]
}
```

---

## 10. 推荐实施顺序

### 阶段 1：数据库与后端底座

1. 新增 `V6` 迁移脚本
2. 扩展 `ModelServiceEntity / DTO / VO`
3. 补模型服务删除、厂商字典、导入、测试连接接口
4. 调整工作台 `resolveService` 逻辑为优先走 binding

### 阶段 2：工作台调用链

1. `WorkbenchChatRequest` 增加 `serviceCode / composerOptions`
2. `composer-schema` 支持按 `serviceCode` 返回
3. 增加 provider 适配器层
4. 增加附件上传接口

### 阶段 3：模型配置服务前端

1. 按原计划重构排版
2. 接真实厂商筛选
3. 接删除、导入、测试连接
4. 接绑定关系编辑

### 阶段 4：工作台前端

1. 保持既定排版
2. 模型候选与 schema 改为后端驱动
3. 输入提交时显式传 `serviceCode`
4. 逐步替换本地 mock blueprint

---

## 11. 验收标准

完成后，应该至少满足以下结果：

### 11.1 模型配置服务

1. 可以按厂商查看模型服务。
2. 可以新增、编辑、删除、启停模型服务。
3. 可以批量导入国内开源模型配置。
4. 可以对单个模型服务执行测试连接。
5. 可以配置一个模型服务绑定到哪些工作台能力。
6. 页面排版严格符合 `模型配置服务前后端改造清单.md`。

### 11.2 工作台

1. 工作台能力参数区来自后端 schema，而不是主要依赖本地 mock。
2. 工作台提交时可以指定模型服务。
3. 工作台按能力展示可用模型候选。
4. 工作台可以真实调用你导入的国内模型服务。
5. 页面排版严格符合 `工作台前后端修改方案.md`。

---

## 12. 最终结论

本次改造的重点不是“先把模型服务页做漂亮”，而是先把下面三件事做对：

1. 模型服务表升级成真正的模型注册中心。
2. 工作台选模型与调模型改成以绑定关系和 provider 适配器为核心。
3. 前端模型配置页和工作台页严格按既定排版接入这些真实数据。

只要按这个说明推进，你后面把中国国内开源大模型的配置信息整理出来后，就可以做到：

- 在模型配置服务里导入模型
- 在能力绑定里把模型挂到工作台能力下
- 在工作台中直接选择或默认调用这些模型
- 后续新增模型时不需要再推翻 UI 结构
