package com.minibank.mini_core_banking.domain.account.service;

import com.minibank.mini_core_banking.domain.account.dto.TransferResponse;
import com.minibank.mini_core_banking.domain.account.exception.ErrorCode;

record TransferProcessingResult(
        TransferResponse response,
        TransferOutcome outcome,
        ErrorCode errorCode,
        String failureReason
) {
    TransferProcessingResult(TransferResponse response, TransferOutcome outcome) {
        this(response, outcome, null, null);
    }
}
