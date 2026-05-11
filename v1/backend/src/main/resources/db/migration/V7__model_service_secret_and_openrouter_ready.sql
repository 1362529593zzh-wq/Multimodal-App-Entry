ALTER TABLE app.mm_model_service
ALTER COLUMN model_code TYPE varchar(128),
ADD COLUMN IF NOT EXISTS api_key_secret text,
ADD COLUMN IF NOT EXISTS secret_key_secret text;
