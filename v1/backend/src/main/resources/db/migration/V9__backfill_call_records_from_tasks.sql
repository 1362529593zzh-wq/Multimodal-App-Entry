INSERT INTO app.mm_call_record (
    record_id,
    task_id,
    session_id,
    message_id,
    parent_task_id,
    source_asset_ids,
    function_code,
    selection_mode,
    resolved_intent,
    service_code,
    model_name,
    request_summary,
    input_text,
    request_params,
    input_assets,
    status,
    result_type,
    result_summary,
    error_message,
    download_count,
    record_status,
    tags,
    created_at,
    started_at,
    finished_at,
    duration_ms,
    is_deleted
)
SELECT
    'record_' || substr(md5(t.task_id), 1, 12),
    t.task_id,
    t.session_id,
    t.message_id,
    t.parent_task_id,
    COALESCE(t.source_asset_ids, '[]'::jsonb),
    t.function_code,
    t.selection_mode,
    t.resolved_intent,
    t.service_code,
    t.model_name,
    left(t.input_text, 255),
    t.input_text,
    COALESCE(t.request_params, '{}'::jsonb),
    COALESCE(t.input_asset_ids, '[]'::jsonb),
    t.status,
    t.result_type,
    COALESCE(r.structured_result, '{}'::jsonb),
    t.error_message,
    0,
    'active',
    '[]'::jsonb,
    t.created_at,
    t.started_at,
    t.finished_at,
    t.duration_ms,
    false
FROM app.mm_task t
LEFT JOIN LATERAL (
    SELECT tr.structured_result
    FROM app.mm_task_result tr
    WHERE tr.task_id = t.task_id
    ORDER BY tr.created_at DESC, tr.id DESC
    LIMIT 1
) r ON true
WHERE NOT EXISTS (
    SELECT 1 FROM app.mm_call_record cr WHERE cr.task_id = t.task_id
);