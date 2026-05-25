ALTER TABLE check_ins
    ADD COLUMN manual_idempotency_key VARCHAR(100);

ALTER TABLE check_ins
    ADD CONSTRAINT uk_check_ins_manual_idempotency_key UNIQUE (manual_idempotency_key);
