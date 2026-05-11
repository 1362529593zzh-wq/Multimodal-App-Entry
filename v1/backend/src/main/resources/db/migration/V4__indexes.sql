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
