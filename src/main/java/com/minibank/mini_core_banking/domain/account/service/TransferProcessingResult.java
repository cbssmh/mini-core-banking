package com.minibank.mini_core_banking.domain.account.service;

import com.minibank.mini_core_banking.domain.account.dto.TransferResponse;

record TransferProcessingResult(
        TransferResponse response,
        TransferOutcome outcome
) {
}
