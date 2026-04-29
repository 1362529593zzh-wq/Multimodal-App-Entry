# 多模态应用入口项目数据库搭建记录与 SQL 附录

## 1. 文档说明

本文档用于记录“多模态应用入口项目”当前已完成的 PostgreSQL 数据库初始化与结构搭建工作，便于后续项目联调、环境复现、部署交接和运维排查。

当前数据库操作基于以下工具完成：

- PostgreSQL
- pgAdmin
- 已连接服务器：`ZZ-study`

当前已完成的工作包括：

- 连接 PostgreSQL 服务
- 创建项目数据库
- 创建业务 Schema
- 创建核心业务表
- 创建扩展业务表
- 创建索引
- 插入基础配置数据
- 配置权限与自动更新时间触发器

---

## 2. 环境信息

### 2.1 数据库环境

- 数据库类型：`PostgreSQL`
- 管理工具：`pgAdmin`
- 已连接服务器：`ZZ-study`
- 数据库名称：`multimodal_app`
- Schema 名称：`app`
- 应用账号：`mm_app`

### 2.2 连接信息

项目后端连接数据库时使用以下信息：

- Host：`127.0.0.1`
- Port：`5432`
- Database：`multimodal_app`
- Username：`mm_app`
- Schema：`app`

---

## 3. 已完成的数据库初始化步骤

### 3.1 连接 PostgreSQL 服务器

已通过 pgAdmin 中的 `ZZ-study` 服务器连接 PostgreSQL 实例，并确认可正常展开数据库对象树。

### 3.2 创建项目用户

已创建项目专用数据库用户：

- 用户名：`mm_app`

该用户用于项目后端连接数据库，避免直接使用 `postgres` 超级管理员账号。

### 3.3 创建项目数据库

已创建业务数据库：

- 数据库名：`multimodal_app`
- Owner：`mm_app`

### 3.4 创建业务 Schema

已在 `multimodal_app` 数据库中创建业务 Schema：

- Schema：`app`

执行内容：

