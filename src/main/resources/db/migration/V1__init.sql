create table account (
    id bigint generated always as identity,
    account_number varchar(64) not null,
    owner_name varchar(100) not null,
    balance bigint not null,
    created_at timestamp not null default current_timestamp,
    constraint pk_account primary key (id),
    constraint uk_account_account_number unique (account_number)
);

create table transfer_history (
    id bigint generated always as identity,
    from_account_id bigint not null,
    to_account_id bigint not null,
    amount bigint not null,
    status varchar(32) not null,
    transferred_at timestamp not null default current_timestamp,
    constraint pk_transfer_history primary key (id),
    constraint fk_transfer_history_from_account foreign key (from_account_id) references account (id),
    constraint fk_transfer_history_to_account foreign key (to_account_id) references account (id)
);

create index idx_transfer_history_from_account_id
    on transfer_history (from_account_id);

create index idx_transfer_history_to_account_id
    on transfer_history (to_account_id);

create index idx_transfer_history_transferred_at
    on transfer_history (transferred_at);
