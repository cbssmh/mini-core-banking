-- Fail migration if existing data violates the invariant; never rewrite money.
alter table account
    add constraint ck_account_balance_nonnegative check (balance >= 0);