```sql
CREATE SCHEMA IF NOT EXISTS app AUTHORIZATION mm_app;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

说明：

- `app` 用于隔离业务表对象
- `pgcrypto` 用于后续可能的 UUID 生成支持

---

## 4. 已完成的表结构搭建

当前已完成两阶段数据库表结构搭建，共包含以下业务表。

### 4.1 第一阶段核心表

#### 1）功能配置表

- `app.mm_function_config`

用途：

- 存储五类能力配置
- 控制功能启停、排序、默认模型等

#### 2）模型服务配置表

- `app.mm_model_service`

用途：

- 存储模型服务信息
- 存储模型能力、模型类型、接口地址、支持参数等

#### 3）会话表

- `app.mm_chat_session`

用途：

- 存储对话会话信息
- 管理会话标题、状态、最近功能等

#### 4）消息表

- `app.mm_chat_message`

用途：

- 存储用户消息与系统消息
- 支持会话消息流展示

#### 5）任务表

- `app.mm_task`

用途：

- 存储任务执行信息
- 记录请求参数、执行状态、耗时等

#### 6）调用记录表

- `app.mm_call_record`

用途：

- 存储完整调用链路记录
- 支持后续查询、统计、导出

#### 7）文件资产表

- `app.mm_file_asset`

用途：

- 存储输入附件、输出文件、预览与下载信息
- 仅存储文件元数据，不直接存储文件本体

### 4.2 第二阶段扩展表

#### 8）功能与模型绑定表

- `app.mm_function_model_binding`

用途：

- 维护功能与模型服务之间的绑定关系
- 支持默认模型与候选模型配置

#### 9）参数模板表

- `app.mm_param_template`

用途：

- 存储功能参数模板
- 支持默认模板和预设参数

#### 10）任务结果表

- `app.mm_task_result`

用途：

- 存储任务执行后的结构化结果
- 支持文本结果、文件结果、原始响应、错误详情

#### 11）下载日志表

- `app.mm_record_download_log`

用途：

- 存储记录导出、结果下载等行为日志

#### 12）日统计表

- `app.mm_stats_daily`

用途：

- 存储按天汇总的调用统计数据
- 支持统计分析页面

---

## 5. 已完成的索引建设

根据方案要求，已完成常用查询字段索引和 `jsonb` 字段 `GIN` 索引建设。

### 5.1 普通索引

已覆盖以下场景：

- 任务状态查询
- 按时间倒序查询
- 按功能类型查询
- 按模型服务查询
- 会话消息顺序查询
- 下载日志查询
- 日统计查询

### 5.2 GIN 索引

已为以下 `jsonb` 字段创建 `GIN` 索引：

- `app.mm_task.request_params`
- `app.mm_call_record.request_params`
- `app.mm_call_record.result_summary`
- `app.mm_model_service.supported_options`
- `app.mm_param_template.template_payload`
- `app.mm_param_template.preset_params`
- `app.mm_task_result.structured_result`
- `app.mm_task_result.raw_response`
- `app.mm_task_result.error_detail`
- `app.mm_record_download_log.extra_meta`

说明：

这些索引用于支持动态参数、结果摘要、配置模板等半结构化数据的检索能力。

---

## 6. 已完成的基础数据初始化

当前已插入项目运行所需的基础配置数据。

### 6.1 五类功能配置

已插入以下 5 类能力配置：

- 图像识别 `image_recognition`
- 图像生成 `image_generation`
- 文生语音 `text_to_speech`
- 文生 PPT `text_to_ppt`
- 文生视频 `text_to_video`

### 6.2 默认模型服务配置

已插入各能力的默认模型服务配置，包括：

- `img-gen-default`
- `img-rec-default`
- `tts-default`
- `ppt-default`
- `video-default`

### 6.3 功能与模型绑定数据

已为五类能力写入默认模型绑定关系。

### 6.4 参数模板数据

已为五类能力插入基础参数模板，包括：

- 文生图模板
- 图像识别模板
- 文生语音模板
- 文生 PPT 模板
- 文生视频模板

---

## 7. 已完成的权限配置

已对项目账号 `mm_app` 配置数据库访问权限，包括：

- 数据库连接权限
- Schema 使用权限
- 表增删改查权限
- 序列使用权限
- 默认权限继承配置

---

## 8. 已完成的自动更新时间触发器配置

为保证 `updated_at` 字段在更新数据时自动刷新，已创建通用触发器函数，并为以下表配置 `BEFORE UPDATE` 触发器：

- `app.mm_function_config`
- `app.mm_model_service`
- `app.mm_chat_session`
- `app.mm_function_model_binding`
- `app.mm_param_template`
- `app.mm_task_result`
- `app.mm_stats_daily`

---

## 9. 当前数据库建设结果

当前数据库已具备以下能力：

- 支撑功能配置管理
- 支撑模型服务配置
- 支撑会话与消息管理
- 支撑任务调度记录
- 支撑调用记录管理
- 支撑文件元数据管理
- 支撑参数模板管理
- 支撑任务结果存储
- 支撑下载日志存储
- 支撑日统计汇总存储

说明：

从数据库结构层面，项目已具备进入后端联调和基础功能开发的条件。

---

## 10. 当前数据库验收检查 SQL

以下 SQL 可用于验证数据库当前状态。

### 10.1 查看所有业务表

```sql
SELECT schemaname, tablename
FROM pg_tables
WHERE schemaname = 'app'
ORDER BY tablename;
```

### 10.2 查看所有索引

```sql
SELECT schemaname, tablename, indexname
FROM pg_indexes
WHERE schemaname = 'app'
ORDER BY tablename, indexname;
```

### 10.3 查看功能配置数据

```sql
SELECT function_code, function_name, enabled
FROM app.mm_function_config
ORDER BY sort_order;
```

### 10.4 查看模型服务配置数据

```sql
SELECT service_code, service_name, function_code, enabled
FROM app.mm_model_service
ORDER BY function_code, service_code;
```

### 10.5 查看功能与模型绑定数据

```sql
SELECT function_code, service_code, is_default, enabled
FROM app.mm_function_model_binding
ORDER BY function_code, sort_order;
```

### 10.6 查看参数模板数据

```sql
SELECT template_code, template_name, function_code, enabled
FROM app.mm_param_template
ORDER BY function_code, sort_order;
```

---

## 11. 后续建议

数据库阶段完成后，建议按以下顺序推进项目：

### 11.1 应用连接配置

在 Spring Boot 项目中配置 PostgreSQL 数据源，例如：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/multimodal_app?currentSchema=app
    username: mm_app
    password: your_password
    driver-class-name: org.postgresql.Driver
```

