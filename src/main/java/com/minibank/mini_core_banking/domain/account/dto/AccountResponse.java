package com.minibank.mini_core_banking.domain.account.dto;

import com.minibank.mini_core_banking.domain.account.Account;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private Long balance;
    private String ownerName;

    public static AccountResponse from(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .ownerName(account.getOwnerName())
                .build();
    }
}