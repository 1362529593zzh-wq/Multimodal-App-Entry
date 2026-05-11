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