注意：

该配置应写入后端项目的 `application.yml`，而不是在 pgAdmin 中执行。

### 11.2 准备中间件环境

若项目需要跑完整任务链路，还需继续准备：

- Redis
- RabbitMQ
- MinIO

### 11.3 补充联调测试数据

建议插入一批联调用测试数据，包括：

- 测试会话
- 测试消息
- 测试任务
- 测试调用记录
- 测试文件记录

### 11.4 迁移脚本化

后续建议将当前手工执行的 SQL 拆分为版本化脚本，例如：

- `V1__init_schema.sql`
- `V2__core_tables.sql`
- `V3__extra_tables.sql`
- `V4__indexes.sql`
- `V5__seed_data.sql`

便于测试环境与生产环境复用。

---

## 12. 总结

截至当前，项目数据库已完成从 0 到基础可运行形态的搭建，主要成果如下：

- 已完成 PostgreSQL 项目库初始化
- 已完成 Schema 创建
- 已完成核心表与扩展表创建
- 已完成索引建设
- 已完成基础配置数据初始化
- 已完成权限授权
- 已完成 `updated_at` 自动维护机制

结论：

数据库侧已基本满足“多模态应用入口项目”后续后端开发、接口联调和基础功能验证所需条件。

---

## 附录 A：数据库初始化与建表 SQL 汇总

### A.1 Schema 与扩展初始化

