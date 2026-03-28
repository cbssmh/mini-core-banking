package com.minibank.mini_core_banking.domain.account.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferRequest {

    private Long fromAccountId;
    private Long toAccountId;
    private Long amount;
}