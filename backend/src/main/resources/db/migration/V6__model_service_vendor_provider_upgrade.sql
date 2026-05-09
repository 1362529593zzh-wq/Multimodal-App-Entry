ALTER TABLE app.mm_model_service
ADD COLUMN IF NOT EXISTS vendor_code varchar(64) NOT NULL DEFAULT 'default_vendor',
ADD COLUMN IF NOT EXISTS vendor_name varchar(128) NOT NULL DEFAULT '默认厂商',
ADD COLUMN IF NOT EXISTS vendor_type varchar(32) NOT NULL DEFAULT 'official',
ADD COLUMN IF NOT EXISTS provider_type varchar(64) NOT NULL DEFAULT 'openai_compatible',
ADD COLUMN IF NOT EXISTS secret_ref varchar(128),
ADD COLUMN IF NOT EXISTS api_key_masked varchar(128),
ADD COLUMN IF NOT EXISTS secret_key_masked varchar(128),
ADD COLUMN IF NOT EXISTS request_method varchar(16) NOT NULL DEFAULT 'POST',
ADD COLUMN IF NOT EXISTS header_template jsonb NOT NULL DEFAULT '{}'::jsonb,
ADD COLUMN IF NOT EXISTS payload_template jsonb NOT NULL DEFAULT '{}'::jsonb,
ADD COLUMN IF NOT EXISTS extra_config jsonb NOT NULL DEFAULT '{}'::jsonb,
ADD COLUMN IF NOT EXISTS sort_order integer NOT NULL DEFAULT 0;

UPDATE app.mm_model_service
SET vendor_code = CASE function_code
    WHEN 'image_generation' THEN 'default_image_vendor'
    WHEN 'image_recognition' THEN 'default_vision_vendor'
    WHEN 'text_to_speech' THEN 'default_audio_vendor'
    WHEN 'text_to_ppt' THEN 'default_ppt_vendor'
    WHEN 'text_to_video' THEN 'default_video_vendor'
    ELSE 'default_vendor'
END
WHERE vendor_code = 'default_vendor';

UPDATE app.mm_model_service
SET vendor_name = CASE function_code
    WHEN 'image_generation' THEN '默认图像厂商'
    WHEN 'image_recognition' THEN '默认视觉厂商'
    WHEN 'text_to_speech' THEN '默认语音厂商'
    WHEN 'text_to_ppt' THEN '默认PPT厂商'
    WHEN 'text_to_video' THEN '默认视频厂商'
    ELSE '默认厂商'
END
WHERE vendor_name = '默认厂商';

CREATE INDEX IF NOT EXISTS idx_mm_model_service_vendor_enabled
ON app.mm_model_service (vendor_code, enabled, sort_order);

CREATE INDEX IF NOT EXISTS idx_mm_model_service_provider_enabled
ON app.mm_model_service (provider_type, enabled, sort_order);

CREATE INDEX IF NOT EXISTS idx_mm_model_service_model_type_enabled
ON app.mm_model_service (model_type, enabled, sort_order);

CREATE INDEX IF NOT EXISTS idx_mm_model_service_extra_config_gin
ON app.mm_model_service USING gin (extra_config);

CREATE INDEX IF NOT EXISTS idx_mm_model_service_header_template_gin
ON app.mm_model_service USING gin (header_template);

CREATE INDEX IF NOT EXISTS idx_mm_model_service_payload_template_gin
ON app.mm_model_service USING gin (payload_template);

CREATE UNIQUE INDEX IF NOT EXISTS uk_mm_function_model_binding_default_per_function
ON app.mm_function_model_binding (function_code)
WHERE is_default = true AND enabled = true;