```sql
CREATE SCHEMA IF NOT EXISTS app AUTHORIZATION mm_app;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

### A.2 第一阶段核心表 SQL

```sql
CREATE TABLE IF NOT EXISTS app.mm_function_config (
    id bigserial PRIMARY KEY,
    function_code varchar(64) NOT NULL UNIQUE,
    function_name varchar(64) NOT NULL,
    icon varchar(128),
    sort_order integer NOT NULL DEFAULT 0,
    enabled boolean NOT NULL DEFAULT true,
    is_default boolean NOT NULL DEFAULT false,
    default_service_code varchar(64),
    allow_manual_model_select boolean NOT NULL DEFAULT true,
    show_in_main_bar boolean NOT NULL DEFAULT true,
    show_in_more_menu boolean NOT NULL DEFAULT false,
    description varchar(255),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS app.mm_model_service (
    id bigserial PRIMARY KEY,
    service_code varchar(64) NOT NULL UNIQUE,
    service_name varchar(128) NOT NULL,
    model_code varchar(64) NOT NULL,
    model_name varchar(128) NOT NULL,
    model_type varchar(64) NOT NULL,
    function_code varchar(64) NOT NULL,
    endpoint varchar(512),
    auth_type varchar(32),
    timeout_ms integer NOT NULL DEFAULT 30000,
    enabled boolean NOT NULL DEFAULT true,
    publish_status varchar(32) NOT NULL DEFAULT 'PUBLISHED',
    is_default boolean NOT NULL DEFAULT false,
    allow_front_select boolean NOT NULL DEFAULT true,
    supported_options jsonb NOT NULL DEFAULT '{}'::jsonb,
    remark varchar(255),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS app.mm_chat_session (
    id bigserial PRIMARY KEY,
    session_id varchar(64) NOT NULL UNIQUE,
    title varchar(255),
    last_function_code varchar(64),
    last_service_code varchar(64),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS app.mm_chat_message (
    id bigserial PRIMARY KEY,
    message_id varchar(64) NOT NULL UNIQUE,
    session_id varchar(64) NOT NULL,
    message_type varchar(32) NOT NULL,
    content_type varchar(32) NOT NULL,
    content_text text,
    related_task_id varchar(64),
    related_record_id varchar(64),
    sequence_no integer NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS app.mm_task (
    id bigserial PRIMARY KEY,
    task_id varchar(64) NOT NULL UNIQUE,
    session_id varchar(64),
    message_id varchar(64),
    parent_task_id varchar(64),
    function_code varchar(64) NOT NULL,
    selection_mode varchar(16) NOT NULL,
    resolved_intent varchar(64),
    service_code varchar(64) NOT NULL,
    model_name varchar(128),
    input_text text,
    request_params jsonb NOT NULL DEFAULT '{}'::jsonb,
    input_asset_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    inheritance_mode varchar(32),
    source_asset_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    status varchar(32) NOT NULL,
    result_type varchar(32),
    error_message text,
    created_at timestamptz NOT NULL DEFAULT now(),
    started_at timestamptz,
    finished_at timestamptz,
    duration_ms bigint
);

CREATE TABLE IF NOT EXISTS app.mm_call_record (
    id bigserial PRIMARY KEY,
    record_id varchar(64) NOT NULL UNIQUE,
    task_id varchar(64) NOT NULL,
    session_id varchar(64),
    message_id varchar(64),
    parent_task_id varchar(64),
    source_asset_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    function_code varchar(64) NOT NULL,
    selection_mode varchar(16) NOT NULL,
    resolved_intent varchar(64),
    service_code varchar(64) NOT NULL,
    model_name varchar(128),
    request_summary varchar(255),
    input_text text,
    request_params jsonb NOT NULL DEFAULT '{}'::jsonb,
    input_assets jsonb NOT NULL DEFAULT '[]'::jsonb,
    status varchar(32) NOT NULL,
    result_type varchar(32),
    result_summary jsonb NOT NULL DEFAULT '{}'::jsonb,
    error_message text,
    download_count integer NOT NULL DEFAULT 0,
    record_status varchar(16) NOT NULL DEFAULT 'active',
    remark varchar(500),
    tags jsonb NOT NULL DEFAULT '[]'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    started_at timestamptz,
    finished_at timestamptz,
    duration_ms bigint,
    is_deleted boolean NOT NULL DEFAULT false,
    deleted_at timestamptz
);

CREATE TABLE IF NOT EXISTS app.mm_file_asset (
    id bigserial PRIMARY KEY,
    file_id varchar(64) NOT NULL UNIQUE,
    related_type varchar(32) NOT NULL,
    related_id varchar(64) NOT NULL,
    file_role varchar(32) NOT NULL,
    file_type varchar(32) NOT NULL,
    file_name varchar(255) NOT NULL,
    storage_key varchar(512) NOT NULL,
    file_size bigint,
    mime_type varchar(128),
    preview_url varchar(512),
    download_url varchar(512),
    source_task_id varchar(64),
    extra_meta jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);
```

### A.3 第二阶段扩展表 SQL

```sql
CREATE TABLE IF NOT EXISTS app.mm_function_model_binding (
    id bigserial PRIMARY KEY,
    function_code varchar(64) NOT NULL,
    service_code varchar(64) NOT NULL,
    is_default boolean NOT NULL DEFAULT false,
    sort_order integer NOT NULL DEFAULT 0,
    enabled boolean NOT NULL DEFAULT true,
    remark varchar(255),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_mm_function_model_binding UNIQUE (function_code, service_code)
);

CREATE TABLE IF NOT EXISTS app.mm_param_template (
    id bigserial PRIMARY KEY,
    template_code varchar(64) NOT NULL UNIQUE,
    template_name varchar(128) NOT NULL,
    function_code varchar(64) NOT NULL,
    service_code varchar(64),
    template_type varchar(32) NOT NULL DEFAULT 'default',
    template_payload jsonb NOT NULL DEFAULT '{}'::jsonb,
    preset_params jsonb NOT NULL DEFAULT '{}'::jsonb,
    description varchar(255),
    sort_order integer NOT NULL DEFAULT 0,
    enabled boolean NOT NULL DEFAULT true,
    is_default boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS app.mm_task_result (
    id bigserial PRIMARY KEY,
    result_id varchar(64) NOT NULL UNIQUE,
    task_id varchar(64) NOT NULL,
    record_id varchar(64),
    result_type varchar(32) NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'SUCCESS',
    text_result text,
    file_ids jsonb NOT NULL DEFAULT '[]'::jsonb,
    structured_result jsonb NOT NULL DEFAULT '{}'::jsonb,
    raw_response jsonb NOT NULL DEFAULT '{}'::jsonb,
    error_detail jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS app.mm_record_download_log (
    id bigserial PRIMARY KEY,
    log_id varchar(64) NOT NULL UNIQUE,
    record_id varchar(64) NOT NULL,
    file_id varchar(64),
    download_type varchar(32) NOT NULL,
    export_format varchar(16),
    operator_id varchar(64),
    operator_name varchar(128),
    client_ip varchar(64),
    user_agent varchar(255),
    extra_meta jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS app.mm_stats_daily (
    id bigserial PRIMARY KEY,
    stat_date date NOT NULL,
    dimension_type varchar(32) NOT NULL,
    dimension_value varchar(128) NOT NULL,
    function_code varchar(64),
    service_code varchar(64),
    model_name varchar(128),
    invoke_count integer NOT NULL DEFAULT 0,
    success_count integer NOT NULL DEFAULT 0,
    failed_count integer NOT NULL DEFAULT 0,
    download_count integer NOT NULL DEFAULT 0,
    avg_duration_ms bigint NOT NULL DEFAULT 0,
    p95_duration_ms bigint,
    total_duration_ms bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_mm_stats_daily UNIQUE (stat_date, dimension_type, dimension_value)
);
```

### A.4 第一阶段索引 SQL

```sql
CREATE INDEX IF NOT EXISTS idx_mm_task_status_created_at
ON app.mm_task (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_mm_task_function_created_at
ON app.mm_task (function_code, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_mm_task_service_created_at
ON app.mm_task (service_code, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_mm_chat_message_session_seq
ON app.mm_chat_message (session_id, sequence_no);

CREATE INDEX IF NOT EXISTS idx_mm_call_record_service_created_at
ON app.mm_call_record (service_code, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_mm_call_record_function_created_at
ON app.mm_call_record (function_code, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_mm_call_record_status_created_at
ON app.mm_call_record (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_mm_file_asset_related
ON app.mm_file_asset (related_type, related_id);

CREATE INDEX IF NOT EXISTS idx_mm_task_request_params_gin
ON app.mm_task USING gin (request_params);

CREATE INDEX IF NOT EXISTS idx_mm_call_record_request_params_gin
ON app.mm_call_record USING gin (request_params);

CREATE INDEX IF NOT EXISTS idx_mm_call_record_result_summary_gin
ON app.mm_call_record USING gin (result_summary);

CREATE INDEX IF NOT EXISTS idx_mm_model_service_supported_options_gin
ON app.mm_model_service USING gin (supported_options);
```

### A.5 第二阶段索引 SQL

```sql
CREATE INDEX IF NOT EXISTS idx_mm_function_model_binding_function
ON app.mm_function_model_binding (function_code, enabled, sort_order);

CREATE INDEX IF NOT EXISTS idx_mm_function_model_binding_service
ON app.mm_function_model_binding (service_code, enabled);

CREATE INDEX IF NOT EXISTS idx_mm_param_template_function
ON app.mm_param_template (function_code, enabled, sort_order);

CREATE INDEX IF NOT EXISTS idx_mm_param_template_service
ON app.mm_param_template (service_code, enabled);

CREATE INDEX IF NOT EXISTS idx_mm_param_template_type
ON app.mm_param_template (template_type, enabled);

CREATE INDEX IF NOT EXISTS idx_mm_task_result_task
ON app.mm_task_result (task_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_mm_task_result_record
ON app.mm_task_result (record_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_mm_task_result_status
ON app.mm_task_result (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_mm_record_download_log_record
ON app.mm_record_download_log (record_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_mm_record_download_log_file
ON app.mm_record_download_log (file_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_mm_record_download_log_type
ON app.mm_record_download_log (download_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_mm_stats_daily_date_dimension
ON app.mm_stats_daily (stat_date DESC, dimension_type, dimension_value);

CREATE INDEX IF NOT EXISTS idx_mm_stats_daily_function
ON app.mm_stats_daily (function_code, stat_date DESC);

CREATE INDEX IF NOT EXISTS idx_mm_stats_daily_service
ON app.mm_stats_daily (service_code, stat_date DESC);

CREATE INDEX IF NOT EXISTS idx_mm_stats_daily_model
ON app.mm_stats_daily (model_name, stat_date DESC);

CREATE INDEX IF NOT EXISTS idx_mm_param_template_payload_gin
ON app.mm_param_template USING gin (template_payload);

CREATE INDEX IF NOT EXISTS idx_mm_param_template_preset_gin
ON app.mm_param_template USING gin (preset_params);

CREATE INDEX IF NOT EXISTS idx_mm_task_result_structured_gin
ON app.mm_task_result USING gin (structured_result);

CREATE INDEX IF NOT EXISTS idx_mm_task_result_raw_response_gin
ON app.mm_task_result USING gin (raw_response);

CREATE INDEX IF NOT EXISTS idx_mm_task_result_error_detail_gin
ON app.mm_task_result USING gin (error_detail);

CREATE INDEX IF NOT EXISTS idx_mm_record_download_log_extra_meta_gin
ON app.mm_record_download_log USING gin (extra_meta);
```

### A.6 基础配置数据 SQL

```sql
INSERT INTO app.mm_function_config
(function_code, function_name, icon, sort_order, enabled, is_default, allow_manual_model_select, show_in_main_bar, show_in_more_menu, description)
VALUES
('image_recognition', '图像识别', 'image-recognition', 1, true, false, true, true, false, '图像内容识别与分析'),
('image_generation', '图像生成', 'image-generation', 2, true, true, true, true, false, '文生图与图像生成'),
('text_to_speech', '文生语音', 'text-to-speech', 3, true, false, true, true, false, '文本转语音'),
('text_to_ppt', '文生PPT', 'text-to-ppt', 4, true, false, true, true, false, '根据文本生成PPT'),
('text_to_video', '文生视频', 'text-to-video', 5, true, false, true, true, false, '根据文本生成视频')
ON CONFLICT (function_code) DO NOTHING;

INSERT INTO app.mm_model_service
(service_code, service_name, model_code, model_name, model_type, function_code, endpoint, auth_type, timeout_ms, enabled, publish_status, is_default, allow_front_select, supported_options, remark)
VALUES
(
  'img-gen-default',
  '默认文生图服务',
  'sdxl-default',
  'SDXL Default',
  'image',
  'image_generation',
  'http://mock-api/image-generation',
  'none',
  30000,
  true,
  'PUBLISHED',
  true,
  true,
  '{"ratio":["1:1","4:3","16:9"],"style":["realistic","illustration","anime"]}'::jsonb,
  '默认文生图模型'
),
(
  'img-rec-default',
  '默认图像识别服务',
  'vision-default',
  'Vision Default',
  'vision',
  'image_recognition',
  'http://mock-api/image-recognition',
  'none',
  30000,
  true,
  'PUBLISHED',
  true,
  true,
  '{"recognitionMode":["general","ocr","detail"]}'::jsonb,
  '默认图像识别模型'
),
(
  'tts-default',
  '默认文生语音服务',
  'tts-default',
  'TTS Default',
  'audio',
  'text_to_speech',
  'http://mock-api/text-to-speech',
  'none',
  30000,
  true,
  'PUBLISHED',
  true,
  true,
  '{"voice":["female","male"],"format":["mp3","wav"]}'::jsonb,
  '默认语音模型'
),
(
  'ppt-default',
  '默认PPT生成服务',
  'ppt-default',
  'PPT Default',
  'ppt',
  'text_to_ppt',
  'http://mock-api/text-to-ppt',
  'none',
  60000,
  true,
  'PUBLISHED',
  true,
  true,
  '{"theme":["business","tech","minimal"]}'::jsonb,
  '默认PPT模型'
),
(
  'video-default',
  '默认视频生成服务',
  'video-default',
  'Video Default',
  'video',
  'text_to_video',
  'http://mock-api/text-to-video',
  'none',
  120000,
  true,
  'PUBLISHED',
  true,
  true,
  '{"duration":["5s","10s","30s"],"ratio":["16:9","9:16"]}'::jsonb,
  '默认视频模型'
)
ON CONFLICT (service_code) DO NOTHING;

INSERT INTO app.mm_function_model_binding
(function_code, service_code, is_default, sort_order, enabled, remark)
VALUES
('image_generation', 'img-gen-default', true, 1, true, '默认文生图服务绑定'),
('image_recognition', 'img-rec-default', true, 1, true, '默认图像识别服务绑定'),
('text_to_speech', 'tts-default', true, 1, true, '默认文生语音服务绑定'),
('text_to_ppt', 'ppt-default', true, 1, true, '默认PPT生成服务绑定'),
('text_to_video', 'video-default', true, 1, true, '默认视频生成服务绑定')
ON CONFLICT (function_code, service_code) DO NOTHING;

INSERT INTO app.mm_param_template
(template_code, template_name, function_code, service_code, template_type, template_payload, preset_params, description, sort_order, enabled, is_default)
VALUES
(
  'tpl-img-gen-basic',
  '文生图-基础模板',
  'image_generation',
  'img-gen-default',
  'default',
  '{"fields":["prompt","ratio","style","reference_image"]}'::jsonb,
  '{"ratio":"1:1","style":"realistic"}'::jsonb,
  '文生图默认参数模板',
  1,
  true,
  true
),
(
  'tpl-img-rec-basic',
  '图像识别-基础模板',
  'image_recognition',
  'img-rec-default',
  'default',
  '{"fields":["image","recognitionMode"]}'::jsonb,
  '{"recognitionMode":"general"}'::jsonb,
  '图像识别默认参数模板',
  1,
  true,
  true
),
(
  'tpl-tts-basic',
  '文生语音-基础模板',
  'text_to_speech',
  'tts-default',
  'default',
  '{"fields":["text","voice","format"]}'::jsonb,
  '{"voice":"female","format":"mp3"}'::jsonb,
  '文生语音默认参数模板',
  1,
  true,
  true
),
(
  'tpl-ppt-basic',
  '文生PPT-基础模板',
  'text_to_ppt',
  'ppt-default',
  'default',
  '{"fields":["topic","theme","pages"]}'::jsonb,
  '{"theme":"business","pages":10}'::jsonb,
  'PPT 默认参数模板',
  1,
  true,
  true
),
(
  'tpl-video-basic',
  '文生视频-基础模板',
  'text_to_video',
  'video-default',
  'default',
  '{"fields":["prompt","duration","ratio"]}'::jsonb,
  '{"duration":"10s","ratio":"16:9"}'::jsonb,
  '视频生成默认参数模板',
  1,
  true,
  true
)
ON CONFLICT (template_code) DO NOTHING;
```

### A.7 权限 SQL

```sql
GRANT CONNECT ON DATABASE multimodal_app TO mm_app;

GRANT USAGE ON SCHEMA app TO mm_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA app TO mm_app;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA app TO mm_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA app
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO mm_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA app
GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO mm_app;
```

### A.8 自动更新时间触发器 SQL

```sql
CREATE OR REPLACE FUNCTION app.set_updated_at()
RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_mm_function_config_updated_at ON app.mm_function_config;
CREATE TRIGGER trg_mm_function_config_updated_at
BEFORE UPDATE ON app.mm_function_config
FOR EACH ROW
EXECUTE FUNCTION app.set_updated_at();

DROP TRIGGER IF EXISTS trg_mm_model_service_updated_at ON app.mm_model_service;
CREATE TRIGGER trg_mm_model_service_updated_at
BEFORE UPDATE ON app.mm_model_service
FOR EACH ROW
EXECUTE FUNCTION app.set_updated_at();

DROP TRIGGER IF EXISTS trg_mm_chat_session_updated_at ON app.mm_chat_session;
CREATE TRIGGER trg_mm_chat_session_updated_at
BEFORE UPDATE ON app.mm_chat_session
FOR EACH ROW
EXECUTE FUNCTION app.set_updated_at();

DROP TRIGGER IF EXISTS trg_mm_function_model_binding_updated_at ON app.mm_function_model_binding;
CREATE TRIGGER trg_mm_function_model_binding_updated_at
BEFORE UPDATE ON app.mm_function_model_binding
FOR EACH ROW
EXECUTE FUNCTION app.set_updated_at();

DROP TRIGGER IF EXISTS trg_mm_param_template_updated_at ON app.mm_param_template;
CREATE TRIGGER trg_mm_param_template_updated_at
BEFORE UPDATE ON app.mm_param_template
FOR EACH ROW
EXECUTE FUNCTION app.set_updated_at();

DROP TRIGGER IF EXISTS trg_mm_task_result_updated_at ON app.mm_task_result;
CREATE TRIGGER trg_mm_task_result_updated_at
BEFORE UPDATE ON app.mm_task_result
FOR EACH ROW
EXECUTE FUNCTION app.set_updated_at();

DROP TRIGGER IF EXISTS trg_mm_stats_daily_updated_at ON app.mm_stats_daily;
CREATE TRIGGER trg_mm_stats_daily_updated_at
BEFORE UPDATE ON app.mm_stats_daily
FOR EACH ROW
EXECUTE FUNCTION app.set_updated_at();
```
