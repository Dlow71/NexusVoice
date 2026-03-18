ALTER TABLE voice_sessions
    ADD COLUMN IF NOT EXISTS selected_asr_model VARCHAR(128) NULL;
