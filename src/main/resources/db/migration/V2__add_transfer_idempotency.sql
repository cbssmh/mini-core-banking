alter table transfer_history
    add column idempotency_key varchar(128);

alter table transfer_history
    add constraint uk_transfer_history_idempotency_key unique (idempotency_key);
