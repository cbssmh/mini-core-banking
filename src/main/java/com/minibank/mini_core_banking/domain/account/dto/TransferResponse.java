package com.minibank.mini_core_banking.domain.account.dto;

import com.minibank.mini_core_banking.domain.account.history.TransferHistory;
import com.minibank.mini_core_banking.domain.account.history.TransferStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransferResponse {

    private Long transferId;
    private TransferStatus status;
    private String idempotencyKey;

    public static TransferResponse from(TransferHistory history) {
        return TransferResponse.builder()
                .transferId(history.getId())
                .status(history.getStatus())
                .idempotencyKey(history.getIdempotencyKey())
                .build();
    }
}
