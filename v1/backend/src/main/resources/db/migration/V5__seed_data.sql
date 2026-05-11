INSERT INTO app.mm_function_config
(function_code, function_name, icon, sort_order, enabled, is_default, allow_manual_model_select, show_in_main_bar, show_in_more_menu, description)
VALUES
('image_recognition', 'Image Recognition', 'image-recognition', 1, true, false, true, true, false, 'Recognize image content and extract information'),
('image_generation', 'Image Generation', 'image-generation', 2, true, true, true, true, false, 'Generate images from text or image prompts'),
('text_to_speech', 'Text To Speech', 'text-to-speech', 3, true, false, true, true, false, 'Convert text into speech'),
('text_to_ppt', 'Text To PPT', 'text-to-ppt', 4, true, false, true, true, false, 'Generate PPT content from text'),
('text_to_video', 'Text To Video', 'text-to-video', 5, true, false, true, true, false, 'Generate video content from text')
ON CONFLICT (function_code) DO NOTHING;

INSERT INTO app.mm_model_service
(service_code, service_name, model_code, model_name, model_type, function_code, endpoint, auth_type, timeout_ms, enabled, publish_status, is_default, allow_front_select, supported_options, remark)
VALUES
(
  'img-gen-default',
  'Default Image Generation Service',
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
  'Default image generation model'
),
(
  'img-rec-default',
  'Default Image Recognition Service',
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
  'Default image recognition model'
),
(
  'tts-default',
  'Default Text To Speech Service',
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
  'Default text to speech model'
),
(
  'ppt-default',
  'Default Text To PPT Service',
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
  'Default PPT generation model'
),
(
  'video-default',
  'Default Text To Video Service',
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
  'Default video generation model'
)
ON CONFLICT (service_code) DO NOTHING;

INSERT INTO app.mm_function_model_binding
(function_code, service_code, is_default, sort_order, enabled, remark)
VALUES
('image_generation', 'img-gen-default', true, 1, true, 'Default image generation binding'),
('image_recognition', 'img-rec-default', true, 1, true, 'Default image recognition binding'),
('text_to_speech', 'tts-default', true, 1, true, 'Default text to speech binding'),
('text_to_ppt', 'ppt-default', true, 1, true, 'Default text to PPT binding'),
('text_to_video', 'video-default', true, 1, true, 'Default text to video binding')
ON CONFLICT (function_code, service_code) DO NOTHING;

INSERT INTO app.mm_param_template
(template_code, template_name, function_code, service_code, template_type, template_payload, preset_params, description, sort_order, enabled, is_default)
VALUES
(
  'tpl-img-gen-basic',
  'Image Generation Basic Template',
  'image_generation',
  'img-gen-default',
  'default',
  '{"fields":["prompt","ratio","style","reference_image"]}'::jsonb,
  '{"ratio":"1:1","style":"realistic"}'::jsonb,
  'Default parameter template for image generation',
  1,
  true,
  true
),
(
  'tpl-img-rec-basic',
  'Image Recognition Basic Template',
  'image_recognition',
  'img-rec-default',
  'default',
  '{"fields":["image","recognitionMode"]}'::jsonb,
  '{"recognitionMode":"general"}'::jsonb,
  'Default parameter template for image recognition',
  1,
  true,
  true
),
(
  'tpl-tts-basic',
  'Text To Speech Basic Template',
  'text_to_speech',
  'tts-default',
  'default',
  '{"fields":["text","voice","format"]}'::jsonb,
  '{"voice":"female","format":"mp3"}'::jsonb,
  'Default parameter template for text to speech',
  1,
  true,
  true
),
(
  'tpl-ppt-basic',
  'Text To PPT Basic Template',
  'text_to_ppt',
  'ppt-default',
  'default',
  '{"fields":["topic","theme","pages"]}'::jsonb,
  '{"theme":"business","pages":10}'::jsonb,
  'Default parameter template for PPT generation',
  1,
  true,
  true
),
(
  'tpl-video-basic',
  'Text To Video Basic Template',
  'text_to_video',
  'video-default',
  'default',
  '{"fields":["prompt","duration","ratio"]}'::jsonb,
  '{"duration":"10s","ratio":"16:9"}'::jsonb,
  'Default parameter template for video generation',
  1,
  true,
  true
)
ON CONFLICT (template_code) DO NOTHING;
