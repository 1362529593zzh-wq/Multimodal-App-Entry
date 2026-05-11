UPDATE app.mm_function_model_binding
SET service_code = 'openrouter-image',
    remark = 'OpenRouter 图像生成默认绑定'
WHERE function_code = 'image_generation'
  AND service_code = '????';

UPDATE app.mm_param_template
SET service_code = 'openrouter-image'
WHERE service_code = '????';

UPDATE app.mm_task
SET service_code = 'openrouter-image'
WHERE service_code = '????';

UPDATE app.mm_call_record
SET service_code = 'openrouter-image'
WHERE service_code = '????';

UPDATE app.mm_stats_daily
SET service_code = 'openrouter-image'
WHERE service_code = '????';

UPDATE app.mm_model_service
SET service_name = 'OpenRouter 图像生成',
    supported_options = '{"ratio":["1:1","4:3","16:9","9:16"],"style":["realistic","illustration","anime","清新","写实"]}'::jsonb,
    remark = 'OpenRouter chat/completions 图像生成服务'
WHERE service_code = 'openrouter-image';

UPDATE app.mm_model_service
SET service_code = 'openrouter-image',
    service_name = 'OpenRouter 图像生成',
    supported_options = '{"ratio":["1:1","4:3","16:9","9:16"],"style":["realistic","illustration","anime","清新","写实"]}'::jsonb,
    remark = 'OpenRouter chat/completions 图像生成服务'
WHERE service_code = '????'
  AND NOT EXISTS (
      SELECT 1
      FROM app.mm_model_service existing
      WHERE existing.service_code = 'openrouter-image'
  );

DELETE FROM app.mm_model_service
WHERE service_code = '????'
  AND EXISTS (
      SELECT 1
      FROM app.mm_model_service existing
      WHERE existing.service_code = 'openrouter-image'
  );

UPDATE app.mm_model_service
SET service_name = 'OpenRouter 图像生成'
WHERE service_code = 'openrouter-image'
  AND service_name = 'OpenRouter ????';

UPDATE app.mm_function_config
SET default_service_code = 'openrouter-image'
WHERE default_service_code = '????';
