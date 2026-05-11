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
