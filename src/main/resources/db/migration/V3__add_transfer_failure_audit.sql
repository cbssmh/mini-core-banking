alter table transfer_history
    drop constraint if exists fk_transfer_history_from_account;

alter table transfer_history
    drop constraint if exists fk_transfer_history_to_account;

alter table transfer_history
    add column request_id varchar(128),
    add column error_code varchar(64),
    add column failure_reason varchar(500),
    add column completed_at timestamp;

create index idx_transfer_history_status
    on transfer_history (status);

create index idx_transfer_history_request_id
    on transfer_history (request_id);
