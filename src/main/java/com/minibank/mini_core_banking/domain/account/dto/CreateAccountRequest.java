package com.minibank.mini_core_banking.domain.account.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAccountRequest {

    @NotBlank
    private String accountNumber;

    @NotNull
    @Min(0)
    private Long balance;

    @NotBlank
    private String ownerName;
}
